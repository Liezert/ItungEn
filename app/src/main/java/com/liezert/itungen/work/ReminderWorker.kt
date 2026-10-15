package com.liezert.itungen.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.liezert.itungen.ItungEnApp
import com.liezert.itungen.MainActivity
import com.liezert.itungen.R
import com.liezert.itungen.domain.model.TransactionType
import com.liezert.itungen.domain.model.formatRupiah
import com.liezert.itungen.domain.usecase.QuickTemplate
import com.liezert.itungen.domain.usecase.quickTemplates
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Pengingat harian (jam dipilih di Pengaturan, bawaan 20.00): bila hari ini belum ada catatan, notifikasi "Sudah catat pengeluaran hari ini?" yang membuka
 * Tambah Transaksi, dengan aksi langsung: catat template tersering, atau "Hari ini tidak jajan".
 * Diam saja bila sudah ada catatan atau hari ini sudah ditandai tidak jajan.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as ItungEnApp).container
        val today = LocalDate.now(container.clock)
        if (container.noSpendDay == today) return Result.success()
        val repository = container.transactionRepository
        if (!repository.hasTransactionOn(today)) {
            val recent = repository.observeBetween(today.minusDays(60), today).first()
            notify(applicationContext, quickTemplates(recent, today, limit = 1).firstOrNull())
        }
        return Result.success()
    }

    companion object {
        private const val WORK = "daily-reminder"
        private const val CHANNEL = "reminder"
        private const val NOTIFICATION_ID = 1

        /** Menjadwalkan ulang setiap hari pada [hour]:00 (menimpa jadwal lama). */
        fun schedule(context: Context, hour: Int) {
            val now = LocalDateTime.now()
            var next = now.toLocalDate().atTime(LocalTime.of(hour.coerceIn(0, 23), 0))
            if (!next.isAfter(now)) next = next.plusDays(1)
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(Duration.between(now, next).toMillis(), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK)
        }

        private fun notify(context: Context, template: QuickTemplate?) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL, "Pengingat harian", NotificationManager.IMPORTANCE_DEFAULT))
            val open = PendingIntent.getActivity(
                context, 0, MainActivity.addTransactionIntent(context),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val builder = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_add)
                .setContentTitle("Sudah catat pengeluaran hari ini?")
                .setContentText("Ketuk untuk mencatat. Cukup beberapa detik.")
                .setContentIntent(open)
                .setAutoCancel(true)
            if (template != null) {
                val add = Intent(context, ReminderReceiver::class.java)
                    .setAction(ReminderReceiver.ACTION_QUICK_ADD)
                    .putExtra(ReminderReceiver.EXTRA_CATEGORY, template.categoryId)
                    .putExtra(ReminderReceiver.EXTRA_AMOUNT, template.amount)
                    .putExtra(ReminderReceiver.EXTRA_NOTE, template.note)
                    .putExtra(ReminderReceiver.EXTRA_LABEL, template.label)
                builder.addAction(0, "Catat ${template.label} ${formatRupiah(template.amount)}", broadcast(context, 1, add))
            }
            val noSpend = Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_NO_SPEND)
            builder.addAction(0, "Hari ini tidak jajan", broadcast(context, 2, noSpend))
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        }

        private fun broadcast(context: Context, code: Int, intent: Intent) =
            PendingIntent.getBroadcast(context, code, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        /** Mengganti pengingat dengan konfirmasi singkat yang hilang sendiri. */
        internal fun confirm(context: Context, text: String) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
            val notification = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_add)
                .setContentTitle(text)
                .setTimeoutAfter(4_000)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }
}

/** Aksi di notifikasi pengingat; berjalan tanpa membuka app. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as ItungEnApp).container
        val today = LocalDate.now(container.clock)
        when (intent.action) {
            ACTION_NO_SPEND -> {
                container.noSpendDay = today
                ReminderWorker.confirm(context, "Oke, hari ini tercatat tanpa jajan")
            }
            ACTION_QUICK_ADD -> {
                val pending = goAsync()
                container.appScope.launch {
                    try {
                        container.transactionRepository.addToMainWallet(
                            TransactionType.EXPENSE,
                            intent.getLongExtra(EXTRA_AMOUNT, 0),
                            intent.getLongExtra(EXTRA_CATEGORY, 0),
                            intent.getStringExtra(EXTRA_NOTE),
                            today,
                        )
                        ReminderWorker.confirm(context, "Tercatat: ${intent.getStringExtra(EXTRA_LABEL)}")
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_QUICK_ADD = "com.liezert.itungen.REMINDER_QUICK_ADD"
        const val ACTION_NO_SPEND = "com.liezert.itungen.REMINDER_NO_SPEND"
        const val EXTRA_CATEGORY = "category"
        const val EXTRA_AMOUNT = "amount"
        const val EXTRA_NOTE = "note"
        const val EXTRA_LABEL = "label"
    }
}

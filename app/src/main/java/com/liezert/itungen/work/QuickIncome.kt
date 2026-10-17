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
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.liezert.itungen.ItungEnApp
import com.liezert.itungen.MainActivity
import com.liezert.itungen.R
import com.liezert.itungen.domain.model.Category
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.TransactionType
import com.liezert.itungen.domain.model.formatRupiah
import com.liezert.itungen.domain.model.parseRupiah
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Notifikasi tetap "Catat pemasukan": maksimal tiga tombol kategori pemasukan. RemoteInput menempel pada satu tombol,
 * jadi alurnya: tekan kategori → kolom nominal muncul → ketik → kirim, tanpa membuka app.
 *
 * Disiapkan untuk Live Updates (ProgressStyle, Android 16) nanti: [show] menyusun notifikasi dasar dan [showResult]
 * menampilkan hasil satu catatan dengan ID yang sama, jadi gaya lain cukup mengganti isi kedua fungsi ini.
 *
 * Android 14+ mengizinkan notifikasi ongoing diusap. Notifikasi dasar membawa delete intent: setelah diusap, ia
 * dipasang lagi ([reshowAfterDismiss]) selama toggle aktif. Cara menghentikannya = mematikan toggle di Pengaturan.
 */
object QuickIncome {
    private const val CHANNEL = "quick_income"
    internal const val NOTIFICATION_ID = 2_001
    internal const val KEY_AMOUNT = "amount"
    private const val ResultMillis = 4_000L

    /** Jeda sebelum notifikasi yang diusap muncul lagi, agar tidak terasa langsung melawan usapan pengguna. */
    private const val ReshowMillis = 1_500L

    /** Urutan pilihan bila kategorinya ada; sisanya diisi kategori pemasukan lain. "Kiriman" = uang saku. */
    private val Preferred = listOf("Gaji", "Kiriman", "Uang saku", "Lainnya")

    fun canNotify(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Tiga kategori pemasukan untuk tombol notifikasi. */
    internal fun pick(categories: List<Category>): List<Category> =
        categories.sortedBy { Preferred.indexOf(it.name).let { i -> if (i < 0) Int.MAX_VALUE else i } }.take(3)

    /** Tampilkan (atau perbarui) notifikasi dasar. Tidak melakukan apa-apa tanpa izin notifikasi. */
    suspend fun show(context: Context) {
        if (!canNotify(context)) return
        val container = (context.applicationContext as ItungEnApp).container
        val categories = pick(container.transactionRepository.observeCategories(CategoryType.INCOME).first())
        val builder = base(context)
            .setContentTitle("Catat pemasukan")
            .setContentText("Pilih kategori, lalu ketik nominal (mis. 50rb, 1,5jt)")
        categories.forEach { builder.addAction(action(context, it)) }
        // Hanya notifikasi dasar: konfirmasi/galat yang diusap tidak memicu apa pun (backToBase tetap memulihkan dasar).
        builder.setDeleteIntent(
            PendingIntent.getBroadcast(
                context, 2_999, Intent(context, QuickIncomeReceiver::class.java).setAction(QuickIncomeReceiver.ACTION_DISMISSED),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        )
        notify(context, builder)
    }

    /**
     * Notifikasi dasar diusap: pasang lagi setelah jeda, bila toggle masih aktif, izin masih ada, dan belum ada
     * notifikasi ber-ID sama yang tampil (konfirmasi yang sedang tampil, atau app yang baru dibuka sudah memasangnya).
     * Mematikan toggle membatalkan notifikasi lewat [cancel], yang tidak memicu delete intent, jadi tidak ada loop.
     * [permitted] hanya diganti di tes: sebagian HP (mis. ColorOS) menolak izin yang diberikan lewat shell.
     * @return true bila dipasang lagi
     */
    internal suspend fun reshowAfterDismiss(context: Context, permitted: () -> Boolean = { canNotify(context) }): Boolean {
        delay(ReshowMillis)
        val enabled = (context.applicationContext as ItungEnApp).container.quickIncomeEnabled
        if (!shouldReshow(enabled, permitted(), isShowing(context))) return false
        show(context)
        return true
    }

    private fun isShowing(context: Context) =
        context.getSystemService(NotificationManager::class.java).activeNotifications.any { it.id == NOTIFICATION_ID }

    /** Hasil satu catatan (berhasil atau galat) di notifikasi yang sama: menghentikan indikator "mengirim". */
    fun showResult(context: Context, text: String) {
        if (!canNotify(context)) return
        notify(context, base(context).setContentTitle(text))
    }

    fun cancel(context: Context) = NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)

    private fun base(context: Context): NotificationCompat.Builder {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Catat cepat", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_add)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
    }

    private fun action(context: Context, category: Category): NotificationCompat.Action {
        val intent = Intent(context, QuickIncomeReceiver::class.java)
            .setAction(QuickIncomeReceiver.ACTION_RECORD)
            .putExtra(QuickIncomeReceiver.EXTRA_CATEGORY, category.id)
            .putExtra(QuickIncomeReceiver.EXTRA_LABEL, category.name)
        // FLAG_MUTABLE wajib agar sistem bisa menambahkan teks RemoteInput; intent eksplisit ke receiver sendiri.
        // requestCode berbeda per kategori agar PendingIntent-nya tidak saling menimpa.
        val pending = PendingIntent.getBroadcast(
            context, 3_000 + category.id.toInt(), intent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val input = RemoteInput.Builder(KEY_AMOUNT).setLabel("Nominal · ${category.name}").build()
        return NotificationCompat.Action.Builder(0, category.name, pending)
            .addRemoteInput(input)
            .setAllowGeneratedReplies(false)
            .build()
    }

    private fun notify(context: Context, builder: NotificationCompat.Builder) {
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
            // Izin dicabut di antara pemeriksaan dan notify.
        }
    }

    internal fun amountFrom(intent: Intent): String? =
        RemoteInput.getResultsFromIntent(intent)?.getCharSequence(KEY_AMOUNT)?.toString()

    internal suspend fun backToBase(context: Context) {
        delay(ResultMillis)
        if ((context.applicationContext as ItungEnApp).container.quickIncomeEnabled) show(context) else cancel(context)
    }
}

/** Nominal dari notifikasi catat cepat → transaksi pemasukan di dompet utama, tanpa membuka app. */
class QuickIncomeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as ItungEnApp).container
        if (intent.action == ACTION_DISMISSED) {
            val pending = goAsync()
            container.appScope.launch(Dispatchers.IO) {
                try {
                    QuickIncome.reshowAfterDismiss(context)
                } finally {
                    pending.finish()
                }
            }
            return
        }
        if (intent.action != ACTION_RECORD) return
        val typed = QuickIncome.amountFrom(intent).orEmpty()
        val categoryId = intent.getLongExtra(EXTRA_CATEGORY, 0)
        val label = intent.getStringExtra(EXTRA_LABEL).orEmpty()
        val pending = goAsync()
        container.appScope.launch(Dispatchers.IO) {
            try {
                val amount = parseRupiah(typed)
                if (amount == null) {
                    QuickIncome.showResult(context, "Nominal \"${typed.take(20)}\" tidak dikenali. Contoh: 50000, 50rb, 1,5jt")
                } else {
                    container.transactionRepository.addToMainWallet(TransactionType.INCOME, amount, categoryId, null, LocalDate.now(container.clock))
                    QuickIncome.showResult(context, "✓ ${formatRupiah(amount)} · $label tercatat")
                }
                QuickIncome.backToBase(context)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                QuickIncome.showResult(context, "Gagal mencatat. Coba lagi.")
                QuickIncome.backToBase(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_RECORD = "com.liezert.itungen.QUICK_INCOME"
        const val ACTION_DISMISSED = "com.liezert.itungen.QUICK_INCOME_DISMISSED"
        const val EXTRA_CATEGORY = "category"
        const val EXTRA_LABEL = "label"
    }
}

/** Notifikasi yang diusap dipasang lagi hanya bila toggle aktif, izin ada, dan belum ada yang tampil. */
internal fun shouldReshow(enabled: Boolean, permitted: Boolean, alreadyShowing: Boolean) = enabled && permitted && !alreadyShowing

/** Notifikasi catat cepat muncul lagi setelah HP dinyalakan ulang atau app diperbarui, bila toggle-nya aktif. */
class QuickIncomeBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val container = (context.applicationContext as ItungEnApp).container
        if (!container.quickIncomeEnabled) return
        val pending = goAsync()
        container.appScope.launch(Dispatchers.IO) {
            try {
                QuickIncome.show(context)
            } finally {
                pending.finish()
            }
        }
    }
}

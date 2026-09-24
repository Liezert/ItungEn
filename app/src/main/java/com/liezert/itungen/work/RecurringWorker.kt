package com.liezert.itungen.work

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.liezert.itungen.ItungEnApp
import java.util.concurrent.TimeUnit

/**
 * Pemeriksaan harian gaji otomatis (§4.1). Bisa ditunda sistem hemat baterai Realme,
 * jadi catch-up saat app dibuka ([ItungEnApp]) menjadi jaring pengamannya.
 */
class RecurringWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        (applicationContext as ItungEnApp).container.transactionRepository.generateRecurring()
        Result.success()
    } catch (_: SQLiteException) {
        Result.retry() // backoff eksponensial bawaan WorkManager (§7.3)
    }

    companion object {
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "recurring-daily",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<RecurringWorker>(1, TimeUnit.DAYS).build(),
            )
        }
    }
}

package com.liezert.itungen

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteException
import android.util.Log
import com.liezert.itungen.data.local.AppDatabase
import com.liezert.itungen.data.repository.RoomTransactionRepository
import com.liezert.itungen.domain.repository.TransactionRepository
import com.liezert.itungen.work.RecurringWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Clock

class ItungEnApp : Application() {
    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        RecurringWorker.schedule(this)
    }
}

/** DI manual (§11.7): satu tempat yang merakit dependensi aplikasi. */
class AppContainer(context: Context) {
    val clock: Clock = Clock.systemDefaultZone()
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val database = AppDatabase.create(context)
    val transactionRepository: TransactionRepository = RoomTransactionRepository(database, clock)

    /** Catch-up saat app dibuka: mencatat gaji yang terlewat bila worker ditunda sistem (§4.1). */
    fun catchUpRecurring() {
        appScope.launch {
            try {
                transactionRepository.generateRecurring()
            } catch (e: SQLiteException) {
                Log.w("ItungEn", "Catch-up gaji otomatis gagal; dicoba lagi oleh worker", e)
            }
        }
    }
}

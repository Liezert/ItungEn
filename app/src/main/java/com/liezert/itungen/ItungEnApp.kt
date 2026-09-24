package com.liezert.itungen

import android.app.Application
import android.content.Context
import com.liezert.itungen.data.local.AppDatabase
import com.liezert.itungen.data.repository.RoomTransactionRepository
import com.liezert.itungen.domain.repository.TransactionRepository
import java.time.Clock

class ItungEnApp : Application() {
    val container by lazy { AppContainer(this) }
}

/** DI manual (§11.7): satu tempat yang merakit dependensi aplikasi. */
class AppContainer(context: Context) {
    val clock: Clock = Clock.systemDefaultZone()
    private val database = AppDatabase.create(context)
    val transactionRepository: TransactionRepository = RoomTransactionRepository(database, clock)
}

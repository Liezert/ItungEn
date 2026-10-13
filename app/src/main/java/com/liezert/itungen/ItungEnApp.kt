package com.liezert.itungen

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.liezert.itungen.data.local.AppDatabase
import com.liezert.itungen.data.repository.RoomGoalRepository
import com.liezert.itungen.data.repository.RoomTransactionRepository
import com.liezert.itungen.domain.repository.GoalRepository
import com.liezert.itungen.data.sync.GoogleAccount
import com.liezert.itungen.data.sync.SyncManager
import com.liezert.itungen.domain.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.Clock

class ItungEnApp : Application() {
    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // Jadwal harian gaji otomatis dari versi sebelumnya tetap tersimpan di WorkManager setelah update; batalkan.
        // ponytail: hapus baris ini dan dependensi work-runtime setelah semua HP terpasang versi ini.
        WorkManager.getInstance(this).cancelUniqueWork("recurring-daily")
    }
}

/** DI manual (§11.7): satu tempat yang merakit dependensi aplikasi. */
class AppContainer(context: Context) {
    val clock: Clock = Clock.systemDefaultZone()
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val database = AppDatabase.create(context)
    private val prefs = context.getSharedPreferences("itungen", Context.MODE_PRIVATE)
    val transactionRepository: TransactionRepository = RoomTransactionRepository(database, clock)
    val goalRepository: GoalRepository = RoomGoalRepository(database, clock)

    // Firebase opsional: tanpa google-services.json, initializeApp mengembalikan null dan app berjalan sebagai tamu.
    private val firebaseReady = FirebaseApp.initializeApp(context) != null
    val account = GoogleAccount(context, if (firebaseReady) FirebaseAuth.getInstance() else null)
    val sync = SyncManager(database, ::firestore, account, prefs, appScope)
    val pendingChanges: Flow<Int> = database.syncDao().observePendingCount()

    /** Layar masuk hanya tampil sampai pengguna memilih masuk atau lanjut sebagai tamu. */
    var onboarded: Boolean
        get() = prefs.getBoolean("onboarded", false)
        set(value) = prefs.edit { putBoolean("onboarded", value) }

    /** Mis. saat app dibuka: tarik perubahan dari HP lain (tidak melakukan apa-apa bila belum masuk). */
    fun syncInBackground() {
        appScope.launch { sync.sync() }
    }

    // Cache memori, bukan disk: antrean tulis Firestore tidak tertinggal untuk akun lain setelah keluar.
    // Perubahan yang belum terkirim tetap aman di Room (synced = 0).
    private fun firestore() = FirebaseFirestore.getInstance().apply {
        firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
            .build()
    }
}

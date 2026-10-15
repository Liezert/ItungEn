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
import com.liezert.itungen.data.backup.Backup
import com.liezert.itungen.data.local.AppDatabase
import com.liezert.itungen.data.repository.RoomGoalRepository
import com.liezert.itungen.data.repository.RoomTransactionRepository
import com.liezert.itungen.domain.repository.GoalRepository
import com.liezert.itungen.data.sync.GoogleAccount
import com.liezert.itungen.data.sync.SyncManager
import com.liezert.itungen.domain.repository.TransactionRepository
import com.liezert.itungen.domain.usecase.BudgetPeriod
import com.liezert.itungen.domain.usecase.BudgetPlan
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

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
    /** Untuk kerja yang harus selesai walau layar ditutup (mis. hapus dengan Urungkan). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    internal val database = AppDatabase.create(context)
    private val prefs = context.getSharedPreferences("itungen", Context.MODE_PRIVATE)
    val transactionRepository: TransactionRepository = RoomTransactionRepository(database, clock)
    val goalRepository: GoalRepository = RoomGoalRepository(database, clock)

    // Firebase opsional: tanpa google-services.json, initializeApp mengembalikan null dan app berjalan sebagai tamu.
    private val firebaseReady = FirebaseApp.initializeApp(context) != null
    val account = GoogleAccount(context, if (firebaseReady) FirebaseAuth.getInstance() else null)
    val sync = SyncManager(database, ::firestore, account, prefs, appScope)
    val pendingChanges: Flow<Int> = database.syncDao().observePendingCount()

    /** Tema gelap (bawaan) atau terang; dipilih di Pengaturan. */
    var darkTheme: Boolean
        get() = prefs.getBoolean("dark_theme", true)
        set(value) = prefs.edit { putBoolean("dark_theme", value) }

    // ponytail: rencana anggaran di SharedPreferences + cadangan JSON, belum ikut sinkron Firestore; pindahkan ke
    // dokumen users/{uid}/settings bila pengguna memakai beberapa HP sekaligus.
    private val _budgetPlan = MutableStateFlow(readBudgetPlan())

    /** Rencana Jatah hari ini (Pengaturan → Anggaran); null = belum diatur. */
    val budgetPlan: StateFlow<BudgetPlan?> = _budgetPlan.asStateFlow()

    val backup = Backup(database, { _budgetPlan.value }, ::saveBudgetPlan)

    fun saveBudgetPlan(plan: BudgetPlan) {
        prefs.edit {
            putLong("budget_income", plan.income)
            putInt("budget_start_day", plan.cycleStartDay)
            putLong("budget_bills", plan.fixedBills)
            putLong("budget_savings", plan.savingsTarget)
            putString("budget_period", plan.period.name)
            putString("budget_started_on", plan.startedOn?.toString())
            putString("budget_next_income_on", plan.nextIncomeOn?.toString())
        }
        _budgetPlan.value = plan
    }

    private fun readBudgetPlan(): BudgetPlan? =
        if (!prefs.contains("budget_income")) null
        else BudgetPlan(
            income = prefs.getLong("budget_income", 0),
            cycleStartDay = prefs.getInt("budget_start_day", 1),
            fixedBills = prefs.getLong("budget_bills", 0),
            savingsTarget = prefs.getLong("budget_savings", 0),
            period = BudgetPeriod.valueOf(prefs.getString("budget_period", null) ?: BudgetPeriod.MONTHLY.name),
            startedOn = prefs.getString("budget_started_on", null)?.let(LocalDate::parse),
            nextIncomeOn = prefs.getString("budget_next_income_on", null)?.let(LocalDate::parse),
        )

    /** Pengingat malam bila hari itu belum ada catatan. */
    var reminderEnabled: Boolean
        get() = prefs.getBoolean("reminder", false)
        set(value) = prefs.edit { putBoolean("reminder", value) }

    /** Hari yang ditandai "tidak jajan" dari notifikasi: tidak diingatkan lagi hari itu. */
    var noSpendDay: LocalDate?
        get() = prefs.getString("no_spend_day", null)?.let(LocalDate::parse)
        set(value) = prefs.edit { putString("no_spend_day", value?.toString()) }

    /** Saldo di Beranda & Profil disamarkan (ikon mata). */
    var hideBalance by mutableStateOf(prefs.getBoolean("hide_balance", false))
        private set

    fun setBalanceHidden(hidden: Boolean) {
        hideBalance = hidden
        prefs.edit { putBoolean("hide_balance", hidden) }
    }

    /** Kunci sidik jari / PIN HP saat app dibuka. */
    var appLock: Boolean
        get() = prefs.getBoolean("app_lock", false)
        set(value) = prefs.edit { putBoolean("app_lock", value) }

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

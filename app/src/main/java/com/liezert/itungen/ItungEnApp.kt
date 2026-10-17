package com.liezert.itungen

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import androidx.core.content.edit
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.liezert.itungen.data.backup.Backup
import com.liezert.itungen.ui.design.ActiveTheme
import com.liezert.itungen.ui.design.ThemeSpec
import com.liezert.itungen.ui.design.Themes
import com.liezert.itungen.data.images.GoalImageStore
import com.liezert.itungen.data.local.AppDatabase
import com.liezert.itungen.data.settings.SyncedSettings
import com.liezert.itungen.data.repository.RoomGoalRepository
import com.liezert.itungen.data.repository.RoomTransactionRepository
import com.liezert.itungen.domain.repository.GoalRepository
import com.liezert.itungen.data.sync.GoogleAccount
import com.liezert.itungen.data.sync.SyncManager
import com.liezert.itungen.domain.repository.TransactionRepository
import com.liezert.itungen.domain.usecase.BudgetPlan
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File
import java.time.Clock

class ItungEnApp : Application() {
    val container by lazy { AppContainer(this) }
}

/** DI manual (§11.7): satu tempat yang merakit dependensi aplikasi. */
class AppContainer(context: Context) {
    /** Untuk teks di luar layar (data contoh, notifikasi): mengikuti bahasa app. */
    val appContext: Context = context.applicationContext
    val clock: Clock = Clock.systemDefaultZone()
    /** Untuk kerja yang harus selesai walau layar ditutup (mis. hapus dengan Urungkan). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    internal val database = AppDatabase.create(context)
    private val prefs = context.getSharedPreferences("itungen", Context.MODE_PRIVATE)

    /** Rencana anggaran (dan pengaturan lain milik akun): ikut cadangan dan sinkron. */
    val settings = SyncedSettings(prefs, clock)
    val transactionRepository: TransactionRepository = RoomTransactionRepository(database, clock)
    val goalRepository: GoalRepository = RoomGoalRepository(database, clock)
    /** Gambar produk goal di penyimpanan internal app; Photo Picker tidak butuh izin. */
    val goalImages = GoalImageStore(File(context.filesDir, "goal_images"))
    val contentResolver: ContentResolver = context.contentResolver

    // Firebase opsional: tanpa google-services.json, initializeApp mengembalikan null dan app berjalan sebagai tamu.
    private val firebaseReady = FirebaseApp.initializeApp(context) != null
    val account = GoogleAccount(context, if (firebaseReady) FirebaseAuth.getInstance() else null)
    val sync = SyncManager(database, ::firestore, account, settings, prefs, appScope)

    /** Perubahan yang belum terunggah: baris database + dokumen pengaturan. */
    val pendingChanges: Flow<Int> =
        combine(database.syncDao().observePendingCount(), settings.dirty) { rows, dirty -> rows + if (dirty) 1 else 0 }

    /** Tema gelap (bawaan) atau terang; dipilih di Pengaturan. */
    var darkTheme: Boolean
        get() = prefs.getBoolean("dark_theme", true)
        set(value) = prefs.edit { putBoolean("dark_theme", value) }

    /** Rencana Jatah hari ini (Pengaturan → Anggaran); null = belum diatur. */
    val budgetPlan: StateFlow<BudgetPlan?> get() = settings.plan

    fun saveBudgetPlan(plan: BudgetPlan) = settings.savePlan(plan)

    val backup = Backup(database, settings, goalImages)

    /** Pengingat harian bila hari itu belum ada catatan. */
    var reminderEnabled: Boolean
        get() = prefs.getBoolean("reminder", false)
        set(value) = prefs.edit { putBoolean("reminder", value) }

    /** Tema tampilan (ui/design/Themes); bawaan Liquid Glass. Mengganti tema langsung berlaku tanpa restart. */
    var themeId: String
        get() = prefs.getString("theme", null) ?: Themes.all.first().id
        private set(value) = prefs.edit { putString("theme", value) }

    fun setTheme(spec: ThemeSpec) {
        themeId = spec.id
        ActiveTheme.spec = spec
    }

    /** Notifikasi tetap "Catat pemasukan" (Pengaturan); default mati. */
    var quickIncomeEnabled: Boolean
        get() = prefs.getBoolean("quick_income", false)
        set(value) = prefs.edit { putBoolean("quick_income", value) }

    /** Dialog izin notifikasi sudah pernah ditampilkan (untuk mengenali penolakan permanen). */
    var notificationAsked: Boolean
        get() = prefs.getBoolean("notification_asked", false)
        set(value) = prefs.edit { putBoolean("notification_asked", value) }

    /** Jam pengingat (0–23), dipilih di Pengaturan. */
    var reminderHour: Int
        get() = prefs.getInt("reminder_hour", 20)
        set(value) = prefs.edit { putInt("reminder_hour", value) }

    /** Saldo di Beranda & Profil disamarkan (ikon mata). */
    var hideBalance by mutableStateOf(prefs.getBoolean("hide_balance", false))
        private set

    fun setBalanceHidden(hidden: Boolean) {
        hideBalance = hidden
        prefs.edit { putBoolean("hide_balance", hidden) }
    }

    /** Mode hemat yang dipilih di Pengaturan. */
    var economyMode by mutableStateOf(prefs.getBoolean("economy_mode", false))
        private set

    fun setEconomy(on: Boolean) {
        economyMode = on
        prefs.edit { putBoolean("economy_mode", on) }
    }

    /** Penghemat Baterai HP sedang menyala (diperbarui MainActivity selama app di depan). */
    var powerSaveActive by mutableStateOf(false)

    /** Kaca versi ringan: dipilih sendiri, atau otomatis saat Penghemat Baterai menyala. */
    val glassEconomy: Boolean get() = economyMode || powerSaveActive

    /** Kunci sidik jari / PIN HP saat app dibuka. */
    var appLock: Boolean
        get() = prefs.getBoolean("app_lock", false)
        set(value) = prefs.edit { putBoolean("app_lock", value) }

    /** Layar masuk hanya tampil sampai pengguna memilih masuk atau lanjut sebagai tamu. */
    var onboarded: Boolean
        get() = prefs.getBoolean("onboarded", false)
        set(value) = prefs.edit { putBoolean("onboarded", value) }

    /** Mis. saat app dibuka: tarik perubahan dari HP lain (tidak melakukan apa-apa bila belum masuk). */
    init {
        // Sekali per proses: file gambar yang tidak dipakai goal mana pun (sheet ditutup sebelum disimpan, target
        // dibatalkan di HP lain, app tertutup di tengah jalan) dihapus agar tidak ada file yatim.
        appScope.launch { goalImages.deleteOrphans(goalRepository.imagesInUse()) }
    }

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

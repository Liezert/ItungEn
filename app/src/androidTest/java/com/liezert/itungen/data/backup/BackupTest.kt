package com.liezert.itungen.data.backup

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.liezert.itungen.data.images.GoalImageStore
import com.liezert.itungen.data.local.AppDatabase
import com.liezert.itungen.data.repository.RoomGoalRepository
import com.liezert.itungen.data.repository.RoomTransactionRepository
import com.liezert.itungen.data.settings.SyncedSettings
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.TransactionType
import com.liezert.itungen.domain.usecase.BudgetPlan
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Cadangan ZIP membawa semua data milik pengguna ke HP lain (DB v9 + gambar goal + pengaturan tersinkron). */
@RunWith(AndroidJUnit4::class)
class BackupTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val today = LocalDate.of(2026, 10, 15)
    private val clock = Clock.fixed(Instant.parse("2026-10-15T08:00:00Z"), ZoneOffset.UTC)
    private val root = File(context.cacheDir, "backup-test")
    private val fromImages = GoalImageStore(File(root, "from/goal_images"))
    private val toImages = GoalImageStore(File(root, "to/goal_images"))
    private lateinit var fromDb: AppDatabase
    private lateinit var toDb: AppDatabase
    private lateinit var fromSettings: SyncedSettings
    private lateinit var toSettings: SyncedSettings

    @Before
    fun setUp() {
        root.deleteRecursively()
        fromDb = AppDatabase.create(context, name = null)
        toDb = AppDatabase.create(context, name = null)
        listOf("backup-from", "backup-to").forEach { context.getSharedPreferences(it, Context.MODE_PRIVATE).edit { clear() } }
        fromSettings = SyncedSettings(context.getSharedPreferences("backup-from", Context.MODE_PRIVATE), clock)
        toSettings = SyncedSettings(context.getSharedPreferences("backup-to", Context.MODE_PRIVATE), clock)
    }

    @After
    fun tearDown() {
        fromDb.close()
        toDb.close()
        root.deleteRecursively()
    }

    private fun export(): ByteArray = ByteArrayOutputStream().also { runBlocking { Backup(fromDb, fromSettings, fromImages).export(it) } }.toByteArray()

    private fun import(file: ByteArray) = runBlocking { Backup(toDb, toSettings, toImages).import(ByteArrayInputStream(file)) }

    private fun webp(width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(30, 120, 200)) }
        return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 85, it) }.toByteArray()
    }

    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray = ByteArrayOutputStream().also { out ->
        ZipOutputStream(out).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
    }.toByteArray()

    /** Cadangan versi awal (JSON tanpa gambar), dibekukan di sini agar format lama tetap teruji. */
    private fun legacyJson(vararg goals: String) = """
        {"app": "ItungEn", "version": 1, "exportedAt": "2026-01-01T00:00:00Z",
         "goals": [${goals.joinToString()}], "transactions": [], "deleted": [], "settings": {}}
    """.trimIndent()

    private fun goalDoc(uuid: String, title: String, image: String? = null) = """
        {"uuid": "$uuid", "walletUuid": "w-$uuid", "walletArchived": false, "title": "$title", "targetAmount": 1500000,
         "plannedMonthly": 250000, "status": "ACTIVE", "createdAt": 1767225600000${image?.let { ", \"image\": \"$it\"" }.orEmpty()}}
    """.trimIndent()

    private suspend fun restoredGoals() = RoomGoalRepository(toDb, clock).observeGoals().first()

    @Test
    fun exportImport_carriesEditsSettingsAndDeletes() = runBlocking<Unit> {
        val money = RoomTransactionRepository(fromDb, clock)
        val food = money.observeCategories(CategoryType.EXPENSE).first().first { it.name == "Makanan" }
        val kept = money.addToMainWallet(TransactionType.EXPENSE, 15_000, food.id, "Makan", today)
        money.updateTransaction(kept, TransactionType.EXPENSE, 18_000, food.id, "Makan siang", today)
        money.deleteTransaction(money.addToMainWallet(TransactionType.EXPENSE, 9_000, food.id, "Dihapus", today))
        fromSettings.savePlan(BudgetPlan(income = 3_000_000, cycleStartDay = 25))
        fromSettings.setCategoryLimit(food.uuid, 500_000)
        fromSettings.setNoSpendDay(today.minusDays(1), marked = true)

        val result = import(export())

        assertEquals(ImportResult(added = 1, planRestored = true), result)
        val restored = RoomTransactionRepository(toDb, clock).allTransactions().single()
        assertEquals(18_000L to "Makan siang", restored.amount to restored.note) // hasil ubah, bukan isi awal
        assertEquals(clock.millis(), toDb.transactionDao().get(restored.id).updatedAt)
        assertEquals(mapOf(food.uuid to 500_000L), toSettings.categoryLimits.value)
        assertEquals(setOf(today.minusDays(1)), toSettings.noSpendDays.value)
        assertEquals(1, toDb.syncDao().deletedUuids().size) // yang dihapus tidak kembali, penandanya ikut
    }

    @Test
    fun exportImport_restoresGoalImages() = runBlocking<Unit> {
        val goals = RoomGoalRepository(fromDb, clock)
        val original = fromImages.save(webp(400, 200))
        goals.createGoal("Laptop baru", 2_000_000, null, 500_000, original)
        goals.createGoal("Sepeda", 1_500_000, null, 250_000, null)

        val result = import(export())

        assertEquals(1, result.images)
        val laptop = restoredGoals().single { it.name == "Laptop baru" }
        val name = laptop.imagePath!!
        assertNotEquals(original, name) // nama baru dari GoalImageStore, bukan nama dari berkas cadangan
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(toImages.file(name).path, bounds)
        assertEquals(400 to 200, bounds.outWidth to bounds.outHeight)
        assertNull(restoredGoals().single { it.name == "Sepeda" }.imagePath)

        // Dipulihkan lagi: goal sudah ada, jadi gambarnya tidak disalin ulang (tidak ada file yatim).
        import(export())
        assertEquals(listOf(name), toImages.file("").list()!!.toList())
    }

    @Test
    fun legacyJson_withoutImages_restoresGoals() = runBlocking<Unit> {
        val result = import(legacyJson(goalDoc("g-1", "Sepeda")).toByteArray())

        assertEquals(1, result.added)
        assertEquals(0, result.images)
        assertNull(restoredGoals().single().imagePath)
    }

    @Test
    fun brokenOrMissingImage_restoresGoalWithoutImage() = runBlocking<Unit> {
        val file = zip(
            "backup.json" to legacyJson(goalDoc("g-1", "Rusak", "goal_broken.webp"), goalDoc("g-2", "Hilang", "goal_missing.webp")).toByteArray(),
            "images/goal_broken.webp" to "bukan gambar".toByteArray(),
        )

        val result = import(file)

        assertEquals(2 to 0, result.added to result.images)
        assertTrue(restoredGoals().all { it.imagePath == null })
        assertTrue(toImages.file("").list().isNullOrEmpty())
    }

    @Test
    fun dangerousNames_neverWriteOutsideImageFolder() = runBlocking<Unit> {
        val image = webp(50, 50)
        val file = zip(
            "backup.json" to legacyJson(goalDoc("g-1", "Jahat", "../../evil.webp")).toByteArray(),
            "images/../../evil.webp" to image,
            "../evil.webp" to image,
            "/evil.webp" to image,
        )

        // Android 14+ menolak ZIP berisi ".." (ZipPathValidator) → bukan cadangan; versi lama: entri dilewati.
        val result = runCatching { import(file) }
        result.exceptionOrNull()?.let { assertTrue(it.toString(), it is InvalidBackupException) }
        result.getOrNull()?.let { assertEquals(0, it.images) }
        assertTrue(root.walk().none { it.name.contains("evil") })
        assertTrue(toImages.file("").list().isNullOrEmpty())
    }
}

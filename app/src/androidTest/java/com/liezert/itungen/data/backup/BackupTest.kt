package com.liezert.itungen.data.backup

import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.liezert.itungen.data.local.AppDatabase
import com.liezert.itungen.data.repository.RoomTransactionRepository
import com.liezert.itungen.data.settings.SyncedSettings
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.TransactionType
import com.liezert.itungen.domain.usecase.BudgetPlan
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Cadangan JSON membawa semua data milik pengguna ke HP lain (DB v8 + pengaturan tersinkron). */
@RunWith(AndroidJUnit4::class)
class BackupTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val today = LocalDate.of(2026, 10, 15)
    private val clock = Clock.fixed(Instant.parse("2026-10-15T08:00:00Z"), ZoneOffset.UTC)
    private lateinit var fromDb: AppDatabase
    private lateinit var toDb: AppDatabase

    @Before
    fun setUp() {
        fromDb = AppDatabase.create(context, name = null)
        toDb = AppDatabase.create(context, name = null)
        listOf("backup-from", "backup-to").forEach { context.getSharedPreferences(it, Context.MODE_PRIVATE).edit { clear() } }
    }

    @After
    fun tearDown() {
        fromDb.close()
        toDb.close()
    }

    @Test
    fun exportImport_carriesEditsSettingsAndDeletes() = runBlocking<Unit> {
        val money = RoomTransactionRepository(fromDb, clock)
        val settings = SyncedSettings(context.getSharedPreferences("backup-from", Context.MODE_PRIVATE), clock)
        val food = money.observeCategories(CategoryType.EXPENSE).first().first { it.name == "Makanan" }
        val kept = money.addToMainWallet(TransactionType.EXPENSE, 15_000, food.id, "Makan", today)
        money.updateTransaction(kept, TransactionType.EXPENSE, 18_000, food.id, "Makan siang", today)
        money.deleteTransaction(money.addToMainWallet(TransactionType.EXPENSE, 9_000, food.id, "Dihapus", today))
        settings.savePlan(BudgetPlan(income = 3_000_000, cycleStartDay = 25))
        settings.setCategoryLimit(food.uuid, 500_000)
        settings.setNoSpendDay(today.minusDays(1), marked = true)
        val file = ByteArrayOutputStream().also { Backup(fromDb, settings).exportJson(it) }.toByteArray()

        val target = SyncedSettings(context.getSharedPreferences("backup-to", Context.MODE_PRIVATE), clock)
        val result = Backup(toDb, target).importJson(ByteArrayInputStream(file))

        assertEquals(ImportResult(added = 1, planRestored = true), result)
        val restored = RoomTransactionRepository(toDb, clock).allTransactions().single()
        assertEquals(18_000L to "Makan siang", restored.amount to restored.note) // hasil ubah, bukan isi awal
        assertEquals(clock.millis(), toDb.transactionDao().get(restored.id).updatedAt)
        assertEquals(mapOf(food.uuid to 500_000L), target.categoryLimits.value)
        assertEquals(setOf(today.minusDays(1)), target.noSpendDays.value)
        assertEquals(1, toDb.syncDao().deletedUuids().size) // yang dihapus tidak kembali, penandanya ikut
    }
}

package com.liezert.itungen.data.sync

import com.liezert.itungen.data.local.dao.PendingGoal
import com.liezert.itungen.data.local.dao.PendingTransaction
import com.liezert.itungen.data.local.entity.GoalEntity
import com.liezert.itungen.data.local.entity.TransactionEntity
import com.liezert.itungen.domain.model.GoalStatus
import com.liezert.itungen.domain.model.TransactionType
import com.liezert.itungen.domain.usecase.BudgetPeriod
import com.liezert.itungen.domain.usecase.BudgetPlan
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/** Dokumen yang diunggah satu HP harus menghasilkan baris yang sama di HP lain (selain id lokal). */
class SyncMappingTest {

    @Test
    fun transaction_roundTrip() {
        val local = TransactionEntity(
            id = 7, walletId = 1, categoryId = 3, type = TransactionType.EXPENSE, amount = 18_000, note = "Makan siang",
            date = LocalDate.of(2026, 10, 13), transferGroupId = null, createdAt = Instant.ofEpochMilli(1_760_000_000_123),
            uuid = "tx-1", synced = false,
        )
        val doc = PendingTransaction(local, walletUuid = "main", categoryUuid = "default-expense-food").toDoc()

        assertEquals("main", doc["walletUuid"])
        assertEquals("default-expense-food", doc["categoryUuid"])
        // Di HP lain: id lokal berbeda, sudah tersinkron.
        assertEquals(local.copy(id = 0, walletId = 11, categoryId = 33, synced = true), transactionFromDoc("tx-1", doc, 11, 33))
    }

    @Test
    fun goal_roundTrip_withNullDate() {
        val local = GoalEntity(
            id = 4, walletId = 9, title = "Sepatu Lari", targetAmount = 800_000, targetDate = null, plannedMonthly = 0,
            status = GoalStatus.ACHIEVED, createdAt = Instant.ofEpochMilli(1_760_000_000_000), uuid = "goal-1", synced = false,
        )
        val doc = PendingGoal(local, walletUuid = "wallet-1", walletArchived = false).toDoc()

        assertEquals("wallet-1", doc["walletUuid"])
        assertEquals(false, doc["walletArchived"])
        assertEquals(local.copy(id = 0, walletId = 5, synced = true), goalFromDoc("goal-1", doc, 5))
    }

    /** Fase 2: gambar produk hanya ada di HP ini; dokumen goal dari HP lain tidak membawa dan tidak menghapusnya. */
    @Test
    fun goal_imageStaysLocal() {
        val local = GoalEntity(walletId = 5, title = "Laptop", targetAmount = 2_000_000, createdAt = Instant.EPOCH, uuid = "g", imagePath = "goal_a.webp")
        val doc = PendingGoal(local, "wallet-1", walletArchived = false).toDoc()
        assertEquals(false, doc.containsKey("imagePath"))
        assertEquals(null, goalFromDoc("g", doc, 5).imagePath)
        assertEquals("goal_a.webp", goalFromDoc("g", doc, 5, local = local).imagePath)
    }

    @Test
    fun budgetPlan_roundTrip_andReadsOldTextDates() {
        val plan = BudgetPlan(
            income = 500_000, cycleStartDay = 1, fixedBills = 0, savingsTarget = 50_000, period = BudgetPeriod.UNTIL_DATE,
            startedOn = LocalDate.of(2026, 10, 10), nextIncomeOn = LocalDate.of(2026, 10, 20),
        )
        assertEquals(plan, budgetPlanFromDoc(plan.toDoc()))
        // Cadangan versi awal: tanggal teks, angka Int dari org.json.
        val old = mapOf("income" to 500_000, "cycleStartDay" to 1, "period" to "UNTIL_DATE", "savingsTarget" to 50_000,
            "startedOn" to "2026-10-10", "nextIncomeOn" to "2026-10-20")
        assertEquals(plan, budgetPlanFromDoc(old))
    }

    @Test
    fun transaction_updatedAt_roundTrip_andOldDocsAreUnedited() {
        val local = TransactionEntity(
            walletId = 1, categoryId = 3, type = TransactionType.EXPENSE, amount = 18_000, date = LocalDate.of(2026, 10, 13),
            createdAt = Instant.ofEpochMilli(1_000), uuid = "tx-2", updatedAt = 9_000,
        )
        val doc = PendingTransaction(local, walletUuid = "main", categoryUuid = null).toDoc()
        assertEquals(9_000L, transactionFromDoc("tx-2", doc, 1, 3).updatedAt)
        assertEquals(0L, transactionFromDoc("tx-2", doc - "updatedAt", 1, 3).updatedAt)
    }
}

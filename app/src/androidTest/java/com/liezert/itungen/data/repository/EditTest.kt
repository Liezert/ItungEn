package com.liezert.itungen.data.repository

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.liezert.itungen.data.local.AppDatabase
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Ubah transaksi: isi berubah, saldo mengikuti, dan perubahan ditandai untuk diunggah. */
@RunWith(AndroidJUnit4::class)
class EditTest {
    private lateinit var db: AppDatabase
    private val today = LocalDate.of(2026, 10, 15)
    private val clock = Clock.fixed(Instant.parse("2026-10-15T08:00:00Z"), ZoneOffset.UTC)
    private lateinit var money: RoomTransactionRepository

    @Before
    fun setUp() {
        db = AppDatabase.create(ApplicationProvider.getApplicationContext(), name = null)
        money = RoomTransactionRepository(db, clock)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun update_changesFieldsAndBalance_andQueuesSync() = runBlocking<Unit> {
        val expense = money.observeCategories(CategoryType.EXPENSE).first().associateBy { it.name }
        val income = money.observeCategories(CategoryType.INCOME).first().associateBy { it.name }
        val id = money.addToMainWallet(TransactionType.EXPENSE, 15_000, expense.getValue("Makanan").id, "Makan", today)
        db.syncDao().markTransactionsSynced(listOf(db.transactionDao().get(id).uuid))

        money.updateTransaction(id, TransactionType.INCOME, 50_000, income.getValue("Bonus").id, "Bonus", today.minusDays(2))

        val edited = money.getTransaction(id)!!
        assertEquals(TransactionType.INCOME, edited.type)
        assertEquals(50_000L, edited.amount)
        assertEquals("Bonus", edited.category?.name)
        assertEquals(today.minusDays(2), edited.date)
        assertEquals(50_000L, money.observeMainBalance().first())
        val row = db.transactionDao().get(id)
        assertEquals(false, row.synced)
        assertEquals(clock.millis(), row.updatedAt)
    }

    @Test
    fun goalTransfers_cannotBeEdited() = runBlocking<Unit> {
        val income = money.observeCategories(CategoryType.INCOME).first().first()
        money.addToMainWallet(TransactionType.INCOME, 100_000, income.id, null, today)
        val goals = RoomGoalRepository(db, clock)
        goals.addFunds(goals.createGoal("Sepatu", 500_000, null, 0), 50_000)
        val transfer = money.allTransactions().first { it.type == TransactionType.TRANSFER_OUT }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { money.updateTransaction(transfer.id, TransactionType.EXPENSE, 1, income.id, null, today) }
        }
    }
}

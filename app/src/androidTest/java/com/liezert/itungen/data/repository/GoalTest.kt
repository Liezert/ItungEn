package com.liezert.itungen.data.repository

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.liezert.itungen.data.local.AppDatabase
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.GoalStatus
import com.liezert.itungen.domain.model.TransactionType
import com.liezert.itungen.domain.usecase.InsufficientBalanceException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

/** Tambah Dana dan batalkan goal: invarian saldo dan atomisitas (§4.2, §7.2, §9). */
@RunWith(AndroidJUnit4::class)
class GoalTest {
    private lateinit var db: AppDatabase
    private val today = LocalDate.of(2026, 10, 4)
    private val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private lateinit var goals: RoomGoalRepository
    private lateinit var money: RoomTransactionRepository

    @Before
    fun setUp() = runBlocking {
        db = AppDatabase.create(ApplicationProvider.getApplicationContext(), name = null)
        goals = RoomGoalRepository(db, clock)
        money = RoomTransactionRepository(db, clock)
        val salary = money.observeCategories(CategoryType.INCOME).first().first { it.name == "Gaji" }
        money.addToMainWallet(TransactionType.INCOME, 1_000_000, salary.id, "Gaji", today)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun addFunds_movesMoneyWithoutCountingAsExpense_andMarksAchieved() = runBlocking<Unit> {
        val id = goals.createGoal("Sepatu Lari", 800_000, null, 0)
        goals.addFunds(id, 300_000)

        assertEquals(700_000L, money.observeMainBalance().first())
        assertEquals(300_000L to GoalStatus.ACTIVE, goals.observeGoals().first().single().let { it.saved to it.status })
        // Riwayat dompet utama: satu baris transfer keluar, bukan pengeluaran.
        val out = money.observeTransactions().first().first()
        assertEquals(TransactionType.TRANSFER_OUT to "Ke Sepatu Lari", out.type to out.note)

        goals.addFunds(id, 500_000)
        val goal = goals.observeGoals().first().single()
        assertEquals(800_000L to GoalStatus.ACHIEVED, goal.saved to goal.status)
        assertEquals(200_000L, money.observeMainBalance().first())
    }

    @Test
    fun addFunds_overMainBalance_isRejectedAndWritesNothing() = runBlocking<Unit> {
        val id = goals.createGoal("Laptop", 5_000_000, null, 0)
        try {
            goals.addFunds(id, 1_000_001)
            fail("Seharusnya ditolak")
        } catch (_: InsufficientBalanceException) {
        }
        assertEquals(1_000_000L, money.observeMainBalance().first())
        assertEquals(0L, goals.observeGoals().first().single().saved)
        assertEquals(1, money.observeTransactions().first().size) // hanya gaji
    }

    @Test
    fun cancelGoal_returnsSavedMoneyAndHidesGoal() = runBlocking<Unit> {
        val id = goals.createGoal("Liburan", 2_000_000, today.plusMonths(6), 300_000)
        goals.addFunds(id, 400_000)
        goals.cancelGoal(id)

        assertEquals(1_000_000L, money.observeMainBalance().first())
        assertTrue(goals.observeGoals().first().isEmpty())
        val back = money.observeTransactions().first().first { it.type == TransactionType.TRANSFER_IN }
        assertEquals(400_000L to "Dari Liburan (dibatalkan)", back.amount to back.note)
    }
}

package com.liezert.itungen.data.repository

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.liezert.itungen.data.local.AppDatabase
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.GoalDeposit
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

/** Tambah Dana, riwayat setoran, dan hapus (batalkan) goal: invarian saldo dan atomisitas (§4.2, §7.2, §9). */
@RunWith(AndroidJUnit4::class)
class GoalTest {
    private lateinit var db: AppDatabase
    private val today = LocalDate.of(2026, 10, 4)
    private val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private lateinit var goals: RoomGoalRepository
    private lateinit var money: RoomTransactionRepository

    @Before
    fun setUp() = runBlocking<Unit> {
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
        val out = money.allTransactions().first()
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
        assertEquals(1, money.allTransactions().size) // hanya gaji
    }

    /**
     * Hapus goal: setoran dan pasangannya di dompet utama ikut terhapus, jadi saldo utama kembali utuh (tidak ada uang
     * yang hilang dari total) dan riwayat dompet utama tidak lagi memuat transfer ke goal itu. Goal lain tidak tersentuh.
     */
    @Test
    fun deleteGoal_removesItsTransfers_andRestoresMainBalance() = runBlocking<Unit> {
        val trip = goals.createGoal("Liburan", 2_000_000, today.plusMonths(6), 300_000)
        val bike = goals.createGoal("Sepeda", 800_000, null, 0)
        goals.addFunds(trip, 400_000)
        goals.addFunds(trip, 100_000)
        goals.addFunds(bike, 50_000)
        val total = { runBlocking { money.observeMainBalance().first() + goals.observeGoals().first().sumOf { it.saved } } }
        assertEquals(1_000_000L, total())

        goals.deleteGoal(trip)

        assertEquals(950_000L, money.observeMainBalance().first()) // hanya setoran Sepeda yang masih di goal
        assertEquals(1_000_000L, total())
        assertEquals(listOf("Sepeda"), goals.observeGoals().first().map { it.name })
        assertTrue(goals.observeDeposits(trip).first().isEmpty())
        assertEquals(listOf(50_000L), goals.observeDeposits(bike).first().map { it.amount })
        // Riwayat utama: gaji + transfer ke Sepeda saja; 4 baris transfer Liburan diberi penanda hapus untuk sinkron.
        assertEquals(listOf("Gaji", "Ke Sepeda"), money.allTransactions().map { it.note }.sortedBy { it != "Gaji" })
        assertEquals(4, db.syncDao().deletedUuids().size)
    }

    /** Riwayat setoran per target: hanya Tambah Dana ke target itu, terbaru dulu. */
    @Test
    fun deposits_listOnlyThisGoalsTopUps_newestFirst() = runBlocking<Unit> {
        val bike = goals.createGoal("Sepeda", 800_000, null, 0)
        val laptop = goals.createGoal("Laptop", 5_000_000, null, 0)
        goals.addFunds(bike, 100_000)
        goals.addFunds(laptop, 50_000)
        goals.addFunds(bike, 200_000)

        assertEquals(listOf(200_000L, 100_000L), goals.observeDeposits(bike).first().map { it.amount })
        assertEquals(listOf(GoalDeposit(50_000, today)), goals.observeDeposits(laptop).first())
    }

    /** Gambar produk tersimpan di goal; dihapus = gambarnya dilepas (file-nya dihapus pemanggil). */
    @Test
    fun image_isSavedReplacedAndClearedOnDelete() = runBlocking<Unit> {
        val id = goals.createGoal("Laptop", 2_000_000, null, 0, imagePath = "goal_a.webp")
        assertEquals("goal_a.webp", goals.observeGoals().first().single { it.id == id }.imagePath)
        goals.setImage(id, "goal_b.webp")
        assertEquals("goal_b.webp", goals.observeGoals().first().single { it.id == id }.imagePath)
        goals.setImage(id, null)
        assertEquals(null, goals.observeGoals().first().single { it.id == id }.imagePath)
        goals.setImage(id, "goal_c.webp")
        goals.deleteGoal(id)
        assertEquals(null, db.goalDao().get(id).imagePath)
    }
}

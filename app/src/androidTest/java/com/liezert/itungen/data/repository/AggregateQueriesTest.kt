package com.liezert.itungen.data.repository

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.liezert.itungen.data.local.AppDatabase
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.TransactionType
import com.liezert.itungen.domain.repository.Totals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

/** Kueri SQL berjendela/teragregasi yang menggantikan agregasi di memori (Summaries lama). */
@RunWith(AndroidJUnit4::class)
class AggregateQueriesTest {
    private lateinit var db: AppDatabase
    private val today = LocalDate.of(2026, 9, 30)
    private val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private lateinit var money: RoomTransactionRepository
    private fun d(m: Int, day: Int) = LocalDate.of(2026, m, day)

    @Before
    fun setUp() = runBlocking<Unit> {
        db = AppDatabase.create(ApplicationProvider.getApplicationContext(), name = null)
        money = RoomTransactionRepository(db, clock)
        val expense = money.observeCategories(CategoryType.EXPENSE).first().associateBy { it.name }
        val income = money.observeCategories(CategoryType.INCOME).first().associateBy { it.name }
        val food = expense.getValue("Makanan").id
        val ride = expense.getValue("Transportasi").id
        money.addToMainWallet(TransactionType.INCOME, 1_000_000, income.getValue("Gaji").id, "Gajian", d(9, 1))
        money.addToMainWallet(TransactionType.EXPENSE, 20_000, food, "Makan siang", d(9, 24))
        money.addToMainWallet(TransactionType.EXPENSE, 30_000, food, null, d(9, 22))
        money.addToMainWallet(TransactionType.EXPENSE, 15_000, ride, "Gojek", d(9, 23))
        money.addToMainWallet(TransactionType.EXPENSE, 99_000, food, null, d(8, 31)) // bulan lain
        // Transfer ke goal: bukan pemasukan/pengeluaran (§3.2), tetapi tetap dihitung sebagai transaksi bulan ini.
        val goal = RoomGoalRepository(db, clock).createGoal("Sepatu", 500_000, null, 0)
        RoomGoalRepository(db, clock).addFunds(goal, 100_000)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun totals_ignoreTransfersAndOtherMonths() = runBlocking<Unit> {
        assertEquals(Totals(income = 1_000_000, expense = 65_000, count = 5), money.observeTotals(d(9, 1), d(9, 30)).first())
        assertEquals(Totals(0, 99_000, 1), money.observeTotals(d(8, 1), d(8, 31)).first())
        assertEquals(Totals(0, 20_000, 1), money.observeTotals(d(9, 24), d(9, 24)).first()) // inklusif
    }

    @Test
    fun categoryTotals_groupCountAndSortDescending_perType() = runBlocking<Unit> {
        val spending = money.observeCategoryTotals(TransactionType.EXPENSE, d(9, 1), d(9, 30)).first()
        assertEquals(listOf("Makanan", "Transportasi"), spending.map { it.category.name })
        assertEquals(50_000L to 2, spending[0].total to spending[0].count)
        assertEquals(15_000L to 1, spending[1].total to spending[1].count)
        val income = money.observeCategoryTotals(TransactionType.INCOME, d(9, 1), d(9, 30)).first()
        assertEquals(listOf("Gaji" to 1_000_000L), income.map { it.category.name to it.total })
    }

    @Test
    fun search_matchesNoteOrCategory_caseInsensitive_withLimit() = runBlocking<Unit> {
        assertEquals(listOf("Makan siang"), money.observeSearch("SIANG", null, 50).first().map { it.note })
        assertEquals(3, money.observeSearch("", "Makanan", 50).first().size) // 20k, 30k, 99k
        assertEquals(3, money.observeSearch("makanan", null, 50).first().size) // cocok lewat nama kategori
        assertEquals(1, money.observeSearch("gojek", "Transportasi", 50).first().size)
        assertEquals(0, money.observeSearch("gojek", "Makanan", 50).first().size)
        assertEquals(6, money.observeSearch("  ", null, 50).first().size) // semua, termasuk transfer keluar
        assertEquals(listOf(d(9, 30), d(9, 24)), money.observeSearch("", null, 2).first().map { it.date }) // terbaru dulu
    }

    @Test
    fun windows_recentFirstDateAndExistence() = runBlocking<Unit> {
        assertEquals(3, money.observeBetween(d(9, 22), d(9, 24)).first().size)
        assertEquals(listOf(d(9, 30), d(9, 24)), money.observeRecent(2).first().map { it.date })
        assertEquals(d(8, 31), money.observeFirstDate().first())
        assertEquals(true, money.observeHasTransactions().first())
        assertEquals(true, money.hasTransactionOn(d(9, 23)))
        assertEquals(false, money.hasTransactionOn(d(9, 25)))
    }
}

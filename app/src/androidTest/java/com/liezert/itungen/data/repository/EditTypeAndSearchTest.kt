package com.liezert.itungen.data.repository

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.liezert.itungen.data.local.AppDatabase
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.TransactionType
import com.liezert.itungen.ui.addtransaction.AddTransactionViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class EditTypeAndSearchTest {
    private lateinit var db: AppDatabase
    private val today = LocalDate.of(2026, 10, 17)
    private val clock = Clock.fixed(Instant.parse("2026-10-17T08:00:00Z"), ZoneOffset.UTC)
    private lateinit var money: RoomTransactionRepository

    @Before
    fun setUp() {
        db = AppDatabase.create(ApplicationProvider.getApplicationContext(), name = null)
        money = RoomTransactionRepository(db, clock)
    }

    @After
    fun tearDown() = db.close()

    /** A2: "Ojek" (Transportasi) diubah jadi Pemasukan dulu diam-diam berkategori "Gaji". */
    @Test
    fun switchingTypeWhileEditing_doesNotPickACategory() = runBlocking<Unit> {
        val ride = money.observeCategories(CategoryType.EXPENSE).first().first { it.name == "Transportasi" }
        val id = money.addToMainWallet(TransactionType.EXPENSE, 12_000, ride.id, "Ojek", today)
        val vm = AddTransactionViewModel(money, clock, SavedStateHandle(mapOf(AddTransactionViewModel.EditArg to id)))
        assertEquals(ride.id, vm.uiState.first { it.initialAmount != null && it.categories.isNotEmpty() }.categoryId)

        vm.selectType(CategoryType.INCOME)
        val income = vm.uiState.first { it.type == CategoryType.INCOME && it.categories.isNotEmpty() }
        assertNull(income.categoryId)
    }

    /** B5: mencari "15.000" atau "15000" dulu tidak menemukan transaksi Rp 15.000. */
    @Test
    fun search_matchesAmount() = runBlocking<Unit> {
        val food = money.observeCategories(CategoryType.EXPENSE).first().first { it.name == "Makanan" }
        money.addToMainWallet(TransactionType.EXPENSE, 15_000, food.id, "Makan siang", today)
        money.addToMainWallet(TransactionType.EXPENSE, 20_000, food.id, "Makan malam", today)
        assertEquals(listOf("Makan siang"), money.observeSearch("15.000", null, 50).first().map { it.note })
        assertEquals(listOf("Makan siang"), money.observeSearch("15000", null, 50).first().map { it.note })
        assertEquals(listOf("Makan siang"), money.observeSearch("Rp 15.000", null, 50).first().map { it.note })
    }
}

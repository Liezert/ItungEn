package com.liezert.itungen.domain.usecase

import com.liezert.itungen.domain.model.Category
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.Goal
import com.liezert.itungen.domain.model.Transaction
import com.liezert.itungen.domain.model.TransactionType
import com.liezert.itungen.domain.model.formatRupiahShort
import com.liezert.itungen.domain.model.formatSigned
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class SummariesTest {
    private val food = Category(1, "Makanan", "food", 0, CategoryType.EXPENSE)
    private val ride = Category(2, "Transportasi", "transport", 0, CategoryType.EXPENSE)
    private val pay = Category(3, "Gaji", "salary", 0, CategoryType.INCOME)
    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)

    private var nextId = 0L
    private fun tx(type: TransactionType, amount: Long, date: LocalDate, cat: Category? = food, note: String? = null) =
        Transaction(++nextId, 1, cat, type, amount, note, date)

    private val sep = YearMonth.of(2026, 9)
    private val all = listOf(
        tx(TransactionType.INCOME, 1_000_000, d(2026, 9, 1), pay, "Gajian"),
        tx(TransactionType.EXPENSE, 20_000, d(2026, 9, 24), food, "Makan siang"),
        tx(TransactionType.EXPENSE, 30_000, d(2026, 9, 22), food),
        tx(TransactionType.EXPENSE, 15_000, d(2026, 9, 23), ride, "Gojek"),
        tx(TransactionType.EXPENSE, 99_000, d(2026, 8, 31), food), // bulan lain
        tx(TransactionType.TRANSFER_OUT, 500_000, d(2026, 9, 10), null), // transfer: diabaikan
    )

    @Test
    fun monthNet_incomeMinusExpense_ignoresOtherMonthsAndTransfers() {
        assertEquals(1_000_000 - 20_000 - 30_000 - 15_000L, monthNet(all, sep))
        assertEquals(-99_000L, monthNet(all, YearMonth.of(2026, 8)))
        assertEquals(0L, monthNet(emptyList(), sep))
    }

    @Test
    fun weekStart_isMonday() {
        assertEquals(d(2026, 9, 21), weekStart(d(2026, 9, 24))) // Kamis → Senin
        assertEquals(d(2026, 9, 21), weekStart(d(2026, 9, 21))) // Senin → dirinya
        assertEquals(d(2026, 9, 21), weekStart(d(2026, 9, 27))) // Minggu → Senin sebelumnya
    }

    @Test
    fun expenseBetween_isInclusive() {
        assertEquals(20_000 + 30_000 + 15_000L, expenseBetween(all, d(2026, 9, 21), d(2026, 9, 24)))
        assertEquals(20_000L, expenseBetween(all, d(2026, 9, 24), d(2026, 9, 24)))
    }

    @Test
    fun categorySpending_groupsCountsAndSortsDescending() {
        val result = categorySpending(all, sep)
        assertEquals(listOf("Makanan", "Transportasi"), result.map { it.category.name })
        assertEquals(50_000L to 2, result[0].total to result[0].count)
        assertEquals(15_000L to 1, result[1].total to result[1].count)
    }

    @Test
    fun filter_bySearchAndCategory() {
        assertEquals(listOf("Makan siang"), filterTransactions(all, "SIANG", null).map { it.note })
        assertEquals(3, filterTransactions(all, "", "Makanan").size) // 20k, 30k, 99k
        assertEquals(3, filterTransactions(all, "makanan", null).size) // cocok lewat nama kategori
        assertEquals(1, filterTransactions(all, "gojek", "Transportasi").size)
        assertEquals(0, filterTransactions(all, "gojek", "Makanan").size)
        assertEquals(all.size, filterTransactions(all, "  ", null).size)
    }

    @Test
    fun formatters() {
        assertEquals("Rp 1.25M", formatRupiahShort(1_250_000))
        assertEquals("Rp 3M", formatRupiahShort(3_000_000))
        assertEquals("Rp 12.5M", formatRupiahShort(12_500_000))
        assertEquals("Rp 2.1B", formatRupiahShort(2_100_000_000))
        assertEquals("Rp 250.000", formatRupiahShort(250_000))
        assertEquals("-Rp 1.5M", formatRupiahShort(-1_500_000))
        assertEquals("+Rp 500.000", formatSigned(500_000, incoming = true))
        assertEquals("-Rp 18.000", formatSigned(18_000, incoming = false))
    }

    @Test
    fun goal_progressIsClamped() {
        assertEquals(56, Goal("Sepatu", 452_000, 800_000).percent)
        assertEquals(348_000L, Goal("Sepatu", 452_000, 800_000).remaining)
        assertEquals(100, Goal("Sepatu", 900_000, 800_000).percent)
        assertEquals(0L, Goal("Sepatu", 900_000, 800_000).remaining)
        assertEquals(0, Goal("Kosong", 0, 0).percent)
    }
}

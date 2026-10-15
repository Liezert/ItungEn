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

class SummariesTest {
    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)

    @Test
    fun weekStart_isMonday() {
        assertEquals(d(2026, 9, 21), weekStart(d(2026, 9, 24))) // Kamis → Senin
        assertEquals(d(2026, 9, 21), weekStart(d(2026, 9, 21))) // Senin → dirinya
        assertEquals(d(2026, 9, 21), weekStart(d(2026, 9, 27))) // Minggu → Senin sebelumnya
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

    @Test
    fun roundedPercents_alwaysSumTo100() {
        assertEquals(listOf(65, 35), roundedPercents(listOf(30_000, 16_000))) // 65,2% + 34,8%
        assertEquals(listOf(34, 33, 33), roundedPercents(listOf(1, 1, 1)))
        assertEquals(100, roundedPercents(listOf(7, 13, 29, 51, 3)).sum())
        assertEquals(listOf(0, 0), roundedPercents(listOf(0, 0)))
    }

    @Test
    fun quickTemplates_needTwoRepeatsAndRankByCount() {
        val food = Category(1, "Makanan", "food", 0, CategoryType.EXPENSE)
        val today = LocalDate.of(2026, 10, 14)
        fun tx(note: String?, amount: Long, daysAgo: Long) =
            Transaction(0, 1, food, TransactionType.EXPENSE, amount, note, today.minusDays(daysAgo))
        val templates = quickTemplates(
            listOf(
                tx("Makan siang", 15_000, 1), tx("makan siang ", 15_000, 2), tx("Makan siang", 15_000, 3),
                tx(null, 5_000, 1), tx(null, 5_000, 4),
                tx("Sekali", 99_000, 1),
                tx("Lama", 7_000, 70), tx("Lama", 7_000, 80), // di luar 60 hari
            ),
            today,
        )
        assertEquals(listOf("Makan siang", "Makanan"), templates.map { it.label })
        assertEquals(15_000L, templates.first().amount)
    }

    @Test
    fun quickTemplates_groupAcrossAmounts_useLatest_skipIncome() {
        val food = Category(1, "Makanan", "food", 0, CategoryType.EXPENSE)
        val salary = Category(2, "Gaji", "salary", 0, CategoryType.INCOME)
        val today = LocalDate.of(2026, 10, 14)
        val templates = quickTemplates(
            listOf(
                Transaction(0, 1, food, TransactionType.EXPENSE, 6_000, "Makan siang", today.minusDays(3)),
                Transaction(0, 1, food, TransactionType.EXPENSE, 7_000, "Makan siang", today.minusDays(1)),
                Transaction(0, 1, salary, TransactionType.INCOME, 3_000_000, "Gaji", today.minusDays(40)),
                Transaction(0, 1, salary, TransactionType.INCOME, 3_000_000, "Gaji", today.minusDays(10)),
            ),
            today,
        )
        assertEquals(listOf("Makan siang"), templates.map { it.label }) // "Gaji" tidak bisa tercatat dengan sekali ketuk
        assertEquals(7_000L, templates.single().amount)
    }
}

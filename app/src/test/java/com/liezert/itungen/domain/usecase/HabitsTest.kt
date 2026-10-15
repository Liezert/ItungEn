package com.liezert.itungen.domain.usecase

import com.liezert.itungen.domain.model.Category
import com.liezert.itungen.domain.model.CategoryType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HabitsTest {
    private val food = Category(1, "Makanan", "food", 0, CategoryType.EXPENSE, uuid = "food")
    private val ride = Category(2, "Transportasi", "transport", 0, CategoryType.EXPENSE, uuid = "ride")
    private val fun_ = Category(3, "Hiburan", "entertainment", 0, CategoryType.EXPENSE, uuid = "fun")

    @Test
    fun limitLevel_warnsAt80AndOver100() {
        assertEquals(LimitLevel.OK, limitLevel(799_999, 1_000_000))
        assertEquals(LimitLevel.NEAR, limitLevel(800_000, 1_000_000))
        assertEquals(LimitLevel.OVER, limitLevel(1_000_000, 1_000_000))
        assertEquals(LimitLevel.OK, limitLevel(5_000, 0)) // tanpa batas
        assertEquals(LimitLevel.OVER, limitLevel(999_999_999_999, 999_999_999_999)) // tidak overflow
    }

    @Test
    fun limitWarnings_onlyLimitedCategories_overFirst() {
        val spending = listOf(CategorySpend(food, 10, 450_000), CategorySpend(ride, 5, 90_000), CategorySpend(fun_, 2, 50_000))
        val warnings = limitWarnings(spending, mapOf("food" to 500_000, "ride" to 80_000, "fun" to 200_000))
        assertEquals(listOf("Transportasi" to LimitLevel.OVER, "Makanan" to LimitLevel.NEAR), warnings.map { it.spend.category.name to it.level })
    }

    @Test
    fun recordStreak_todayStillOpen() {
        val today = LocalDate.of(2026, 10, 15)
        val days = setOf(today.minusDays(1), today.minusDays(2), today.minusDays(4))
        assertEquals(2, recordStreak(days, today)) // hari ini belum dicatat: tetap 2
        assertEquals(3, recordStreak(days + today, today))
        assertEquals(0, recordStreak(setOf(today.minusDays(3)), today))
    }

    @Test
    fun heatLevel_quartersOfMax() {
        assertEquals(0, heatLevel(0, 100_000))
        assertEquals(1, heatLevel(1_000, 100_000))
        assertEquals(2, heatLevel(50_000, 100_000))
        assertEquals(4, heatLevel(100_000, 100_000))
    }
}

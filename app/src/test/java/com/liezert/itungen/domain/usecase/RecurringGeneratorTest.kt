package com.liezert.itungen.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RecurringGeneratorTest {
    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)

    @Test
    fun firstRun_includesStartDate() {
        assertEquals(listOf(d(2026, 9, 24)), recurringDueDates(24, d(2026, 9, 24), null, null, today = d(2026, 9, 24)))
    }

    @Test
    fun beforePayday_nothingDue() {
        assertEquals(emptyList<LocalDate>(), recurringDueDates(25, d(2026, 9, 25), null, null, today = d(2026, 9, 24)))
    }

    @Test
    fun day31_fallsOnLastDayOfShortMonths() {
        assertEquals(
            listOf(d(2026, 1, 31), d(2026, 2, 28), d(2026, 3, 31), d(2026, 4, 30)),
            recurringDueDates(31, d(2026, 1, 31), null, null, today = d(2026, 4, 30)),
        )
    }

    @Test
    fun leapYearFebruary_uses29th() {
        assertEquals(listOf(d(2028, 2, 29)), recurringDueDates(30, d(2028, 1, 30), null, d(2028, 1, 30), today = d(2028, 3, 1)))
    }

    @Test
    fun catchUp_multipleMissedMonths() {
        assertEquals(
            listOf(d(2026, 7, 25), d(2026, 8, 25), d(2026, 9, 25)),
            recurringDueDates(25, d(2026, 1, 25), null, lastGenerated = d(2026, 6, 25), today = d(2026, 9, 26)),
        )
    }

    @Test
    fun idempotent_afterGenerating_nothingMore() {
        assertEquals(emptyList<LocalDate>(), recurringDueDates(25, d(2026, 1, 25), null, lastGenerated = d(2026, 9, 25), today = d(2026, 9, 30)))
    }

    @Test
    fun stopsAtEndDate() {
        assertEquals(
            listOf(d(2026, 2, 1), d(2026, 3, 1)),
            recurringDueDates(1, d(2026, 1, 1), endDate = d(2026, 3, 15), lastGenerated = d(2026, 1, 1), today = d(2026, 9, 1)),
        )
    }
}

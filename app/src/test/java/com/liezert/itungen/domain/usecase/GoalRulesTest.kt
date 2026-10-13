package com.liezert.itungen.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class GoalRulesTest {
    private val today = LocalDate.of(2026, 10, 4)

    @Test
    fun validateGoal_eachRule() {
        assertNull(validateGoal("Sepatu", 800_000, null, today))
        assertNull(validateGoal("Sepatu", 800_000, today, today))
        assertEquals(GoalError.TITLE_REQUIRED, validateGoal("  ", 800_000, null, today))
        assertEquals(GoalError.TARGET_REQUIRED, validateGoal("Sepatu", null, null, today))
        assertEquals(GoalError.TARGET_REQUIRED, validateGoal("Sepatu", 0, null, today))
        assertEquals(GoalError.TARGET_TOO_LARGE, validateGoal("Sepatu", 1_000_000_000_000, null, today))
        assertEquals(GoalError.DATE_IN_PAST, validateGoal("Sepatu", 800_000, today.minusDays(1), today))
    }

    @Test
    fun validateTransfer_blocksMoreThanMainBalance() {
        assertNull(validateTransfer(50_000, 50_000))
        assertEquals(TransferError.INSUFFICIENT_BALANCE, validateTransfer(50_001, 50_000))
        assertEquals(TransferError.INSUFFICIENT_BALANCE, validateTransfer(1, -19_999)) // saldo negatif
        assertEquals(TransferError.AMOUNT_REQUIRED, validateTransfer(0, 50_000))
        assertEquals(TransferError.AMOUNT_REQUIRED, validateTransfer(null, 50_000))
    }

    @Test
    fun suggestedMonthly_remainingOverMonths_roundedUpTo1000() {
        // 4 Okt → 4 Des = tepat 2 bulan; sisa 348.000 → 174.000
        assertEquals(174_000L, suggestedMonthly(800_000, 452_000, LocalDate.of(2026, 12, 4), today))
        // 4 Okt → 31 Des = 2 bulan lebih → 3 bulan; 348.000 / 3 = 116.000
        assertEquals(116_000L, suggestedMonthly(800_000, 452_000, LocalDate.of(2026, 12, 31), today))
        // pembulatan ke atas: 100.001 / 1 → 101.000
        assertEquals(101_000L, suggestedMonthly(100_001, 0, LocalDate.of(2026, 10, 20), today))
        // target hari ini → minimal 1 bulan
        assertEquals(10_000L, suggestedMonthly(10_000, 0, today, today))
        // sudah tercapai / tanpa tanggal
        assertEquals(0L, suggestedMonthly(800_000, 900_000, LocalDate.of(2026, 12, 4), today))
        assertNull(suggestedMonthly(800_000, 0, null, today))
    }
}

package com.liezert.itungen.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class TransactionRulesTest {
    private val today = LocalDate.of(2026, 9, 24)

    @Test
    /** Bug C3: spasi biasa membuat baris patah jadi "Rp⏎30.000"; pemisahnya spasi tak-putus. */
    fun formatRupiah_usesDotThousandsSeparator() {
        assertEquals("Rp\u00A00", formatRupiah(0))
        assertEquals("Rp\u00A0500", formatRupiah(500))
        assertEquals("Rp\u00A01.500.000", formatRupiah(1_500_000))
        assertEquals("Rp\u00A0999.999.999.999", formatRupiah(MAX_AMOUNT))
        assertEquals("-Rp\u00A050.000", formatRupiah(-50_000))
    }

    @Test
    fun validate_acceptsValidInput_includingToday() {
        assertNull(validateTransaction(amount = 1, categoryId = 1, date = today, today = today))
        assertNull(validateTransaction(amount = MAX_AMOUNT, categoryId = 1, date = today.minusYears(1), today = today))
    }

    @Test
    fun validate_rejectsEachRule() {
        assertEquals(TransactionError.AMOUNT_REQUIRED, validateTransaction(null, 1, today, today))
        assertEquals(TransactionError.AMOUNT_REQUIRED, validateTransaction(0, 1, today, today))
        assertEquals(TransactionError.AMOUNT_REQUIRED, validateTransaction(-5, 1, today, today))
        assertEquals(TransactionError.AMOUNT_TOO_LARGE, validateTransaction(MAX_AMOUNT + 1, 1, today, today))
        assertEquals(TransactionError.CATEGORY_REQUIRED, validateTransaction(1_000, null, today, today))
        assertEquals(TransactionError.DATE_IN_FUTURE, validateTransaction(1_000, 1, today.plusDays(1), today))
    }
}

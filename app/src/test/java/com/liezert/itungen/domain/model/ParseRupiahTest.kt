package com.liezert.itungen.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/** Fase 3: nominal yang diketik di notifikasi catat cepat. */
class ParseRupiahTest {
    @Test
    fun acceptsCommonWritings() {
        assertEquals(50_000L, parseRupiah("50000"))
        assertEquals(50_000L, parseRupiah("50.000"))
        assertEquals(50_000L, parseRupiah("50rb"))
        assertEquals(50_000L, parseRupiah("50 ribu"))
        assertEquals(50_000L, parseRupiah("50k"))
        assertEquals(50_000L, parseRupiah("50K"))
        assertEquals(1_500_000L, parseRupiah("1,5jt"))
        assertEquals(1_500_000L, parseRupiah("1.5jt"))
        assertEquals(2_000_000L, parseRupiah("2 juta"))
        assertEquals(50_000L, parseRupiah("Rp 50.000"))
        assertEquals(50_000L, parseRupiah("Rp50.000"))
        assertEquals(50_000L, parseRupiah("rp 50rb"))
        assertEquals(50_000L, parseRupiah(" 50.000,00 "))
        assertEquals(1_250_000L, parseRupiah("1.250.000"))
    }

    @Test
    fun rejectsInvalid() {
        listOf("", "  ", "abc", "Rp", "0", "0rb", "-5000", "50.00", "50.000.0", "1,0005rb", "5e4", "1,5", "12.34.567", "9999999999999")
            .forEach { assertEquals("\"$it\"", null, parseRupiah(it)) }
    }
}

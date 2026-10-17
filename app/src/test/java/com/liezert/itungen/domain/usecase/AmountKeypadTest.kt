package com.liezert.itungen.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountKeypadTest {

    private fun type(vararg keys: String) = keys.fold("") { expr, key ->
        when (key) {
            "<" -> expr.dropLast(1)
            in OPERATORS.map(Char::toString) -> keypadOperator(expr, key[0])
            else -> keypadDigit(expr, key)
        }
    }

    @Test
    fun input_rules() {
        assertEquals("", type("0", "000"))                 // tanpa nol di depan
        assertEquals("15000", type("15", "000"))
        assertEquals("", type("+"))                        // operator di awal diabaikan
        assertEquals("5×", type("5", "+", "×"))             // operator menggantikan operator
        assertEquals("5+0", type("5", "+", "0"))            // nol tunggal boleh (C1: "5 ÷ 0" harus terlihat)
        assertEquals("5+0", type("5", "+", "0", "000"))     // tanpa nol di depan
        assertEquals("5+3", type("5", "+", "0", "3"))       // nol tunggal diganti angka berikutnya
        assertEquals("1", type("12", "<"))
        assertEquals("999999999999", type("999999999999", "9")) // maks 12 digit
        assertEquals(21_000L, evaluateKeypad(type("1", "5", "000", "+", "2", "000", "×", "3")))
    }

    @Test
    fun evaluate_precedenceAndRounding() {
        assertNull(evaluateKeypad(""))
        assertEquals(15_000L, evaluateKeypad("15000"))
        assertEquals(15_000L, evaluateKeypad("15000+"))     // operator di akhir diabaikan
        assertEquals(21_000L, evaluateKeypad("15000+2000×3"))
        assertEquals(3_333L, evaluateKeypad("10000÷3"))
        assertEquals(6_667L, evaluateKeypad("20000÷3"))
        assertEquals(-5_000L, evaluateKeypad("5000-10000"))
        assertNull(evaluateKeypad("5÷0"))
        assertEquals(Long.MAX_VALUE, evaluateKeypad("999999999999×999999999999"))
    }

    @Test
    fun format_groupsThousands() {
        assertEquals("15.000 + 2.000", formatKeypad("15000+2000"))
        assertEquals("1.000.000 ÷ ", formatKeypad("1000000÷"))
        assertEquals("", formatKeypad(""))
    }

    /** Bug C1/C2: "5 ÷ 0" dulu diam-diam jadi 5; "7 ÷ 2" dibulatkan tanpa tanda. */
    @Test
    fun divideByZero_andRounding_areVisible() {
        assertNull(evaluateKeypad(type("5", "÷", "0")))
        assertEquals(true, keypadRounds("7÷2"))
        assertEquals(false, keypadRounds("8÷2"))
        assertEquals(false, keypadRounds("15000+2000"))
    }
}

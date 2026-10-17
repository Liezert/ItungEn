package com.liezert.itungen.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/** Fase 1: nominal pemasukan/pengeluaran tanpa tanda +/-; jenisnya dibedakan warna, ikon, dan teks aksesibilitas. */
class AmountFormatTest {
    @Test
    fun amountText_hasNoSign() {
        assertEquals("Rp50.000", amountText(50_000))
        assertEquals("Rp50.000", amountText(-50_000)) // arah ditentukan jenis, bukan tanda nominal
        assertEquals("Rp1.25M", amountText(1_250_000, short = true))
    }
}

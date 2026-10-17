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

    @Test
    fun description_namesTheKind() {
        assertEquals("Pengeluaran Rp50.000", amountDescription(50_000, AmountKind.EXPENSE))
        assertEquals("Pemasukan Rp1.500.000", amountDescription(1_500_000, AmountKind.INCOME))
        assertEquals("Rp475.000", amountDescription(475_000, AmountKind.NEUTRAL))
        assertEquals("-Rp20.000", amountDescription(-20_000, AmountKind.NEUTRAL)) // saldo minus tetap bertanda
    }
}

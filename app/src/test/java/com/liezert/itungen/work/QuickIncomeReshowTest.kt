package com.liezert.itungen.work

import org.junit.Assert.assertEquals
import org.junit.Test

/** Notifikasi catat cepat yang diusap hanya dipasang lagi bila toggle aktif, izin ada, dan belum ada yang tampil. */
class QuickIncomeReshowTest {
    @Test
    fun reshow_onlyWhenEnabledPermittedAndNotShowing() {
        assertEquals(true, shouldReshow(enabled = true, permitted = true, alreadyShowing = false))
        assertEquals(false, shouldReshow(enabled = false, permitted = true, alreadyShowing = false)) // toggle mati
        assertEquals(false, shouldReshow(enabled = true, permitted = false, alreadyShowing = false)) // izin dicabut
        assertEquals(false, shouldReshow(enabled = true, permitted = true, alreadyShowing = true)) // konfirmasi tampil
    }
}

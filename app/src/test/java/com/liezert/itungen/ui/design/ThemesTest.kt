package com.liezert.itungen.ui.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fase 4–6: daftar tema dan aksesibilitas token di setiap tema (mode gelap dan terang). */
class ThemesTest {
    private fun contrast(a: Color, b: Color): Float {
        val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (hi + 0.05f) / (lo + 0.05f)
    }

    @Test
    fun unknownId_fallsBackToLiquidGlass() {
        assertEquals("liquid_glass", Themes.byId("tidak-ada").id)
        assertEquals("liquid_glass", Themes.byId(null).id)
        assertEquals(Themes.all.size, Themes.all.map { it.id }.toSet().size)
    }

    /** Teks utama ≥ 4,5:1 di atas latar dan kartu; nominal hijau/merah ≥ 3:1 dan berbeda satu sama lain. */
    @Test
    fun everyTheme_hasReadableTextAndAmounts() {
        for (spec in Themes.all) for (dark in listOf(true, false)) {
            val c = spec.colors(dark, null)
            val card = c.surface.compositeOver(c.background)
            val where = "${spec.id} ${if (dark) "gelap" else "terang"}"
            assertTrue("$where: teks ${contrast(c.text, c.background)}", contrast(c.text, c.background) >= 4.5f)
            assertTrue("$where: teks di kartu ${contrast(c.text, card)}", contrast(c.text, card) >= 4.5f)
            assertTrue("$where: pemasukan ${contrast(c.income, card)}", contrast(c.income, card) >= 3f)
            assertTrue("$where: pengeluaran ${contrast(c.expense, card)}", contrast(c.expense, card) >= 3f)
            assertTrue("$where: pemasukan = pengeluaran", c.income != c.expense)
            // Chip "Tercapai" di Goals: teks onGreen di atas hijau.
            assertTrue("$where: chip tercapai ${contrast(c.onGreen, c.green)}", contrast(c.onGreen, c.green) >= 4.5f)
        }
    }
}

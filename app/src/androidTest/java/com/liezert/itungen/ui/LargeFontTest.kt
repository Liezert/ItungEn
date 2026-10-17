package com.liezert.itungen.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.liezert.glass.components.GlassOverlayHost
import com.liezert.glass.components.LocalGlassBackdrop
import com.liezert.glass.components.LocalGlassOverlayHost
import com.liezert.itungen.ui.account.GoogleSignInButton
import com.liezert.itungen.ui.components.ItungAlertDialog
import com.liezert.itungen.ui.design.ActiveTheme
import com.liezert.itungen.ui.design.ThemeSpec
import com.liezert.itungen.ui.design.Themes
import com.liezert.itungen.ui.design.themes.glass.LiquidGlassTheme
import com.liezert.itungen.ui.goals.GoalsContent
import com.liezert.itungen.ui.theme.ItungEnTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Audit Fase 4 (huruf 150%, HP 360 dp): label tidak terpotong dan kata tidak dipecah di tengah ("Kemba / li",
 * "Tabung / an", "Masuk dengan / Google" yang terpotong). Di setiap tema, karena tiap tema punya tombol sendiri.
 */
@RunWith(Parameterized::class)
class LargeFontTest(@Suppress("unused") private val name: String, private val theme: ThemeSpec) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun themes() = Themes.all.map { arrayOf(it.id, it) }
    }

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun applyTheme() {
        ActiveTheme.spec = theme
    }

    @After
    fun resetTheme() {
        ActiveTheme.spec = LiquidGlassTheme
    }

    private fun large(fontScale: Float = 1.5f, content: @Composable () -> Unit) = compose.setContent {
        val host = remember { GlassOverlayHost() }
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
            ItungEnTheme {
                CompositionLocalProvider(LocalGlassBackdrop provides emptyBackdrop(), LocalGlassOverlayHost provides host) {
                    Box(Modifier.width(360.dp).fillMaxSize()) {
                        content()
                        host.Content()
                    }
                }
            }
        }
    }

    private fun SemanticsNodeInteraction.layout(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results)
        return results.first()
    }

    /** Teks boleh membungkus, tetapi hanya di spasi, dan tidak boleh terpotong. */
    private fun assertReadable(text: String) {
        val layout = compose.onNodeWithText(text).layout()
        // Lebar tidak diperiksa: hasil GetTextLayoutResult memakai lebar batas sebagai lebar paragraf, jadi
        // didOverflowWidth selalu true. Teks yang terlalu lebar terlihat sebagai baris berlebih, elipsis, atau tinggi.
        val clipped = layout.didOverflowHeight || layout.multiParagraph.didExceedMaxLines ||
            layout.isLineEllipsized(layout.lineCount - 1)
        assertFalse("\"$text\" terpotong (tinggi ${layout.size.height} < ${layout.multiParagraph.height}?)", clipped)
        for (line in 0 until layout.lineCount - 1) {
            val end = layout.getLineEnd(line)
            assertTrue("\"$text\" dipecah di tengah kata", text[end - 1] == ' ' || text.getOrNull(end) == ' ')
        }
    }

    @Test
    fun dialogButtons_stayWhole() = dialogButtons("Kembali", "Hapus")

    /** "Delete" lebih lebar dari setengah dialog di 150% (dulu "Delet / e" walau "Back" pendek). */
    @Test
    fun dialogButtons_stayWhole_english() = dialogButtons("Back", "Delete")

    private fun dialogButtons(dismiss: String, confirm: String) {
        large {
            ItungAlertDialog(
                visible = true,
                title = "Hapus \"Helm baru\"?",
                text = "Dana Rp350.000 kembali ke saldo utama.",
                confirmLabel = confirm,
                onConfirm = {},
                onDismiss = {},
                dismissLabel = dismiss,
            )
        }
        for (label in listOf(dismiss, confirm)) {
            assertReadable(label)
            assertEquals("\"$label\" satu baris", 1, compose.onNodeWithText(label).layout().lineCount)
        }
    }

    @Test
    fun goalsTitle_wrapsAtWords() {
        large { GoalsContent(onAddGoal = {}) }
        assertReadable("Target Tabungan")
        assertReadable("Tambah Target")
    }

    /** Huruf normal: tombol tetap di samping judul (judul boleh membungkus di spasi), tidak turun ke bawah. */
    @Test
    fun goalsTitle_sharesRowWithButton_atNormalSize() {
        large(fontScale = 1f) { GoalsContent(onAddGoal = {}) }
        val title = compose.onNodeWithText("Target Tabungan").fetchSemanticsNode().boundsInRoot
        val button = compose.onNodeWithText("Tambah Target").fetchSemanticsNode().boundsInRoot
        assertTrue("tombol turun ke bawah judul: judul $title, tombol $button", button.top < title.bottom)
    }

    @Test
    fun googleButton_growsWithItsLabel() {
        large { GoogleSignInButton(busy = false) {} }
        assertReadable("Masuk dengan Google")
    }
}

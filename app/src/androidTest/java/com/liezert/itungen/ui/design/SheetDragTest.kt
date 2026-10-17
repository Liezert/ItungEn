package com.liezert.itungen.ui.design

import androidx.activity.ComponentActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.liezert.glass.components.GlassOverlayHost
import com.liezert.glass.components.LocalGlassBackdrop
import com.liezert.glass.components.LocalGlassOverlayHost
import com.liezert.itungen.ui.design.themes.glass.LiquidGlassTheme
import com.liezert.itungen.ui.theme.ItungEnTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.math.abs

/** Sheet di setiap tema bisa ditarik ke bawah untuk menutup, dengan ambang yang sama dan tanpa bentrok dengan scroll isinya. */
@RunWith(Parameterized::class)
class SheetDragTest(@Suppress("unused") private val name: String, private val theme: ThemeSpec) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun themes() = Themes.all.map { arrayOf(it.id, it) }
    }

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var visible by mutableStateOf(true)
    private var dismissed = 0
    private lateinit var scroll: ScrollState

    @Before
    fun applyTheme() {
        ActiveTheme.spec = theme
    }

    @After
    fun resetTheme() {
        ActiveTheme.spec = LiquidGlassTheme
    }

    /** [closes] = false: onDismiss tidak menutup sheet, seperti sheet yang meminta konfirmasi dulu. */
    private fun sheet(scrollable: Boolean = false, closes: Boolean = true) {
        compose.setContent {
            scroll = rememberScrollState()
            val host = remember { GlassOverlayHost() }
            ItungEnTheme {
                CompositionLocalProvider(LocalGlassBackdrop provides emptyBackdrop(), LocalGlassOverlayHost provides host) {
                    Box(Modifier.fillMaxSize()) {
                        ItungSheet(visible, onDismiss = { dismissed++; if (closes) visible = false }) {
                            Column(Modifier.testTag("body").fillMaxWidth().height(320.dp).then(if (scrollable) Modifier.verticalScroll(scroll) else Modifier)) {
                                repeat(if (scrollable) 30 else 1) { Text("Baris $it", Modifier.height(48.dp)) }
                            }
                        }
                        host.Content()
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun top() = compose.onNodeWithTag("body").fetchSemanticsNode().boundsInRoot.top

    /** Seret dari dekat atas isi sheet; [hold] = jari diam sebentar sebelum diangkat (kecepatan lepas ≈ 0). */
    private fun drag(distance: Dp, steps: Int = 30, hold: Boolean = true) = compose.onNodeWithTag("body").performTouchInput {
        down(Offset(centerX, 40.dp.toPx()))
        repeat(steps) { moveBy(Offset(0f, distance.toPx() / steps), delayMillis = 16) }
        if (hold) advanceEventTime(300)
        up()
    }

    private fun awaitDismissed(count: Int) = compose.waitUntil(3_000) { dismissed == count }

    @Test
    fun dragPastThreshold_dismisses() {
        sheet()
        drag(200.dp)
        awaitDismissed(1)
        assertEquals(false, visible)
    }

    @Test
    fun quickFlick_dismissesBeforeDistance() {
        sheet()
        drag(60.dp, steps = 4, hold = false) // ± 2.800 px/detik, di atas ambang kecepatan
        awaitDismissed(1)
    }

    @Test
    fun shortSlowDrag_springsBack() {
        sheet()
        val before = top()
        drag(40.dp)
        compose.waitUntil(3_000) { abs(top() - before) < 1f }
        assertEquals(0, dismissed)
    }

    @Test
    fun scrolledContent_scrollsFirst_thenDragsSheetFromTop() {
        sheet(scrollable = true)
        val start = with(compose.density) { 150.dp.roundToPx() }
        compose.runOnIdle { runBlocking { scroll.scrollTo(start) } }

        drag(100.dp) // isinya belum di atas: hanya menggulir, sheet tidak ikut
        compose.waitForIdle()
        assertEquals(0, dismissed)
        assertTrue("scroll ${scroll.value}", scroll.value in 1 until start)

        drag(260.dp) // sisa scroll habis, lalu sisanya menarik sheet melewati ambang
        awaitDismissed(1)
        assertEquals(0, scroll.value)
    }

    @Test
    fun dismissThatKeepsSheetOpen_springsBack() {
        sheet(closes = false)
        val before = top()
        drag(200.dp)
        awaitDismissed(1)
        compose.waitUntil(3_000) { abs(top() - before) < 1f }
        assertEquals(true, visible)
    }
}

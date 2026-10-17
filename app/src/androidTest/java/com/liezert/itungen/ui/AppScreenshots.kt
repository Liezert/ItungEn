package com.liezert.itungen.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.liezert.itungen.ItungEnApp
import com.liezert.itungen.data.demo.seedDemoData
import com.liezert.itungen.ui.design.ActiveTheme
import com.liezert.itungen.ui.design.ThemeSpec
import com.liezert.itungen.ui.design.Themes
import com.liezert.itungen.ui.design.themes.glass.LiquidGlassTheme
import com.liezert.itungen.ui.navigation.AppNav
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.ItungEnTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Tangkapan layar app sungguhan (AppNav + data contoh) untuk audit visual di HP: setiap tema × gelap/terang, dan
 * skala huruf besar tanpa mengubah setelan sistem (Density diganti di dalam app). Hanya jalan dengan argumen
 * `-e shots true`; gambar disimpan di folder files/shots app tes lalu ditarik ke PC (lihat skrip shots.sh).
 */
@RunWith(AndroidJUnit4::class)
class AppScreenshots {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as ItungEnApp
    private val out = File(app.getExternalFilesDir(null), "shots").apply { mkdirs() }
    private var fontScale by mutableFloatStateOf(1f)
    /** Lebar layar pura-pura (dp), mis. 320 = HP kecil; 0 = lebar HP ini. */
    private var widthDp by mutableIntStateOf(0)

    @Before
    fun setUp() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("shots") == "true")
        val container = app.container
        container.onboarded = true
        runBlocking {
            if (container.goalRepository.imagesInUse().isEmpty()) {
                seedDemoData(container)
                val goals = container.goalRepository
                val helmet = goals.createGoal("Helm baru", 300_000, null, 0, container.goalImages.save(sampleImage()))
                goals.addFunds(helmet, 350_000)
            }
        }
        ActiveTheme.context = app
        compose.runOnUiThread { compose.activity.enableEdgeToEdge() }
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                Box(Modifier.fillMaxHeight().then(if (widthDp > 0) Modifier.width(widthDp.dp) else Modifier.fillMaxWidth())) {
                    ItungEnTheme { AppNav() }
                }
            }
        }
        compose.waitForIdle()
    }

    @After
    fun tearDown() {
        ActiveTheme.spec = LiquidGlassTheme
        Ink.dark = true
    }

    /** Gambar produk buatan sendiri (gradasi), bukan foto siapa pun. */
    private fun sampleImage(): ByteArray {
        val bitmap = Bitmap.createBitmap(1200, 800, Bitmap.Config.ARGB_8888)
        val paint = Paint().apply { shader = LinearGradient(0f, 0f, 1200f, 800f, 0xFF2D6CDF.toInt(), 0xFF9B5DE5.toInt(), Shader.TileMode.CLAMP) }
        Canvas(bitmap).drawRect(0f, 0f, 1200f, 800f, paint)
        return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
    }

    private fun use(theme: ThemeSpec, dark: Boolean) {
        compose.runOnUiThread {
            ActiveTheme.spec = theme
            Ink.dark = dark
        }
        compose.waitForIdle()
    }

    private fun shoot(name: String) {
        compose.waitForIdle()
        Thread.sleep(400) // animasi masuk/keluar layar selesai
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(out, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun back() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    private val variants = Themes.all.flatMap { theme -> listOf(true, false).map { theme to it } }

    private fun ThemeSpec.tag(dark: Boolean) = "${id}_${if (dark) "dark" else "light"}"

    /** Font besar dan layar sempit, di Liquid Glass gelap: [name]_<skala>[_sempit]. */
    private fun stress(name: String, open: () -> Unit) {
        use(LiquidGlassTheme, true)
        for ((scale, width) in listOf(1.3f to 0, 1.5f to 0, 1.3f to 320)) {
            compose.runOnUiThread {
                fontScale = scale
                widthDp = width
            }
            compose.waitForIdle()
            open()
            shoot("${name}_${(scale * 100).toInt()}${if (width > 0) "_sempit" else ""}")
        }
        compose.runOnUiThread {
            fontScale = 1f
            widthDp = 0
        }
    }

    @Test
    fun home() {
        for ((theme, dark) in variants) {
            use(theme, dark)
            shoot("home_${theme.tag(dark)}")
        }
        stress("home") {}
    }

    @Test
    fun goals() {
        for ((theme, dark) in variants) {
            use(theme, dark)
            compose.onNodeWithText("Goals").performClick()
            shoot("goals_list_${theme.tag(dark)}")
            compose.onNodeWithText("Helm baru").performClick()
            shoot("goals_detail_${theme.tag(dark)}")
            compose.onRoot().performTouchInput { swipeUp() }
            shoot("goals_detail_bottom_${theme.tag(dark)}")
            back()
            compose.onNodeWithText("Sepatu lari").performClick()
            shoot("goals_detail_partial_${theme.tag(dark)}")
            back()
            compose.onNodeWithText("Beranda").performClick()
        }
    }
}

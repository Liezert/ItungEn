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
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.swipeDown
import com.liezert.itungen.R
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

    private fun text(id: Int) = app.getString(id)

    /** Ketuk teks; digulir dulu bila ada di dalam daftar yang bisa digulir (navbar tidak). */
    private fun tap(label: String) {
        val node = compose.onNodeWithText(label)
        runCatching { node.performScrollTo() }
        node.performClick()
        compose.waitForIdle()
    }

    private fun tap(id: Int) = tap(text(id))

    /** Ikon di bilah atas/FAB: tidak digulir (performScrollTo bisa menaruhnya di bawah status bar). */
    private fun tapIcon(id: Int) {
        compose.onNodeWithContentDescription(text(id)).performClick()
        compose.waitForIdle()
    }

    /** Keluar dari sub-layar lewat tombolnya sendiri: bila navigasi gagal, tes berhenti jelas (bukan menutup activity). */
    private fun up(id: Int = R.string.action_back) {
        scroll(down = false, times = 6)
        tapIcon(id)
    }

    private fun scroll(down: Boolean, times: Int = 1) = repeat(times) {
        compose.onRoot().performTouchInput { if (down) swipeUp() else swipeDown() }
        compose.waitForIdle()
    }

    /**
     * Audit visual: semua layar utama, sheet, dan dialog untuk tema/mode yang diminta. Argumen instrumentasi:
     * themes=liquid_glass,receipt  modes=dark,light  scale=1.5  width=320  tag=a. Nama berkas: tag_layar_tema_mode.png.
     */
    /**
     * Berkas cadangan uji (data contoh + target bergambar + riwayat uang masuk), dibuat oleh kode ekspor app sendiri,
     * untuk menguji Pulihkan di APK rilis tanpa memilih foto dari galeri pengguna.
     */
    @Test
    fun backupFixture() = runBlocking {
        File(out, "itungen-uji.zip").outputStream().use { app.container.backup.export(it) }
    }

    @Test
    fun audit() {
        val args = InstrumentationRegistry.getArguments()
        val themes = args.getString("themes")?.split(",")?.map { Themes.byId(it) } ?: Themes.all
        val modes = args.getString("modes")?.split(",")?.map { it == "dark" } ?: listOf(true, false)
        val tag = args.getString("tag") ?: "a"
        compose.runOnUiThread {
            fontScale = args.getString("scale")?.toFloat() ?: 1f
            widthDp = args.getString("width")?.toInt() ?: 0
        }
        for (theme in themes) for (dark in modes) {
            use(theme, dark)
            walk { screen -> "${tag}_${screen}_${theme.tag(dark)}" }
        }
    }

    private fun walk(name: (String) -> String) {
        tap(R.string.tab_home)
        scroll(down = false, times = 3)
        shoot(name("home"))
        scroll(down = true, times = 3)
        shoot(name("homebottom"))
        scroll(down = false, times = 3)
        tapIcon(R.string.all_transactions)
        shoot(name("tx"))
        up()
        tapIcon(R.string.add_transaction)
        shoot(name("add"))
        tapIcon(R.string.action_close)

        tap(R.string.tab_insight)
        scroll(down = false, times = 3)
        shoot(name("insight"))
        scroll(down = true, times = 3)
        shoot(name("insightbottom"))

        tap(R.string.tab_goals)
        shoot(name("goals"))
        tap("Helm baru")
        shoot(name("detail"))
        scroll(down = true, times = 2)
        shoot(name("detailbottom"))
        tap(R.string.delete_goal)
        shoot(name("deletedialog"))
        back() // dialog
        tap(R.string.add_funds)
        shoot(name("addfunds"))
        back() // sheet
        up()
        tap(R.string.add_goal)
        shoot(name("creategoal"))
        back() // sheet

        tap(R.string.tab_profile)
        shoot(name("profile"))
        tap(R.string.settings)
        shoot(name("settings"))
        scroll(down = true, times = 1)
        shoot(name("settingsmid"))
        scroll(down = true, times = 4)
        shoot(name("settingsbottom"))
        scroll(down = false, times = 6)
        tap(R.string.budget_plan)
        shoot(name("budget"))
        scroll(down = true, times = 3)
        shoot(name("budgetbottom"))
        up()
        up()
        tap(R.string.tab_home)
    }
}

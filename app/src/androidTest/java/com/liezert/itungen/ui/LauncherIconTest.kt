package com.liezert.itungen.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.liezert.itungen.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Ikon peluncur: adaptif (latar + depan + monokrom untuk themed icon) dan dipakai manifest. */
@RunWith(AndroidJUnit4::class)
class LauncherIconTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun launcherIcon_isAdaptive_withMonochromeLayer() {
        assertEquals(R.mipmap.ic_launcher, context.applicationInfo.icon)
        for (id in listOf(R.mipmap.ic_launcher, R.mipmap.ic_launcher_round)) {
            val icon = context.getDrawable(id) as AdaptiveIconDrawable
            assertNotNull(icon.background)
            assertNotNull(icon.foreground)
            assertNotNull(icon.monochrome)
        }
    }

    /**
     * Pratinjau dirender oleh Android di HP ini (bukan perkiraan di PC): mask sistem HP, beberapa mask launcher lain,
     * themed icon terang/gelap, dan PNG 512×512 tanpa mask untuk docs/. Hanya dengan `-e shots true`.
     */
    @Test
    fun renderPreviews() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("shots") == "true")
        val out = File(context.getExternalFilesDir(null), "shots").apply { mkdirs() }
        val icon = context.getDrawable(R.mipmap.ic_launcher) as AdaptiveIconDrawable

        fun render(size: Int, draw: (Canvas, Int) -> Unit) =
            Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { draw(Canvas(it), size) }

        // Lapisan digambar langsung = area tampak 72 dp penuh, tanpa mask (format ikon Play Store).
        fun layers(canvas: Canvas, size: Int) {
            icon.setBounds(0, 0, size, size)
            icon.background.draw(canvas)
            icon.foreground.draw(canvas)
        }

        fun masked(mask: (Int) -> Path) = { canvas: Canvas, size: Int ->
            canvas.clipPath(mask(size))
            layers(canvas, size)
        }

        fun themed(bg: Int, fg: Int) = { canvas: Canvas, size: Int ->
            val s = size.toFloat()
            canvas.drawCircle(s / 2, s / 2, s / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bg })
            icon.setBounds(0, 0, size, size)
            with(icon.monochrome!!) {
                bounds = icon.foreground.bounds
                colorFilter = PorterDuffColorFilter(fg, PorterDuff.Mode.SRC_IN)
                draw(canvas)
                colorFilter = null
            }
        }

        fun rounded(fraction: Float) = { size: Int ->
            Path().apply { addRoundRect(0f, 0f, size.toFloat(), size.toFloat(), size * fraction, size * fraction, Path.Direction.CW) }
        }

        val tile = 192
        val variants = listOf<Pair<String, (Canvas, Int) -> Unit>>(
            "sistem HP" to { canvas, size -> icon.setBounds(0, 0, size, size); icon.draw(canvas) },
            "lingkaran" to masked { size -> Path().apply { addCircle(size / 2f, size / 2f, size / 2f, Path.Direction.CW) } },
            "squircle" to masked(rounded(0.36f)),
            "kotak bulat" to masked(rounded(0.18f)),
            "tetes" to masked { size ->
                val r = size / 2f
                Path().apply { addRoundRect(0f, 0f, size.toFloat(), size.toFloat(), floatArrayOf(r, r, r, r, size * 0.1f, size * 0.1f, r, r), Path.Direction.CW) }
            },
            "themed terang" to themed(0xFFD8E2FF.toInt(), 0xFF0F2A5C.toInt()),
            "themed gelap" to themed(0xFF2B3A55.toInt(), 0xFFD8E2FF.toInt()),
            "zona aman" to { canvas, size ->
                masked(rounded(0f))(canvas, size)
                // Lingkaran aman 66 dp dari 72 dp area tampak: isi penting harus di dalamnya.
                canvas.drawCircle(size / 2f, size / 2f, size * 33f / 72f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE; strokeWidth = 2f; color = Color.RED
                })
            },
        )

        val pad = 28
        val label = 36
        val sheet = Bitmap.createBitmap(variants.size * (tile + pad) + pad, 2 * (tile + pad + label) + pad, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheet)
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 22f; textAlign = Paint.Align.CENTER }
        listOf(0xFFF2F2F2.toInt() to Color.BLACK, 0xFF121212.toInt() to Color.WHITE).forEachIndexed { row, (bg, ink) ->
            val top = pad + row * (tile + pad + label)
            canvas.drawRect(0f, top - pad / 2f, sheet.width.toFloat(), top + tile + label + pad / 2f, Paint().apply { color = bg })
            text.color = ink
            variants.forEachIndexed { i, (name, draw) ->
                val left = pad + i * (tile + pad)
                canvas.drawBitmap(render(tile, draw), left.toFloat(), top.toFloat(), null)
                canvas.drawText(name, left + tile / 2f, top + tile + 28f, text)
            }
        }

        fun save(bitmap: Bitmap, name: String) =
            File(out, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        save(sheet, "icon_preview.png")
        save(render(512, ::layers), "icon_512.png")
    }
}

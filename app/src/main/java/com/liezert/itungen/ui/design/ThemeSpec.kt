package com.liezert.itungen.ui.design

import android.content.Context
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.liezert.itungen.ui.design.themes.brutal.BrutalTheme
import com.liezert.itungen.ui.design.themes.glass.LiquidGlassTheme
import com.liezert.itungen.ui.design.themes.material.MaterialYouTheme
import com.liezert.itungen.ui.design.themes.receipt.ReceiptTheme
import com.liezert.itungen.ui.theme.Ink

/**
 * Satu tema = token (warna, tipografi, bentuk, ukuran) + gaya komponen. Layar memanggil komponen semantik
 * ([ItungButton], [ItungCard], ...) dan token ([Ink], `appText`); nilainya datang dari tema aktif.
 *
 * Menambah tema: buat `object : ThemeSpec` di paket `themes/<nama>` lalu daftarkan di [Themes.all]. Layar tidak
 * perlu disentuh.
 */
interface ThemeSpec {
    /** Disimpan di pengaturan; jangan diubah setelah rilis. */
    val id: String
    val name: String

    /** Token warna mode gelap/terang. [context] hanya untuk tema yang mengambil warna sistem (Material You). */
    fun colors(dark: Boolean, context: Context?): ItungColors

    val type: ItungType
    val metrics: ItungMetrics
    val components: ItungComponents

    /** Kaca yang membiaskan isi layar butuh layar direkam ke backdrop; tema tanpa kaca tidak membayar biayanya. */
    val capturesScreen: Boolean get() = false

    /** Skema Material (date picker, komponen M3); null = diturunkan dari token [colors]. */
    fun materialScheme(dark: Boolean, context: Context): ColorScheme? = null
}

/**
 * Token warna. Nama mengikuti tema gelap Figma (lihat [Ink]): [background] = latar, [text] = teks utama.
 * [income]/[expense] wajib di setiap tema: nominal pemasukan/pengeluaran (AmountText).
 */
data class ItungColors(
    val background: Color,
    val text: Color,
    val title: Color,
    val gray: Color,
    val muted: Color,
    val subtle: Color,
    val orange: Color,
    val green: Color,
    val blue: Color,
    val red: Color,
    val cyan: Color,
    val purple: Color,
    val yellow: Color,
    val pink: Color,
    val accent: Color,
    val accentLight: Color,
    val onAccent: Color,
    val track: Color,
    val emptyIconBg: Color,
    val avatarBg: Color,
    val onGreen: Color,
    val navHome: Color,
    val navInsight: Color,
    val navGoals: Color,
    val navProfile: Color,
    val income: Color,
    val expense: Color,
    /** Isi bawaan kartu/kontrol (di kaca: lapisan gelap/terang α0,4 di atas latar). */
    val surface: Color,
    /** Garis tepi (kartu, input, navbar) untuk tema yang memakainya. */
    val border: Color,
)

/** Tipografi: keluarga font dan penyesuaian bobot per tema (mis. Neo-brutalism lebih tebal). */
data class ItungType(
    val family: FontFamily,
    /** Font angka/nominal; sama dengan [family] kecuali tema yang memakai monospace untuk nominal. */
    val amountFamily: FontFamily = family,
    val weight: (FontWeight) -> FontWeight = { it },
    /** Skala ukuran huruf: font yang lebih lebar (monospace) sedikit dikecilkan agar tata letak tetap muat. */
    val sizeScale: Float = 1f,
)

/** Ukuran non-warna. Sudut kartu dan tombol ditentukan masing-masing tema di komponennya. */
data class ItungMetrics(
    val cardRadius: Dp,
    val controlRadius: Dp,
    val borderWidth: Dp,
    /** Offset bayangan keras (tanpa blur); 0 = tanpa bayangan. */
    val shadowOffset: Dp,
    /** Sudut gambar (thumbnail goal, dsb.): lebih kecil dari kartu agar tetap terlihat sebagai gambar di dalam kartu. */
    val imageRadius: Dp = (cardRadius * 0.6f).coerceAtMost(16.dp),
)

/** Peran permukaan, agar tema bisa membedakan kartu (mis. tepi bergerigi struk) dari kolom input dan panel. */
enum class SurfaceRole { CARD, FIELD, PANEL }

/** Navbar bawah atau kontrol segmen di dalam layar (Pengeluaran/Pemasukan, periode anggaran). */
enum class TabsRole { NAVBAR, SEGMENTED }

/**
 * Gaya komponen satu tema. Fungsi di sini tidak punya nilai bawaan (aturan Compose untuk fungsi abstrak):
 * layar memanggil pembungkus di Components.kt yang mengisi nilai bawaan lalu meneruskan ke tema aktif.
 */
interface ItungComponents {
    @Composable fun Background(vignette: Boolean, modifier: Modifier)

    /** Penutup status bar saat isi discroll di bawahnya. */
    @Composable fun StatusBarCover(modifier: Modifier)

    @Composable fun Button(
        onClick: () -> Unit,
        modifier: Modifier,
        tint: Color,
        surfaceColor: Color,
        enabled: Boolean,
        contentPadding: Dp,
        content: @Composable RowScope.() -> Unit,
    )

    @Composable fun Surface(
        role: SurfaceRole,
        modifier: Modifier,
        shape: Shape?,
        surfaceColor: Color,
        tint: Brush?,
        dashedBorder: Color?,
        contentPadding: PaddingValues,
        contentAlignment: Alignment,
        onClick: (() -> Unit)?,
        content: @Composable BoxScope.() -> Unit,
    )

    @Composable fun Tabs(
        role: TabsRole,
        selectedTabIndex: () -> Int,
        onTabSelected: (Int) -> Unit,
        onTabReselected: (Int) -> Unit,
        tabsCount: Int,
        modifier: Modifier,
        accentColor: Color,
        height: Dp,
        content: @Composable RowScope.() -> Unit,
    )

    @Composable fun RowScope.Tab(onClick: () -> Unit, modifier: Modifier, selected: Boolean, content: @Composable ColumnScope.() -> Unit)

    @Composable fun Switch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier)

    @Composable fun Sheet(visible: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit)

    @Composable fun Dialog(visible: Boolean, onDismiss: () -> Unit, modifier: Modifier, content: @Composable ColumnScope.() -> Unit)

    @Composable fun Divider(modifier: Modifier, color: Color)

    /** Baris transaksi: ikon, judul + keterangan, nominal (Struk menambah titik pengisi antara judul dan nominal). */
    @Composable fun TransactionRow(
        icon: @Composable () -> Unit,
        title: @Composable () -> Unit,
        subtitle: @Composable () -> Unit,
        amount: @Composable () -> Unit,
        dense: Boolean,
        modifier: Modifier,
    )

    /** Kartu bergambar (goal): gambar setinggi [imageHeight] di atas, isi di bawah. */
    @Composable fun ImageCard(
        modifier: Modifier,
        imageHeight: Dp,
        image: @Composable BoxScope.() -> Unit,
        content: @Composable ColumnScope.() -> Unit,
    )

    @Composable fun TopBarTitle(text: String)
}

/** Semua tema yang bisa dipilih di Pengaturan; yang pertama adalah bawaan. */
object Themes {
    val all: List<ThemeSpec> = listOf(LiquidGlassTheme, ReceiptTheme, BrutalTheme, MaterialYouTheme)
    fun byId(id: String?): ThemeSpec = all.firstOrNull { it.id == id } ?: LiquidGlassTheme
}

/**
 * Tema aktif. Komponen membacanya lewat [LocalItungTheme]; token [Ink] dan `appText` (dipakai juga di luar
 * komposisi) membacanya langsung dari sini. Mengganti [spec] atau mode gelap langsung menggambar ulang UI.
 */
object ActiveTheme {
    var spec: ThemeSpec by mutableStateOf(LiquidGlassTheme)

    /** Konteks aplikasi untuk warna sistem (Material You); null di pratinjau/tes. */
    var context: Context? = null

    val colors: ItungColors by derivedStateOf { spec.colors(Ink.dark, context) }
}

val LocalItungTheme = compositionLocalOf<ThemeSpec> { LiquidGlassTheme }

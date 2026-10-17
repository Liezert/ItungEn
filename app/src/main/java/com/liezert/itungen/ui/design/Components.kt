package com.liezert.itungen.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.liezert.itungen.ui.theme.Ink

/*
 * Komponen semantik yang dipanggil layar. Masing-masing hanya mengisi nilai bawaan lalu meneruskan ke tema aktif
 * (LocalItungTheme); gayanya ada di ui/design/themes/<tema>.
 */

private val theme: ItungComponents @Composable get() = LocalItungTheme.current.components

/** Isi bawaan kartu/kontrol tema aktif (di kaca: GlassDefaults.Surface). */
object ItungDefaults {
    val surface: Color get() = Ink.Surface
}

/**
 * Tombol. [tint] = tombol utama berwarna (mis. Simpan); [surfaceColor] = isi khusus (mis. chip terpilih);
 * keduanya tidak diisi = tombol biasa tema.
 */
@Composable
fun ItungButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    enabled: Boolean = true,
    contentPadding: Dp = 16.dp,
    content: @Composable RowScope.() -> Unit,
) = theme.Button(onClick, modifier, tint, surfaceColor, enabled, contentPadding, content)

/** Permukaan dasar (panel, kolom input). Kartu berisi kolom memakai [ItungCard]. */
@Composable
fun ItungSurface(
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    surfaceColor: Color = ItungDefaults.surface,
    tint: Brush? = null,
    dashedBorder: Color? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    contentAlignment: Alignment = Alignment.TopStart,
    onClick: (() -> Unit)? = null,
    role: SurfaceRole = SurfaceRole.PANEL,
    content: @Composable BoxScope.() -> Unit,
) = theme.Surface(role, modifier, shape, surfaceColor, tint, dashedBorder, contentPadding, contentAlignment, onClick, content)

/** Kartu: permukaan peran [SurfaceRole.CARD] berisi kolom selebar kartu. */
@Composable
fun ItungCard(
    modifier: Modifier = Modifier,
    padding: Dp = 21.dp,
    surfaceColor: Color = ItungDefaults.surface,
    tint: Brush? = null,
    dashedBorder: Color? = null,
    shape: Shape? = null,
    onClick: (() -> Unit)? = null,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) = ItungSurface(
    modifier = modifier,
    role = SurfaceRole.CARD,
    shape = shape,
    surfaceColor = surfaceColor,
    tint = tint,
    dashedBorder = dashedBorder,
    onClick = onClick,
    contentPadding = PaddingValues(padding),
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = verticalArrangement, horizontalAlignment = horizontalAlignment, content = content)
}

/** Navbar bawah ([TabsRole.NAVBAR]) atau kontrol segmen di dalam layar ([TabsRole.SEGMENTED]). Isinya [ItungTab]. */
@Composable
fun ItungTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (Int) -> Unit,
    tabsCount: Int,
    accentColor: Color,
    modifier: Modifier = Modifier,
    role: TabsRole = TabsRole.SEGMENTED,
    height: Dp = 64.dp,
    onTabReselected: (Int) -> Unit = {},
    content: @Composable RowScope.() -> Unit,
) = theme.Tabs(role, selectedTabIndex, onTabSelected, onTabReselected, tabsCount, modifier, accentColor, height, content)

@Composable
fun RowScope.ItungTab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) = with(theme) { Tab(onClick, modifier, selected, content) }

@Composable
fun ItungSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) =
    theme.Switch(checked, onCheckedChange, modifier)

/** Sheet dari bawah; tutup dengan tap di luar, tombol back, atau seret. */
@Composable
fun ItungSheet(visible: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) =
    theme.Sheet(visible, onDismiss, content)

@Composable
fun ItungDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier.padding(40.dp),
    content: @Composable ColumnScope.() -> Unit,
) = theme.Dialog(visible, onDismiss, modifier, content)

/** Garis pemisah tipis (Struk: putus-putus). */
@Composable
fun ItungDivider(modifier: Modifier = Modifier, color: Color = Ink.white(0.05f)) = theme.Divider(modifier, color)

@Composable
fun ItungTransactionRow(
    icon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    subtitle: @Composable () -> Unit,
    amount: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    /** Baris rapat (Semua Transaksi) vs longgar (Beranda). */
    dense: Boolean = false,
) = theme.TransactionRow(icon, title, subtitle, amount, dense, modifier)

/** Kartu goal bergambar: gambar setinggi [imageHeight] di atas, isi di bawah. */
@Composable
fun ItungImageCard(
    modifier: Modifier = Modifier,
    imageHeight: Dp = 192.dp,
    image: @Composable BoxScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) = theme.ImageCard(modifier, imageHeight, image, content)

/**
 * Bingkai gambar (mis. thumbnail goal) mengikuti tema: sudut [ItungMetrics.imageRadius], tepi setebal
 * [ItungMetrics.borderWidth] dan bayangan keras [ItungMetrics.shadowOffset] bila tema memakainya. Isinya dipotong
 * ke bingkai, dengan latar kosong yang rapi selama gambar dimuat atau bila belum ada gambar.
 */
@Composable
fun Modifier.itungImageFrame(): Modifier {
    val metrics = LocalItungTheme.current.metrics
    val shape = RoundedCornerShape(metrics.imageRadius)
    val edge = Ink.Border
    return this
        .then(
            if (metrics.shadowOffset > 0.dp) Modifier.drawBehind {
                val o = metrics.shadowOffset.toPx()
                translate(o, o) { drawOutline(shape.createOutline(size, layoutDirection, this), edge) }
            } else Modifier
        )
        .clip(shape)
        .background(Ink.EmptyIconBg)
        .then(if (metrics.borderWidth > 0.dp) Modifier.border(metrics.borderWidth, edge, shape) else Modifier)
}

@Composable
fun ItungBackground(vignette: Boolean, modifier: Modifier = Modifier) = theme.Background(vignette, modifier)

@Composable
fun ItungStatusBarCover(modifier: Modifier = Modifier) = theme.StatusBarCover(modifier)

@Composable
fun ItungTopBarTitle(text: String) = theme.TopBarTitle(text)

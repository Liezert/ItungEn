package com.liezert.itungen.ui.components

import androidx.compose.ui.res.stringResource
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.liezert.glass.components.PlainBackdrop
import com.liezert.itungen.R
import com.liezert.itungen.ui.theme.Bold
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.appText
import androidx.compose.ui.text.style.TextOverflow
import com.liezert.itungen.ui.design.ItungDefaults
import com.liezert.itungen.ui.design.ItungButton
import com.liezert.itungen.ui.design.ItungTopBarTitle

/** Gutter horizontal yang dipakai semua frame desain. */
fun Modifier.screenPad(): Modifier = padding(horizontal = 20.dp)

/** Menampilkan SVG Figma yang sudah dikonversi pada ukuran aslinya. */
@Composable
fun Ico(@DrawableRes id: Int, modifier: Modifier = Modifier, contentDescription: String? = null) {
    // Ikon Figma berwarna terang (putih / abu muda) untuk tema gelap; di tema terang diwarnai ulang agar terlihat.
    val tint = when {
        Ink.dark -> null
        id in PrimaryIcons -> Ink.White
        id in MutedIcons -> Ink.Muted
        else -> null
    }
    Image(painterResource(id), contentDescription, modifier, colorFilter = tint?.let { ColorFilter.tint(it) })
}

private val PrimaryIcons = setOf(
    R.drawable.ic_add_food, R.drawable.ic_backspace, R.drawable.ic_chevron_right, R.drawable.ic_close,
    R.drawable.ic_delete, R.drawable.ic_history, R.drawable.ic_inc_gaji, R.drawable.ic_profile_avatar, R.drawable.ic_settings,
    R.drawable.ic_add_bill, R.drawable.ic_add_health, R.drawable.ic_add_education,
)
private val MutedIcons = setOf(
    R.drawable.ic_add_car, R.drawable.ic_add_film, R.drawable.ic_add_note, R.drawable.ic_add_other, R.drawable.ic_back,
    R.drawable.ic_inc_bonus, R.drawable.ic_inc_other, R.drawable.ic_inc_transfer, R.drawable.ic_search,
    R.drawable.ic_eye, R.drawable.ic_eye_off, R.drawable.ic_tx_bag,
)

/** Kanvas hitam; Beranda dan Semua Transaksi menambah vinyet lembut di atas (§6.1, hanya tema gelap). */
@Composable
fun AppBackground(vignette: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(Ink.Black)
            .then(if (vignette && Ink.dark) Modifier.drawBehind { drawVignette(size.width) } else Modifier)
    )
}

/**
 * Latar app sebagai backdrop kaca di dalam layar. Latar polos/vinyet halus tidak berubah oleh blur, lensa, dan
 * vibrancy, jadi kaca cukup menggambar latar di posisinya sendiri ([PlainBackdrop]): tampilan sama tanpa merekam
 * layer latar dan menjalankan shader setiap frame saat discroll (docs/PERFORMANCE.md).
 * Kaca yang melayang di atas konten (navbar, FAB, sheet, dialog, toast) tetap membiaskan isi layar sungguhan.
 */
class BackgroundBackdrop(private val color: Color, private val vignette: Boolean) : PlainBackdrop {
    override val isCoordinatesDependent: Boolean get() = vignette

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
    ) {
        drawRect(color)
        if (!vignette || coordinates == null || !coordinates.isAttached) return
        val root = coordinates.findRootCoordinates()
        val offset = root.localPositionOf(coordinates, Offset.Zero)
        translate(-offset.x, -offset.y) { drawVignette(root.size.width.toFloat()) }
    }
}

/** Latar layar saat ini untuk [com.liezert.glass.components.LocalGlassBackdrop] di dalam layar. */
@Composable
fun rememberBackgroundBackdrop(vignette: Boolean): BackgroundBackdrop {
    val color = Ink.Black
    val withVignette = vignette && Ink.dark
    return remember(color, withVignette) { BackgroundBackdrop(color, withVignette) }
}

/** Gradien radial dari fill body Figma: elips 1085,7 × 1052,1 dp berpusat di tepi atas layar selebar [width]. */
private fun DrawScope.drawVignette(width: Float) {
    translate(left = width / 2f, top = 0f) {
        scale(1085.7.dp.toPx(), 1052.1.dp.toPx(), pivot = Offset.Zero) {
            drawCircle(
                brush = Brush.radialGradient(
                    0.00f to Color(28, 28, 30, 102),
                    0.35f to Color(14, 14, 15, 178),
                    0.525f to Color(7, 7, 8, 217),
                    0.70f to Color(0, 0, 0, 255),
                    center = Offset.Zero,
                    radius = 1f,
                ),
                radius = 1f,
                center = Offset.Zero,
            )
        }
    }
}

/** Tombol bulat Liquid Glass 44 dp (ukuran iOS); area sentuhnya 48 dp (PRD §6). */
@Composable
fun ItungIconButton(
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    Box(modifier.minimumInteractiveComponentSize(), contentAlignment = Alignment.Center) {
        ItungButton(
            onClick = onClick,
            modifier = Modifier.size(size).semantics { this.contentDescription = contentDescription },
            contentPadding = 0.dp,
        ) { Ico(icon) }
    }
}

/**
 * App bar 88 dp: [leading] — "ItungEn" — [trailing]. Slot kosong diisi `Spacer(Modifier.size(48.dp))`
 * agar judul tetap di tengah.
 */
@Composable
fun TopBar(
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Row(
        modifier
            .fillMaxWidth()
            // Tombol 48 dp (area sentuh) menggantikan 40 dp desain, jadi padding bawah 16 → 12 menjaga tinggi 88 dp.
            .padding(start = 16.dp, end = 16.dp, top = maxOf(32.dp, statusTop), bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        ItungTopBarTitle(stringResource(R.string.app_name))
        trailing()
    }
}

@Composable
fun IconCircle(
    @DrawableRes icon: Int,
    size: Dp,
    background: Color,
    modifier: Modifier = Modifier,
    border: Color? = null,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .then(if (border != null) Modifier.border(1.dp, border, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) { Ico(icon) }
}

@Composable
fun ProgressBar(
    fraction: Float,
    brush: Brush,
    height: Dp,
    track: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth().height(height).clip(CircleShape).background(track)) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(CircleShape)
                .background(brush)
        )
    }
}

/** Chip filter / kategori: kapsul Liquid Glass; terpilih = permukaan [selectedFill]. */
@Composable
fun PillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedText: TextStyle,
    unselectedText: TextStyle,
    modifier: Modifier = Modifier,
    horizontal: Dp = 21.dp,
    height: Dp = 42.dp,
    selectedFill: Color = Ink.White,
    @DrawableRes icon: Int? = null,
) {
    ItungButton(
        onClick = onClick,
        modifier = modifier.height(height).semantics { this.selected = selected },
        surfaceColor = if (selected) selectedFill else ItungDefaults.surface,
        contentPadding = horizontal,
    ) {
        if (icon != null) Ico(icon)
        Text(label, style = if (selected) selectedText else unselectedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

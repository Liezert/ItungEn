package com.liezert.glass.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle

/**
 * Tempat sheet dan dialog kaca digambar: di atas navbar dan DI LUAR layer yang direkam backdrop, sehingga kacanya
 * bisa membiaskan seluruh layar tanpa merekam dirinya sendiri. Dialog Material/ModalBottomSheet tidak dipakai
 * karena tampil di window terpisah yang tidak bisa membaca backdrop.
 */
class GlassOverlayHost {
    internal val entries = mutableStateListOf<State<@Composable () -> Unit>>()

    @Composable
    fun Content() {
        entries.forEach { key(it) { it.value() } }
    }
}

val LocalGlassOverlayHost = staticCompositionLocalOf<GlassOverlayHost> { error("LocalGlassOverlayHost belum disediakan") }

/** Menggambar [content] di [GlassOverlayHost] selama pemanggil ada di komposisi. */
@Composable
fun GlassOverlay(content: @Composable () -> Unit) {
    val host = LocalGlassOverlayHost.current
    val latest = rememberUpdatedState(content)
    DisposableEffect(host, latest) {
        host.entries += latest
        onDispose { host.entries -= latest }
    }
}

internal val Bouncy = spring<Float>(dampingRatio = 0.82f, stiffness = 380f)

@Composable
private fun dimColor() =
    if (LocalLiquidLightTheme.current) Color(0xFF29293A).copy(0.23f)
    else Color(0xFF121212).copy(0.56f)

/** Mode hemat: tanpa blur, permukaan sheet/dialog dibuat pekat agar isi di belakangnya tidak mengganggu teks. */
@Composable
private fun containerColor(): Color {
    val economy = LocalGlassEconomy.current
    return if (LocalLiquidLightTheme.current) Color(0xFFFAFAFA).copy(if (economy) 0.94f else 0.6f)
    else Color(0xFF121212).copy(if (economy) 0.92f else 0.4f)
}

/**
 * Kaca dialog dari repo Kyant0 (catalog/destinations/DialogContent.kt): colorControls + blur + lensa dengan efek
 * kedalaman, highlight Plain. Peredup latar ikut direkam ke backdrop agar kacanya membiaskan versi yang redup,
 * persis seperti demo (di sana peredup digambar di atas wallpaper yang direkam).
 */
@Composable
private fun Modifier.dialogGlass(cornerRadius: Float): Modifier {
    val isLightTheme = LocalLiquidLightTheme.current
    val economy = LocalGlassEconomy.current
    val dim = dimColor()
    val container = containerColor()
    return drawBackdrop(
        backdrop = glassBackdrop(economy, LocalGlassBackdrop.current),
        shape = { RoundedRectangle(cornerRadius.dp) },
        effects = glassEffects(economy, LocalGlassBackdrop.current) {
            colorControls(
                brightness = if (isLightTheme) 0.2f else 0f,
                saturation = 1.5f
            )
            blur(if (isLightTheme) 16f.dp.toPx() else 8f.dp.toPx())
            lens(24f.dp.toPx(), 48f.dp.toPx(), depthEffect = true)
        },
        highlight = { Highlight.Plain },
        onDrawBackdrop = { drawBackdrop ->
            drawBackdrop()
            drawRect(dim)
        },
        onDrawSurface = { drawRect(container) }
    )
}

@Composable
private fun Scrim(visible: Boolean, onDismiss: () -> Unit) {
    val dim = dimColor()
    AnimatedVisibility(visible, enter = fadeIn(tween(250)), exit = fadeOut(tween(200))) {
        Box(Modifier.fillMaxSize().background(dim).pointerInput(Unit) { detectTapGestures { onDismiss() } })
    }
}

/** Dialog kaca di tengah layar (gaya alert iOS 26: muncul sedikit membesar lalu menyusut ke ukuran normal). */
@Composable
fun LiquidDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier.padding(40f.dp),
    content: @Composable ColumnScope.() -> Unit,
) = GlassOverlay {
    BackHandler(visible, onDismiss)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Scrim(visible, onDismiss)
        AnimatedVisibility(
            visible,
            enter = fadeIn(tween(200)) + scaleIn(Bouncy, initialScale = 1.15f),
            exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.95f),
        ) {
            Column(
                modifier
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .dialogGlass(48f)
                    .fillMaxWidth()
                    // Sentuhan di dalam dialog tidak menutupnya.
                    .pointerInput(Unit) { detectTapGestures { } },
                content = content,
            )
        }
    }
}

/**
 * Sheet kaca melayang ala iOS 26: inset dari tepi layar, sudut besar, pegangan di atas.
 * Tutup dengan tap di luar, tombol back, atau tarik ke bawah ([SheetDragState]).
 */
@Composable
fun LiquidSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) = GlassOverlay {
    BackHandler(visible, onDismiss)
    Box(Modifier.fillMaxSize()) {
        Scrim(visible, onDismiss)
        AnimatedVisibility(
            visible,
            Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(spring(dampingRatio = 0.86f, stiffness = 320f)) { it },
            exit = slideOutVertically(spring(stiffness = 500f)) { it },
        ) {
            val drag = rememberSheetDragState(visible, onDismiss)
            Column(
                Modifier
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(8.dp)
                    .then(drag.modifier)
                    .graphicsLayer { translationY = drag.offset }
                    .dialogGlass(38f)
                    .fillMaxWidth()
                    .pointerInput(Unit) { detectTapGestures { } },
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(36.dp, 5.dp).clip(Capsule()).background((if (LocalLiquidLightTheme.current) Color.Black else Color.White).copy(alpha = 0.3f)))
                }
                content()
            }
        }
    }
}

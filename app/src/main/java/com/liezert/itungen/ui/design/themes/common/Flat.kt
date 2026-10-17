package com.liezert.itungen.ui.design.themes.common

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.liezert.glass.components.GlassOverlay
import com.liezert.glass.components.SheetDragState
import com.liezert.glass.components.rememberSheetDragState

/*
 * Bagian bersama tema tanpa kaca (Struk, Neo-brutalism, Material You). Sheet dan dialog memakai host overlay dari
 * :glass hanya sebagai lapisan di atas layar (agar tampil di atas navbar, sama seperti tema kaca); tanpa efek kaca.
 */

/**
 * Sheet dari bawah: [panel] = gaya panel tema (latar, tepi, bentuk). Tutup dengan tap di luar, tombol back, atau tarik
 * ke bawah dengan ambang dan spring yang sama seperti sheet kaca ([SheetDragState]).
 */
@Composable
fun FlatSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    scrim: Color,
    handle: Color,
    panel: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) = GlassOverlay {
    BackHandler(visible, onDismiss)
    Box(Modifier.fillMaxSize()) {
        Scrim(visible, scrim, onDismiss)
        AnimatedVisibility(
            visible,
            Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(260)) { it },
            exit = slideOutVertically(tween(200)) { it },
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
                    .then(panel)
                    .fillMaxWidth()
                    .pointerInput(Unit) { detectTapGestures { } },
            ) {
                Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 12.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(36.dp, 4.dp).background(handle))
                }
                content()
            }
        }
    }
}

/** Dialog di tengah layar; [panel] = gaya panel tema. */
@Composable
fun FlatDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    scrim: Color,
    panel: Modifier,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) = GlassOverlay {
    BackHandler(visible, onDismiss)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Scrim(visible, scrim, onDismiss)
        AnimatedVisibility(visible, enter = fadeIn(tween(160)) + scaleIn(tween(160), 0.96f), exit = fadeOut(tween(120)) + scaleOut(tween(120), 0.96f)) {
            Column(
                modifier
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .then(panel)
                    .fillMaxWidth()
                    .pointerInput(Unit) { detectTapGestures { } },
                content = content,
            )
        }
    }
}

@Composable
private fun Scrim(visible: Boolean, color: Color, onDismiss: () -> Unit) {
    AnimatedVisibility(visible, enter = fadeIn(tween(200)), exit = fadeOut(tween(160))) {
        Box(Modifier.fillMaxSize().background(color).pointerInput(Unit) { detectTapGestures { onDismiss() } })
    }
}

/** Garis putus-putus horizontal setebal [thickness]. */
@Composable
fun DashedLine(color: Color, modifier: Modifier = Modifier, thickness: Dp = 1.dp, dash: Dp = 4.dp) {
    Canvas(modifier.fillMaxWidth().height(thickness)) {
        drawLine(
            color,
            Offset(0f, size.height / 2),
            Offset(size.width, size.height / 2),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), dash.toPx() * 0.75f)),
        )
    }
}

/** Persegi dengan tepi bawah bergerigi seperti struk yang disobek; gigi selebar [tooth]. */
class ZigzagBottomShape(private val tooth: Dp = 10.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val t = with(density) { tooth.toPx() }
        val depth = t / 2
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height - depth)
            var x = size.width
            var down = true
            while (x > 0f) {
                x = (x - t / 2).coerceAtLeast(0f)
                lineTo(x, if (down) size.height else size.height - depth)
                down = !down
            }
            lineTo(0f, size.height - depth)
            close()
        }
        return Outline.Generic(path)
    }
}

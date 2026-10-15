package com.liezert.itungen.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Satu potongan: warna + bobot (mis. total rupiah kategori). */
class DonutSlice(val color: Color, val value: Float)

/**
 * Diagram lingkaran berlubang. Potongan mulai dari jam 12 searah jarum jam, dipisah celah tipis,
 * dan "tumbuh" dari 0 saat pertama tampil / data berubah. Tanpa data: hanya lingkaran [track].
 */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    track: Color,
    modifier: Modifier = Modifier,
    thickness: Dp = 22.dp,
    center: @Composable BoxScope.() -> Unit = {},
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(slices.map { it.value }) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }
    val total = slices.sumOf { it.value.toDouble() }.toFloat()

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = thickness.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val arcSize = Size(diameter, diameter)
            drawArc(track, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
            if (total <= 0f) return@Canvas

            val gap = if (slices.count { it.value > 0f } > 1) 2.5f else 0f
            var start = -90f
            val sweepTotal = 360f * progress.value
            for (slice in slices) {
                val sweep = slice.value / total * sweepTotal
                if (sweep - gap > 0.1f) {
                    drawArc(
                        slice.color, start + gap / 2, sweep - gap, useCenter = false,
                        topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Butt),
                    )
                }
                start += sweep
            }
        }
        center()
    }
}

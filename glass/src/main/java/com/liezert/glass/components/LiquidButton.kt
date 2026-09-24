package com.liezert.glass.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.liezert.glass.tokens.GlassColors

// Angka §6.5, dikunci setelah uji di Realme 13+ (Tahap 2).
private const val PressedScale = 1.1f
private val PressSpring = spring<Float>(dampingRatio = 0.5f, stiffness = 400f)

/** Tombol Glass Riil: membesar saat ditekan, memantul sedikit melewati ukuran normal saat dilepas. */
@Composable
fun LiquidButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    /** Warna di atas kaca; default tint gelap agar isi tetap kontras (§6.2). */
    surface: Color = GlassColors.GlassTint,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // Spring kurang teredam sengaja melewati 0 saat lepas: itulah efek "melewati ukuran normal".
    val press by animateFloatAsState(if (pressed) 1f else 0f, PressSpring, label = "press")

    Box(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(2.dp.toPx())
                    lens(lerp(12f, 20f, press).dp.toPx(), lerp(24f, 36f, press).dp.toPx())
                },
                highlight = { Highlight.Default.copy(alpha = lerp(0.6f, 1f, press).coerceIn(0f, 1f)) },
                layerBlock = {
                    val scale = lerp(1f, PressedScale, press)
                    scaleX = scale
                    scaleY = scale
                },
                onDrawSurface = { drawRect(surface) },
            )
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

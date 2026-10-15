package com.liezert.glass.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import com.liezert.glass.utils.InteractiveHighlight

private val SurfaceEffects: BackdropEffectScope.() -> Unit = {
    vibrancy()
    blur(8.dp.toPx())
    lens(16.dp.toPx(), 32.dp.toPx())
}

object GlassDefaults {
    /** Sudut kurva kontinu (squircle) seperti kartu iOS 26. */
    val CardShape: Shape = RoundedRectangle(24.dp)

    /** containerColor dari repo Kyant0 (gelap #121212 / terang #FAFAFA, α0.4): menjaga teks kontras di atas kaca. */
    val Surface: Color
        @Composable get() = if (LocalLiquidLightTheme.current) Color(0xFFFAFAFA).copy(0.4f) else Color(0xFF121212).copy(0.4f)
}

/**
 * Permukaan Liquid Glass riil (library Backdrop): vibrancy + blur + lensa di tepi, highlight dan bayangan bawaan
 * library, seperti kontainer di demo scroll repo Kyant0. Membiaskan [LocalGlassBackdrop]; di atas [PlainBackdrop]
 * efeknya dilewati karena tidak mengubah piksel.
 * Bila [onClick] diberikan: cahaya mengikuti jari dan kaca sedikit membesar, sama seperti LiquidButton.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = GlassDefaults.CardShape,
    surfaceColor: Color = GlassDefaults.Surface,
    /** Lapisan warna di atas [surfaceColor] (mis. gradien aksen kartu saldo). */
    tint: Brush? = null,
    /** Garis putus-putus untuk keadaan kosong. */
    dashedBorder: Color? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    contentAlignment: Alignment = Alignment.TopStart,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val economy = LocalGlassEconomy.current
    val source = LocalGlassBackdrop.current
    val backdrop = glassBackdrop(economy, source)
    val animationScope = rememberCoroutineScope()
    val highlight = remember(animationScope, onClick != null) {
        if (onClick != null) InteractiveHighlight(animationScope) else null
    }

    Box(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = glassEffects(economy, source, SurfaceEffects),
                layerBlock = highlight?.let { h ->
                    {
                        val scale = lerp(1f, 1f + 4.dp.toPx() / size.height, h.pressProgress)
                        scaleX = scale
                        scaleY = scale
                    }
                },
                onDrawSurface = {
                    drawRect(surfaceColor)
                    if (tint != null) drawRect(tint)
                },
            )
            .then(if (dashedBorder != null) Modifier.dashedBorder(dashedBorder, shape) else Modifier)
            .then(
                if (onClick != null && highlight != null) {
                    Modifier
                        .clickable(interactionSource = null, indication = null, role = Role.Button, onClick = onClick)
                        .then(highlight.modifier)
                        .then(highlight.gestureModifier)
                } else {
                    Modifier
                }
            )
            .padding(contentPadding),
        contentAlignment = contentAlignment,
        content = content,
    )
}

private fun Modifier.dashedBorder(color: Color, shape: Shape): Modifier = drawWithContent {
    drawContent()
    val outline = shape.createOutline(size, layoutDirection, this)
    val dash = 3.dp.toPx()
    drawOutline(
        outline, color,
        // Separuh luar terpotong klip bentuk, jadi lebar 2 dp = garis 1 dp.
        style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))),
    )
}

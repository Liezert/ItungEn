package com.liezert.glass.components

import android.graphics.RenderEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.effect

private const val AlphaMaskShader = """
    uniform shader content;

    uniform float2 size;
    layout(color) uniform half4 tint;
    uniform float tintIntensity;

    half4 main(float2 coord) {
        float blurAlpha = smoothstep(size.y, size.y * 0.5, coord.y);
        float tintAlpha = smoothstep(size.y, size.y * 0.5, coord.y);
        return mix(content.eval(coord) * blurAlpha, tint * tintAlpha, tintIntensity);
    }"""

/**
 * Blur progresif ber-mask alfa dari demo ProgressiveBlur repo Kyant0: penuh di atas, memudar ke bawah.
 * Dipakai sebagai efek tepi scroll iOS 26 di bawah status bar. (Backdrop 1.0.6 belum punya runtimeShaderEffect,
 * jadi shader yang sama dirangkai lewat [effect].)
 */
@Composable
fun ProgressiveBlur(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    tint: Color =
        if (LocalLiquidLightTheme.current) Color.White
        else Color(0xFF808080),
    tintIntensity: Float = 0.8f,
) {
    Box(
        modifier.drawPlainBackdrop(
            backdrop = backdrop,
            shape = { RectangleShape },
            effects = {
                blur(4f.dp.toPx())
                val shader = obtainRuntimeShader("AlphaMask", AlphaMaskShader).apply {
                    setFloatUniform("size", size.width, size.height)
                    setColorUniform("tint", tint.toArgb())
                    setFloatUniform("tintIntensity", tintIntensity)
                }
                effect(RenderEffect.createRuntimeShaderEffect(shader, "content"))
            }
        )
    )
}

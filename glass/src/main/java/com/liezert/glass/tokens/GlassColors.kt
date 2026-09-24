package com.liezert.glass.tokens

import androidx.compose.ui.graphics.Color

// Nilai dari DESIGN_SPEC §6.1.
object GlassColors {
    val Background = Color(0xFF000000)
    val TextPrimary = Color.White
    val TextSecondary = Color(0xFF8E8E93)
    val AccentBlue = Color(0xFF0A84FF)
    val TabInactive = Color(0xB3C0C6D6)
    val NavBorder = Color(0x4D414754)

    // Tint gelap di atas Glass Riil agar teks tetap kontras (§6.2).
    val GlassTint = Color(0x66121212)
}

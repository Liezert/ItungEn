package com.liezert.itungen.ui.theme

import androidx.compose.ui.graphics.Color

/** Token warna dari Figma (DESIGN_SPEC §6.1). Tema gelap saja; tukar objek ini saat tema terang dibuat. */
object Ink {
    val Black = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)

    // teks
    val Title = Color(0xFFE2E2E2)
    val Gray = Color(0xFF8E8E93)
    val Muted = Color(0xFFC0C6D6)
    val Subtle = Color(0xFF8B91A0)

    // aksen sistem iOS
    val Orange = Color(0xFFFF9F0A)
    val Green = Color(0xFF32D74B)
    val Blue = Color(0xFF0A84FF)
    val Red = Color(0xFFFF453A)
    val Cyan = Color(0xFF64D2FF)

    // tambahan untuk kategori di luar empat yang digambar di Figma
    val Purple = Color(0xFFBF5AF2)
    val Yellow = Color(0xFFFFD60A)
    val Pink = Color(0xFFFF375F)

    // goals
    val Accent = Color(0xFF3E90FF)
    val AccentLight = Color(0xFFAAC7FF)
    val OnAccent = Color(0xFF002957)
    val Track = Color(0xFF2A2A2A)
    val EmptyIconBg = Color(0xFF1F1F1F)
    val AvatarBg = Color(0xFF1C1C1E)
    val OnGreen = Color(0xFF00390A)

    // navigasi bawah (aksen tab aktif)
    val NavHome = Color(0xFFFFAF4E)
    val NavInsight = Color(0xFF38BDF8).copy(alpha = 0.7f)
    val NavGoals = Color(0xFF34D399).copy(alpha = 0.7f)
    val NavProfile = Color(0xFFC084FC).copy(alpha = 0.7f)
    val NavInactive = Color(0xFFC0C6D6).copy(alpha = 0.7f)

    fun white(alpha: Float) = Color.White.copy(alpha = alpha)
}

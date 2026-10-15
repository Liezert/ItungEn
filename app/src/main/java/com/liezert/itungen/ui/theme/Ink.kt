package com.liezert.itungen.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Token warna dari Figma (DESIGN_SPEC §6.1) untuk tema gelap, plus padanan tema terang (warna sistem iOS).
 * Nama mengikuti tema gelap: [White] = teks utama, [Black] = latar. Nilainya dibaca dari [dark] saat komposisi,
 * jadi mengganti tema langsung menggambar ulang seluruh UI tanpa mengubah pemanggil.
 */
object Ink {
    /** Tema aktif. Diisi dari pengaturan saat app dibuka; diubah dari halaman Pengaturan. */
    var dark by mutableStateOf(true)

    private fun pick(dark: Long, light: Long) = Color(if (this.dark) dark else light)

    val Black get() = pick(0xFF000000, 0xFFF2F2F7)
    val White get() = pick(0xFFFFFFFF, 0xFF000000)

    // teks
    val Title get() = pick(0xFFE2E2E2, 0xFF1C1C1E)
    val Gray = Color(0xFF8E8E93)
    val Muted get() = pick(0xFFC0C6D6, 0xFF55596A)
    val Subtle get() = pick(0xFF8B91A0, 0xFF6E7383)

    // aksen sistem iOS (gelap / terang)
    val Orange get() = pick(0xFFFF9F0A, 0xFFFF9500)
    val Green get() = pick(0xFF32D74B, 0xFF34C759)
    val Blue get() = pick(0xFF0A84FF, 0xFF007AFF)
    val Red get() = pick(0xFFFF453A, 0xFFFF3B30)
    val Cyan get() = pick(0xFF64D2FF, 0xFF32ADE6)

    // tambahan untuk kategori di luar empat yang digambar di Figma
    val Purple get() = pick(0xFFBF5AF2, 0xFFAF52DE)
    val Yellow get() = pick(0xFFFFD60A, 0xFFFFCC00)
    val Pink get() = pick(0xFFFF375F, 0xFFFF2D55)

    // goals
    val Accent = Color(0xFF3E90FF)
    /** Teks/ikon beraksen; di tema terang lebih gelap agar terbaca di atas latar terang. */
    val AccentLight get() = pick(0xFFAAC7FF, 0xFF0060DF)
    val OnAccent = Color(0xFF002957)
    val Track get() = pick(0xFF2A2A2A, 0xFFE5E5EA)
    val EmptyIconBg get() = pick(0xFF1F1F1F, 0xFFE5E5EA)
    val AvatarBg get() = pick(0xFF1C1C1E, 0xFFE5E5EA)
    val OnGreen = Color(0xFF00390A)

    // navigasi bawah (aksen tab aktif)
    val NavHome get() = pick(0xFFFFAF4E, 0xFFFF9500)
    val NavInsight = Color(0xFF38BDF8).copy(alpha = 0.7f)
    val NavGoals = Color(0xFF34D399).copy(alpha = 0.7f)
    val NavProfile = Color(0xFFC084FC).copy(alpha = 0.7f)
    val NavInactive get() = Muted.copy(alpha = 0.7f)

    /** Lapisan tipis warna teks utama: putih transparan di tema gelap, hitam transparan di tema terang. */
    fun white(alpha: Float) = White.copy(alpha = alpha)
}

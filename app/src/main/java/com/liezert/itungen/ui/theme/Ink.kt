package com.liezert.itungen.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.liezert.itungen.ui.design.ActiveTheme

/**
 * Token warna. Nama mengikuti tema gelap Figma (DESIGN_SPEC §6.1): [White] = teks utama, [Black] = latar.
 * Nilainya dari tema aktif ([ActiveTheme], palet di `ui/design/themes/...`), dibaca saat komposisi, jadi mengganti
 * tema atau mode gelap langsung menggambar ulang seluruh UI tanpa mengubah pemanggil.
 */
object Ink {
    /** Mode gelap. Diisi dari pengaturan saat app dibuka; diubah dari halaman Pengaturan. */
    var dark by mutableStateOf(true)

    private val c get() = ActiveTheme.colors

    val Black get() = c.background
    val White get() = c.text

    // teks
    val Title get() = c.title
    val Gray get() = c.gray
    val Muted get() = c.muted
    val Subtle get() = c.subtle

    // aksen
    val Orange get() = c.orange
    val Green get() = c.green
    val Blue get() = c.blue
    val Red get() = c.red
    val Cyan get() = c.cyan

    /** Nominal pemasukan / pengeluaran (AmountText); wajib ada di setiap tema. */
    val Income get() = c.income
    val Expense get() = c.expense

    // tambahan untuk kategori di luar empat yang digambar di Figma
    val Purple get() = c.purple
    val Yellow get() = c.yellow
    val Pink get() = c.pink

    // goals
    val Accent get() = c.accent
    /** Teks/ikon beraksen; di tema terang lebih gelap agar terbaca di atas latar terang. */
    val AccentLight get() = c.accentLight
    val OnAccent get() = c.onAccent
    val Track get() = c.track
    val EmptyIconBg get() = c.emptyIconBg
    val AvatarBg get() = c.avatarBg
    val OnGreen get() = c.onGreen

    // navigasi bawah (aksen tab aktif)
    val NavHome get() = c.navHome
    val NavInsight get() = c.navInsight
    val NavGoals get() = c.navGoals
    val NavProfile get() = c.navProfile
    val NavInactive get() = Muted.copy(alpha = 0.7f)

    /** Isi bawaan kartu/kontrol tema aktif. */
    val Surface get() = c.surface
    val Border get() = c.border

    /** Lapisan tipis warna teks utama: putih transparan di tema gelap, hitam transparan di tema terang. */
    fun white(alpha: Float) = White.copy(alpha = alpha)
}

package com.liezert.glass.components

import androidx.compose.runtime.staticCompositionLocalOf
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.backdrops.emptyBackdrop

/**
 * Pengganti `isSystemInDarkTheme()` di komponen repo Kyant0: ItungEn gelap saja (DESIGN_SPEC §6), jadi tidak boleh
 * ikut tema sistem. Cabang warna terang dari repo tetap disimpan untuk tema terang nanti.
 */
val LocalLiquidLightTheme = staticCompositionLocalOf { false }

/**
 * Backdrop yang dibiaskan kaca bila tidak diberikan langsung. Di dalam layar = wallpaper;
 * di navbar/sheet/dialog = wallpaper + isi layar.
 */
val LocalGlassBackdrop = staticCompositionLocalOf<Backdrop> { error("LocalGlassBackdrop belum disediakan") }

/**
 * Mode hemat (Pengaturan, atau otomatis saat Penghemat Baterai menyala): semua kaca memakai versi ringan —
 * highlight tepi, bayangan, warna permukaan, dan animasi tekan tetap, tetapi latar tidak direkam dan tidak dibiaskan
 * (tanpa blur, lensa, vibrancy). Kartu statis selalu ringan (lihat [GlassSurface]).
 */
val LocalGlassEconomy = staticCompositionLocalOf { false }

/**
 * Backdrop berupa latar polos atau gradien halus (bukan isi layar). Blur, lensa, dan vibrancy di atas latar seperti
 * ini tidak mengubah piksel, jadi komponen kaca melewati efeknya dan cukup menggambar latarnya di posisi yang sama:
 * tampilan sama, tanpa merekam layer dan menjalankan shader setiap frame saat discroll.
 */
interface PlainBackdrop : Backdrop

/** Kaca tanpa efek backdrop. */
internal val NoEffects: BackdropEffectScope.() -> Unit = {}

/** Backdrop yang dipakai: di Mode hemat isi layar tidak dibiaskan (kosong); latar polos tetap digambar. */
internal fun glassBackdrop(economy: Boolean, backdrop: Backdrop): Backdrop =
    if (economy && backdrop !is PlainBackdrop) emptyBackdrop() else backdrop

/** [effects] untuk kaca yang membiaskan isi layar; dilewati di Mode hemat dan di atas [PlainBackdrop]. */
internal fun glassEffects(economy: Boolean, backdrop: Backdrop, effects: BackdropEffectScope.() -> Unit) =
    if (economy || backdrop is PlainBackdrop) NoEffects else effects

/** [effects] kecuali di Mode hemat (untuk kaca yang membiaskan elemen lain, bukan hanya latar). */
internal fun unlessEconomy(economy: Boolean, effects: BackdropEffectScope.() -> Unit) = if (economy) NoEffects else effects

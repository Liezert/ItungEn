package com.liezert.glass.components

import androidx.compose.runtime.compositionLocalOf
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.backdrops.emptyBackdrop

/**
 * Pengganti `isSystemInDarkTheme()` di komponen repo Kyant0: tema mengikuti pilihan di ItungEn, bukan tema sistem.
 *
 * Ketiga local di file ini bisa berubah saat app berjalan (Mode gelap, Mode hemat, latar per layar), jadi memakai
 * `compositionLocalOf` yang melacak pembacanya. Dengan `staticCompositionLocalOf`, kaca yang dilewati rekomposisi
 * (argumennya tidak berubah, mis. kartu berisi tombol saja) tetap memakai nilai lama: kartu "DATA" di Pengaturan
 * tertinggal satu tema setelah Mode gelap diganti.
 */
val LocalLiquidLightTheme = compositionLocalOf { false }

/**
 * Backdrop yang dibiaskan kaca bila tidak diberikan langsung. Di dalam layar = wallpaper;
 * di navbar/sheet/dialog = wallpaper + isi layar.
 */
val LocalGlassBackdrop = compositionLocalOf<Backdrop> { error("LocalGlassBackdrop belum disediakan") }

/**
 * Mode hemat (Pengaturan, atau otomatis saat Penghemat Baterai menyala): semua kaca memakai versi ringan —
 * highlight tepi, bayangan, warna permukaan, dan animasi tekan tetap, tetapi latar tidak direkam dan tidak dibiaskan
 * (tanpa blur, lensa, vibrancy). Kartu statis selalu ringan (lihat [GlassSurface]).
 */
val LocalGlassEconomy = compositionLocalOf { false }

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

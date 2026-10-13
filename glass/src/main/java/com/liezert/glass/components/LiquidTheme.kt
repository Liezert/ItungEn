package com.liezert.glass.components

import androidx.compose.runtime.staticCompositionLocalOf
import com.kyant.backdrop.Backdrop

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

package com.liezert.itungen.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.liezert.glass.components.LocalGlassEconomy
import com.liezert.glass.components.LocalLiquidLightTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Dibuat ulang tiap ganti tema: Ink membaca tema aktif.
private fun scheme() = (if (Ink.dark) darkColorScheme() else lightColorScheme()).copy(
    primary = Ink.Accent,
    onPrimary = Color.White,
    background = Ink.Black,
    surface = Ink.Black,
    onBackground = Ink.White,
    onSurface = Ink.White,
    error = Ink.Red,
)

/**
 * Tema gelap/terang mengikuti [Ink.dark]; komponen kaca :glass ikut lewat LocalLiquidLightTheme.
 * [glassEconomy] = Mode hemat: semua kaca versi ringan (lihat LocalGlassEconomy).
 */
@Composable
fun ItungEnTheme(glassEconomy: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = scheme(),
        typography = Typography().withFamily(InterFamily),
        content = {
            CompositionLocalProvider(
                LocalLiquidLightTheme provides !Ink.dark,
                LocalGlassEconomy provides glassEconomy,
                content = content,
            )
        },
    )
}

private val DateFormat = DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", Locale.forLanguageTag("id-ID"))

/** "Kamis, 24 Sep 2026". */
fun LocalDate.formatId(): String = format(DateFormat)

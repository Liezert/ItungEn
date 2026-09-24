package com.liezert.itungen.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.liezert.glass.tokens.GlassColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Tema gelap saja untuk MVP (§6). */
@Composable
fun ItungEnTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = GlassColors.AccentBlue,
            onPrimary = Color.White,
            background = GlassColors.Background,
            surface = GlassColors.Background,
            error = GlassColors.AccentRed,
        ),
        content = content,
    )
}

private val DateFormat = DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", Locale.forLanguageTag("id-ID"))

/** "Kamis, 24 Sep 2026". */
fun LocalDate.formatId(): String = format(DateFormat)

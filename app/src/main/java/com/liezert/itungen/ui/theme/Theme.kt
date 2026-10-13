package com.liezert.itungen.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DarkScheme = darkColorScheme(
    primary = Ink.Accent,
    onPrimary = Ink.OnAccent,
    background = Ink.Black,
    surface = Ink.Black,
    onBackground = Ink.White,
    onSurface = Ink.White,
    error = Ink.Red,
)

@Composable
fun ItungEnTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        typography = Typography().withFamily(InterFamily),
        content = content,
    )
}

private val DateFormat = DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", Locale.forLanguageTag("id-ID"))

/** "Kamis, 24 Sep 2026". */
fun LocalDate.formatId(): String = format(DateFormat)

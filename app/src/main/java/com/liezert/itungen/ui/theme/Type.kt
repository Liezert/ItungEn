package com.liezert.itungen.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.liezert.itungen.R

@OptIn(ExperimentalTextApi::class)
// inter_variable.ttf = subset Inter (Latin + tanda baca yang dipakai UI, sumbu bobot 400–700): 160 KB, bukan 876 KB.
// Huruf lain di catatan pengguna (mis. emoji) memakai font sistem. Cara membuat ulang: docs/PERFORMANCE.md.
val InterFamily = FontFamily(
    Font(R.font.inter_variable, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.inter_variable, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.inter_variable, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.inter_variable, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

/**
 * TextStyle langsung dari angka Figma: ukuran / tinggi baris / bobot / letter-spacing (px → sp 1:1).
 * Figma menaruh teks di tengah kotak barisnya, jadi tinggi baris dipusatkan tanpa trim.
 */
fun appText(
    size: Float,
    line: Float,
    weight: FontWeight = FontWeight.Normal,
    color: Color = Ink.White,
    tracking: Float = 0f,
) = TextStyle(
    fontFamily = InterFamily,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
    color = color,
    letterSpacing = tracking.sp,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
)

val Regular = FontWeight.Normal
val Medium = FontWeight.Medium
val SemiBold = FontWeight.SemiBold
val Bold = FontWeight.Bold

/** Komponen Material (dialog, date picker, switch) memakai Inter juga. */
fun Typography.withFamily(f: FontFamily) = copy(
    displayLarge = displayLarge.copy(fontFamily = f),
    displayMedium = displayMedium.copy(fontFamily = f),
    displaySmall = displaySmall.copy(fontFamily = f),
    headlineLarge = headlineLarge.copy(fontFamily = f),
    headlineMedium = headlineMedium.copy(fontFamily = f),
    headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f),
    titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f),
    bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f),
    bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f),
    labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f),
)

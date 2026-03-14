package org.dgeek.sumgrid.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.dgeek.sumgrid.R

/**
 * Outfit Variable font family.
 *
 * Single variable-weight TTF (~108KB) covers all weights from Thin to Black.
 * Falls back to system default (Roboto) if the font resource is unavailable.
 */
val OutfitFontFamily = FontFamily(
    Font(R.font.outfit_variable, FontWeight.Thin),
    Font(R.font.outfit_variable, FontWeight.ExtraLight),
    Font(R.font.outfit_variable, FontWeight.Light),
    Font(R.font.outfit_variable, FontWeight.Normal),
    Font(R.font.outfit_variable, FontWeight.Medium),
    Font(R.font.outfit_variable, FontWeight.SemiBold),
    Font(R.font.outfit_variable, FontWeight.Bold),
    Font(R.font.outfit_variable, FontWeight.ExtraBold),
    Font(R.font.outfit_variable, FontWeight.Black)
)

/**
 * SumGrid typography scale using the Outfit Variable font.
 *
 * Outfit is a geometric sans-serif that gives SumGrid a clean, modern identity
 * distinct from the default Roboto. The variable font file covers all weights.
 */
val SumGridTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    labelLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

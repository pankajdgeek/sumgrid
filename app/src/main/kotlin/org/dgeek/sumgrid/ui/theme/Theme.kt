package org.dgeek.sumgrid.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Indigo80,
    onPrimary = Indigo10,
    primaryContainer = IndigoContainer30,
    onPrimaryContainer = IndigoContainer90,
    secondary = Amber80,
    onSecondary = Neutral10,
    secondaryContainer = Amber40,
    onSecondaryContainer = Amber90,
    error = ErrorRed80,
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90
)

private val LightColorScheme = lightColorScheme(
    primary = Indigo40,
    onPrimary = Neutral99,
    primaryContainer = IndigoContainer90,
    onPrimaryContainer = Indigo10,
    secondary = Amber40,
    onSecondary = Neutral99,
    secondaryContainer = Amber90,
    onSecondaryContainer = Amber40,
    error = ErrorRed40,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10
)

/**
 * SumGrid Material3 theme.
 *
 * Dynamic color is intentionally disabled — SumGrid uses a branded deep-indigo/amber palette
 * that must remain consistent across all devices for the visual identity.
 */
@Composable
fun SumGridTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+ but disabled to preserve brand identity.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SumGridTypography,
        shapes = SumGridShapes,
        content = content
    )
}

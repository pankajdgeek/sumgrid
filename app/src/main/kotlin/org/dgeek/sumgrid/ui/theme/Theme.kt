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
    tertiary = WarmAmber80,
    onTertiary = WarmAmber10,
    tertiaryContainer = WarmAmberContainer30,
    onTertiaryContainer = WarmAmber90,
    error = ErrorRed80,
    onError = OnError20,
    errorContainer = OnError30,
    onErrorContainer = ErrorRed90,
    background = OledBlack,
    onBackground = Neutral90,
    surface = OledSurface,
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    outline = NeutralVariant60,
    outlineVariant = NeutralVariant30,
    inverseSurface = InverseSurface,
    inverseOnSurface = InverseOnSurface,
    inversePrimary = InversePrimary,
    scrim = Scrim
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
    tertiary = WarmAmber40,
    onTertiary = Neutral99,
    tertiaryContainer = WarmAmberContainer90,
    onTertiaryContainer = WarmAmber10,
    error = ErrorRed40,
    onError = Neutral99,
    errorContainer = ErrorRed90,
    onErrorContainer = OnError20,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    outline = NeutralVariant50,
    outlineVariant = NeutralVariant80,
    inverseSurface = Neutral20,
    inverseOnSurface = Neutral95,
    inversePrimary = Indigo80,
    scrim = Scrim
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

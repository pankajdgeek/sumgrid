package org.dgeek.sumgrid.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests verifying the expanded Material3 color palette.
 */
class ColorPaletteTest {

    private val colorSource: String by lazy {
        java.io.File("src/main/kotlin/org/dgeek/sumgrid/ui/theme/Color.kt").readText()
    }

    private val themeSource: String by lazy {
        java.io.File("src/main/kotlin/org/dgeek/sumgrid/ui/theme/Theme.kt").readText()
    }

    @Test
    fun palette_hasTertiaryColors() {
        assertTrue("Color.kt should define WarmAmber40", colorSource.contains("WarmAmber40"))
        assertTrue("Color.kt should define WarmAmber80", colorSource.contains("WarmAmber80"))
        assertTrue("Color.kt should define WarmAmber90", colorSource.contains("WarmAmber90"))
    }

    @Test
    fun palette_hasSurfaceVariantColors() {
        assertTrue("Color.kt should define NeutralVariant30", colorSource.contains("NeutralVariant30"))
        assertTrue("Color.kt should define NeutralVariant90", colorSource.contains("NeutralVariant90"))
    }

    @Test
    fun palette_hasOutlineColors() {
        assertTrue("Color.kt should define NeutralVariant50 (outline)", colorSource.contains("NeutralVariant50"))
        assertTrue("Color.kt should define NeutralVariant80 (outlineVariant)", colorSource.contains("NeutralVariant80"))
    }

    @Test
    fun palette_hasInverseColors() {
        assertTrue("Color.kt should define InverseSurface", colorSource.contains("InverseSurface"))
        assertTrue("Color.kt should define InverseOnSurface", colorSource.contains("InverseOnSurface"))
        assertTrue("Color.kt should define InversePrimary", colorSource.contains("InversePrimary"))
    }

    @Test
    fun palette_hasScrim() {
        assertTrue("Color.kt should define Scrim", colorSource.contains("val Scrim"))
    }

    @Test
    fun palette_hasOledDarkColors() {
        assertTrue("Color.kt should define OledBlack", colorSource.contains("OledBlack"))
        assertTrue("Color.kt should define OledSurface", colorSource.contains("OledSurface"))
    }

    @Test
    fun theme_lightSchemeHasTertiary() {
        assertTrue("LightColorScheme should set tertiary", themeSource.contains("tertiary = WarmAmber40"))
    }

    @Test
    fun theme_darkSchemeHasTertiary() {
        assertTrue("DarkColorScheme should set tertiary", themeSource.contains("tertiary = WarmAmber80"))
    }

    @Test
    fun theme_lightSchemeHasSurfaceVariant() {
        assertTrue("LightColorScheme should set surfaceVariant", themeSource.contains("surfaceVariant = NeutralVariant90"))
    }

    @Test
    fun theme_darkSchemeHasOledBackground() {
        assertTrue("DarkColorScheme should use OledBlack for background", themeSource.contains("background = OledBlack"))
    }

    @Test
    fun theme_darkSchemeHasOledSurface() {
        assertTrue("DarkColorScheme should use OledSurface for surface", themeSource.contains("surface = OledSurface"))
    }

    @Test
    fun palette_colorTokenCount_atLeast25() {
        // Count val declarations in Color.kt
        val valCount = Regex("^val\\s+\\w+\\s*=", RegexOption.MULTILINE)
            .findAll(colorSource)
            .count()
        assertTrue(
            "Color.kt should have at least 25 color tokens, found $valCount",
            valCount >= 25
        )
    }
}

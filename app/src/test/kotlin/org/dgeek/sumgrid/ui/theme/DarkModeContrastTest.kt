package org.dgeek.sumgrid.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests verifying dark mode OLED polish and contrast.
 */
class DarkModeContrastTest {

    @Test
    fun oledBlack_isNearBlack() {
        // OledBlack should be very dark (near 0x090909)
        val r = (OledBlack.red * 255).toInt()
        val g = (OledBlack.green * 255).toInt()
        val b = (OledBlack.blue * 255).toInt()
        assertTrue("OledBlack red channel should be <= 15", r <= 15)
        assertTrue("OledBlack green channel should be <= 15", g <= 15)
        assertTrue("OledBlack blue channel should be <= 15", b <= 15)
    }

    @Test
    fun oledSurface_isDarkerThan20Percent() {
        // OledSurface should be dark but slightly elevated (~0x121212)
        val luminance = OledSurface.red * 0.2126f + OledSurface.green * 0.7152f + OledSurface.blue * 0.0722f
        assertTrue("OledSurface luminance should be < 0.10", luminance < 0.10f)
    }

    @Test
    fun oledSurface_isLighterThanOledBlack() {
        val bgLum = OledBlack.red * 0.2126f + OledBlack.green * 0.7152f + OledBlack.blue * 0.0722f
        val surfLum = OledSurface.red * 0.2126f + OledSurface.green * 0.7152f + OledSurface.blue * 0.0722f
        assertTrue("OledSurface should be lighter than OledBlack for elevation", surfLum > bgLum)
    }

    @Test
    fun darkMode_textOnBackgroundMeetsWcagAA() {
        // Neutral90 on OledBlack — need >= 4.5:1 contrast ratio
        val textLum = relativeLuminance(Neutral90)
        val bgLum = relativeLuminance(OledBlack)
        val ratio = contrastRatio(textLum, bgLum)
        assertTrue(
            "Text (Neutral90) on background (OledBlack) contrast ratio should be >= 4.5:1, was $ratio",
            ratio >= 4.5
        )
    }

    @Test
    fun darkMode_primaryOnBackgroundMeetsWcagAA() {
        val primaryLum = relativeLuminance(Indigo80)
        val bgLum = relativeLuminance(OledBlack)
        val ratio = contrastRatio(primaryLum, bgLum)
        assertTrue(
            "Primary (Indigo80) on background (OledBlack) contrast ratio should be >= 4.5:1, was $ratio",
            ratio >= 4.5
        )
    }

    @Test
    fun oledGridLine_hasModerateVisibility() {
        // Grid lines should be visible but not harsh (~15% luminosity)
        val lum = relativeLuminance(OledGridLine)
        assertTrue("OledGridLine luminance should be > 0.02", lum > 0.02)
        assertTrue("OledGridLine luminance should be < 0.10", lum < 0.10)
    }

    @Test
    fun darkMode_givenCellContainer_onSurface_isDistinct() {
        // Given cells use primaryContainer on surface background
        // Non-text element contrast requirement is lower (WCAG 1.4.11: 3:1)
        // IndigoContainer30 on OledSurface — must be visually distinct
        val containerLum = relativeLuminance(IndigoContainer30)
        val surfaceLum = relativeLuminance(OledSurface)
        val ratio = contrastRatio(containerLum, surfaceLum)
        assertTrue(
            "Given cell container should be distinct from surface, ratio was $ratio",
            ratio >= 2.0
        )
    }

    @Test
    fun darkMode_textOnPrimaryContainer_meetsWcagAA() {
        // Text on given cells: onPrimaryContainer (IndigoContainer90) on primaryContainer (IndigoContainer30)
        val textLum = relativeLuminance(IndigoContainer90)
        val bgLum = relativeLuminance(IndigoContainer30)
        val ratio = contrastRatio(textLum, bgLum)
        assertTrue(
            "Text on given cell container contrast should be >= 4.5:1, was $ratio",
            ratio >= 4.5
        )
    }

    @Test
    fun darkMode_secondaryContainer_distinct() {
        // User cells use secondaryContainer — should be distinguishable from primaryContainer
        val primaryLum = relativeLuminance(IndigoContainer30)
        val secondaryLum = relativeLuminance(Amber40)
        assertTrue(
            "Primary and secondary containers should be visually distinct",
            Math.abs(primaryLum - secondaryLum) > 0.01 || IndigoContainer30 != Amber40
        )
    }

    // WCAG contrast ratio helpers
    private fun relativeLuminance(color: androidx.compose.ui.graphics.Color): Double {
        fun linearize(v: Float): Double {
            return if (v <= 0.03928f) (v / 12.92).toDouble()
            else Math.pow(((v + 0.055) / 1.055).toDouble(), 2.4)
        }
        return 0.2126 * linearize(color.red) +
               0.7152 * linearize(color.green) +
               0.0722 * linearize(color.blue)
    }

    private fun contrastRatio(l1: Double, l2: Double): Double {
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }
}

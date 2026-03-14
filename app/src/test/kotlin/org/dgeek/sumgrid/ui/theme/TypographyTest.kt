package org.dgeek.sumgrid.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests verifying the SumGrid typography configuration.
 *
 * Parses Type.kt source to confirm Outfit font is used and validates
 * the typography scale values at compile time.
 */
class TypographyTest {

    private val typeSource: String by lazy {
        val file = java.io.File("src/main/kotlin/org/dgeek/sumgrid/ui/theme/Type.kt")
        file.readText()
    }

    @Test
    fun typography_usesOutfitFontFamily() {
        assertTrue(
            "Type.kt should reference OutfitFontFamily",
            typeSource.contains("OutfitFontFamily")
        )
    }

    @Test
    fun typography_doesNotUseDefaultFontFamily() {
        // Ensure no TextStyle in SumGridTypography uses FontFamily.Default
        val typographySection = typeSource.substringAfter("val SumGridTypography")
        assertTrue(
            "SumGridTypography should not use FontFamily.Default",
            !typographySection.contains("FontFamily.Default")
        )
    }

    @Test
    fun typography_outfitFontFileExists() {
        val fontFile = java.io.File("src/main/res/font/outfit_variable.ttf")
        assertTrue(
            "Outfit Variable font file should exist at src/main/res/font/outfit_variable.ttf",
            fontFile.exists()
        )
    }

    @Test
    fun typography_outfitFontSizeUnder110KB() {
        val fontFile = java.io.File("src/main/res/font/outfit_variable.ttf")
        val sizeKB = fontFile.length() / 1024
        assertTrue(
            "Outfit font file should be under 110KB, was ${sizeKB}KB",
            sizeKB <= 110
        )
    }

    @Test
    fun typography_hasAllRequiredStyles() {
        assertNotNull("displayLarge should be defined", SumGridTypography.displayLarge)
        assertNotNull("headlineLarge should be defined", SumGridTypography.headlineLarge)
        assertNotNull("headlineMedium should be defined", SumGridTypography.headlineMedium)
        assertNotNull("titleLarge should be defined", SumGridTypography.titleLarge)
        assertNotNull("bodyLarge should be defined", SumGridTypography.bodyLarge)
        assertNotNull("bodyMedium should be defined", SumGridTypography.bodyMedium)
        assertNotNull("labelLarge should be defined", SumGridTypography.labelLarge)
        assertNotNull("labelSmall should be defined", SumGridTypography.labelSmall)
    }

    @Test
    fun typography_displayLargeIs57sp() {
        assertEquals(57f, SumGridTypography.displayLarge.fontSize.value, 0.01f)
    }
}

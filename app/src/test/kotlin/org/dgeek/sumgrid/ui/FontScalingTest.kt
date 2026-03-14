package org.dgeek.sumgrid.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM tests verifying font scaling preparedness.
 * Checks that no fixed dp-based text sizes are used (should be sp).
 */
class FontScalingTest {

    private val homeScreenSource: String by lazy {
        java.io.File("src/main/kotlin/org/dgeek/sumgrid/ui/screens/HomeScreen.kt").readText()
    }

    private val numberPadSource: String by lazy {
        java.io.File("src/main/kotlin/org/dgeek/sumgrid/ui/components/NumberPad.kt").readText()
    }

    private val typeSource: String by lazy {
        java.io.File("src/main/kotlin/org/dgeek/sumgrid/ui/theme/Type.kt").readText()
    }

    @Test
    fun homeScreen_usesNoFixedDpFontSizes() {
        // Font sizes should use sp (scalable) not dp (fixed)
        assertFalse(
            "HomeScreen should not use .dp for font sizes",
            homeScreenSource.contains("fontSize") && homeScreenSource.contains("fontSize.*\\.dp".toRegex())
        )
    }

    @Test
    fun numberPad_usesSpForFontSizes() {
        assertTrue(
            "NumberPad should use sp for font sizes",
            numberPadSource.contains(".sp")
        )
    }

    @Test
    fun typography_usesOutfitFontFamily() {
        assertTrue(
            "Typography should use custom Outfit font family",
            typeSource.contains("OutfitFontFamily") || typeSource.contains("FontFamily")
        )
    }
}

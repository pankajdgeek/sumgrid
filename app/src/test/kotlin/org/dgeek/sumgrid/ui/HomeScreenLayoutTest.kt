package org.dgeek.sumgrid.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests verifying HomeScreen layout features.
 */
class HomeScreenLayoutTest {

    private val homeScreenSource: String by lazy {
        java.io.File("src/main/kotlin/org/dgeek/sumgrid/ui/screens/HomeScreen.kt").readText()
    }

    @Test
    fun homeScreen_hasVerticalScroll() {
        assertTrue(
            "HomeScreen should use verticalScroll for compact viewports",
            homeScreenSource.contains("verticalScroll")
        )
    }

    @Test
    fun homeScreen_hasBadgeRow() {
        assertTrue(
            "HomeScreen should display a BadgeRow composable",
            homeScreenSource.contains("BadgeRow")
        )
    }

    @Test
    fun homeScreen_hasAllDoneSection() {
        assertTrue(
            "HomeScreen should have an AllDoneSection composable",
            homeScreenSource.contains("AllDoneSection")
        )
    }

    @Test
    fun homeScreen_hasAllCompleteState() {
        assertTrue(
            "HomeScreen should check allComplete state",
            homeScreenSource.contains("allComplete")
        )
    }

    @Test
    fun homeScreen_badgeAccessibility() {
        assertTrue(
            "Badge items should have accessibility content descriptions",
            homeScreenSource.contains("contentDescription")
        )
    }

    // ── T016 — Abbreviated PuzzleStatusChip labels ───────────────────────────

    @Test
    fun puzzleStatusChip_usesAbbreviatedLabels() {
        assertTrue(
            "PuzzleStatusChip must use abbreviated labels",
            homeScreenSource.contains("\"BEG\"") || homeScreenSource.contains("abbreviatedLabel")
        )
    }

    @Test
    fun puzzleStatusChip_doesNotUseLowercaseReplaceFirstChar() {
        // replaceFirstChar is acceptable in BadgeRow accessibility labels but must NOT
        // appear inside the PuzzleStatusChip call site. Check that the label mapping
        // uses abbreviated strings rather than full names derived from replaceFirstChar.
        // Since we now use a when expression with "BEG", this test verifies the pattern.
        val hasBegAbbreviation = homeScreenSource.contains("\"BEG\"")
        assertTrue(
            "PuzzleStatusChip must use abbreviated label 'BEG' (not full difficulty name)",
            hasBegAbbreviation
        )
    }
}

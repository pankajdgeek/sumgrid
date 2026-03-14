package org.dgeek.sumgrid.navigation

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Source-level tests verifying navigation routes are correctly declared.
 */
class RoutesTest {

    private val navSource: String by lazy {
        java.io.File("src/main/kotlin/org/dgeek/sumgrid/navigation/SumGridNavigation.kt").readText()
    }

    @Test
    fun navGraph_containsStatsRoute() {
        assertTrue(
            "SumGridNavigation should declare a STATS route constant",
            navSource.contains("STATS = \"stats\"")
        )
        assertTrue(
            "NavHost should have a composable(Routes.STATS) destination",
            navSource.contains("composable(Routes.STATS)")
        )
    }

    @Test
    fun navGraph_containsPracticeRoute() {
        assertTrue(
            "SumGridNavigation should declare a PRACTICE route",
            navSource.contains("PRACTICE = \"practice/{difficulty}\"")
        )
        assertTrue(
            "NavHost should have a composable for Routes.PRACTICE",
            navSource.contains("composable(") && navSource.contains("Routes.PRACTICE")
        )
    }

    @Test
    fun homeScreen_exposesOnOpenStatsCallback() {
        val homeSource = java.io.File("src/main/kotlin/org/dgeek/sumgrid/ui/screens/HomeScreen.kt").readText()
        assertTrue(
            "HomeScreen should accept an onOpenStats callback",
            homeSource.contains("onOpenStats:")
        )
    }

    @Test
    fun homeScreen_exposesOnStartPracticeCallback() {
        val homeSource = java.io.File("src/main/kotlin/org/dgeek/sumgrid/ui/screens/HomeScreen.kt").readText()
        assertTrue(
            "HomeScreen should accept an onStartPractice callback",
            homeSource.contains("onStartPractice:")
        )
    }
}

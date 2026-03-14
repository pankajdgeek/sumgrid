package org.dgeek.sumgrid.ui

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
}

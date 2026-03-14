package org.dgeek.sumgrid.ui

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests verifying gradient backgrounds are applied to screens.
 */
class GradientBackgroundTest {

    private val homeScreenSource: String by lazy {
        java.io.File("src/main/kotlin/org/dgeek/sumgrid/ui/screens/HomeScreen.kt").readText()
    }

    private val puzzleScreenSource: String by lazy {
        java.io.File("src/main/kotlin/org/dgeek/sumgrid/ui/screens/PuzzleScreen.kt").readText()
    }

    @Test
    fun homeScreen_hasVerticalGradient() {
        assertTrue(
            "HomeScreen should use Brush.verticalGradient",
            homeScreenSource.contains("verticalGradient")
        )
    }

    @Test
    fun homeScreen_gradientUsesThemeColors() {
        assertTrue(
            "HomeScreen gradient should use colorScheme.background",
            homeScreenSource.contains("colorScheme.background")
        )
        assertTrue(
            "HomeScreen gradient should use colorScheme.surfaceVariant",
            homeScreenSource.contains("colorScheme.surfaceVariant")
        )
    }

    @Test
    fun puzzleScreen_hasVerticalGradient() {
        assertTrue(
            "PuzzleScreen should use Brush.verticalGradient",
            puzzleScreenSource.contains("verticalGradient")
        )
    }

    @Test
    fun puzzleScreen_gradientUsesThemeColors() {
        assertTrue(
            "PuzzleScreen gradient should use colorScheme.background",
            puzzleScreenSource.contains("colorScheme.background")
        )
    }
}

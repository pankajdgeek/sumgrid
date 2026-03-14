package org.dgeek.sumgrid.ui

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests verifying streak counter animation integration.
 */
class StreakAnimationTest {

    private val homeScreenSource: String by lazy {
        java.io.File("src/main/kotlin/org/dgeek/sumgrid/ui/screens/HomeScreen.kt").readText()
    }

    @Test
    fun streakDisplay_hasScaleAnimation() {
        assertTrue(
            "StreakDisplay should use animateFloatAsState for scale",
            homeScreenSource.contains("animateFloatAsState")
        )
    }

    @Test
    fun streakDisplay_hasSpringSpec() {
        assertTrue(
            "StreakDisplay scale animation should use spring spec",
            homeScreenSource.contains("spring(")
        )
    }

    @Test
    fun streakDisplay_hasFlameAlphaPulse() {
        assertTrue(
            "StreakDisplay should have flame alpha pulse via infiniteRepeatable",
            homeScreenSource.contains("infiniteRepeatable") &&
                homeScreenSource.contains("flameAlpha")
        )
    }

    @Test
    fun streakDisplay_flamePulseDuration1200ms() {
        assertTrue(
            "Flame pulse should have 1200ms duration",
            homeScreenSource.contains("1200")
        )
    }

    @Test
    fun streakDisplay_usesModifierScale() {
        assertTrue(
            "StreakDisplay should apply Modifier.scale to streak number",
            homeScreenSource.contains("Modifier.scale(streakScale)")
        )
    }

    @Test
    fun streakDisplay_usesModifierAlpha() {
        assertTrue(
            "StreakDisplay should apply Modifier.alpha to flame icon",
            homeScreenSource.contains("Modifier.alpha(flameAlpha)")
        )
    }
}

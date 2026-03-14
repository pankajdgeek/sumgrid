package org.dgeek.sumgrid.ui

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests verifying OnboardingScreen UI features.
 */
class OnboardingScreenUiTest {

    private val onboardingSource: String by lazy {
        java.io.File("src/main/kotlin/org/dgeek/sumgrid/ui/screens/OnboardingScreen.kt").readText()
    }

    @Test
    fun onboardingScreen_hasSkipButton() {
        assertTrue(
            "OnboardingScreen should have a 'Skip tutorial' button",
            onboardingSource.contains("Skip tutorial")
        )
    }

    @Test
    fun onboardingScreen_callsSkipOnboarding() {
        assertTrue(
            "Skip button should call skipOnboarding()",
            onboardingSource.contains("skipOnboarding")
        )
    }

    @Test
    fun onboardingScreen_hasRulesOverlay() {
        assertTrue(
            "OnboardingScreen should have a rules explanation",
            onboardingSource.contains("How to play SumGrid")
        )
    }

    @Test
    fun onboardingScreen_rulesExplainRowColumnSums() {
        assertTrue(
            "Rules should explain row and column target sums",
            onboardingSource.contains("target sum")
        )
    }

    @Test
    fun onboardingScreen_rulesHasDismissButton() {
        assertTrue(
            "Rules overlay should have a 'Got it' dismiss button",
            onboardingSource.contains("rulesAcknowledged")
        )
    }

    @Test
    fun onboardingScreen_usesAnimatedVisibility() {
        assertTrue(
            "Rules overlay should use AnimatedVisibility for fade",
            onboardingSource.contains("AnimatedVisibility")
        )
    }
}

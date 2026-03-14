package org.dgeek.sumgrid.navigation

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests verifying animated screen transitions are configured.
 *
 * Parses SumGridNavigation.kt source to confirm that NavHost uses
 * transition parameters (enterTransition, exitTransition, etc.).
 */
class AnimatedNavTest {

    private val navSource: String by lazy {
        val file = java.io.File("src/main/kotlin/org/dgeek/sumgrid/navigation/SumGridNavigation.kt")
        file.readText()
    }

    @Test
    fun navHost_hasEnterTransition() {
        assertTrue(
            "NavHost should have enterTransition configured",
            navSource.contains("enterTransition")
        )
    }

    @Test
    fun navHost_hasExitTransition() {
        assertTrue(
            "NavHost should have exitTransition configured",
            navSource.contains("exitTransition")
        )
    }

    @Test
    fun navHost_hasPopEnterTransition() {
        assertTrue(
            "NavHost should have popEnterTransition configured",
            navSource.contains("popEnterTransition")
        )
    }

    @Test
    fun navHost_hasPopExitTransition() {
        assertTrue(
            "NavHost should have popExitTransition configured",
            navSource.contains("popExitTransition")
        )
    }

    @Test
    fun navHost_usesFadeTransition() {
        assertTrue(
            "Transitions should use fadeIn/fadeOut",
            navSource.contains("fadeIn") && navSource.contains("fadeOut")
        )
    }

    @Test
    fun navHost_usesSlideTransition() {
        assertTrue(
            "Transitions should use slideIntoContainer/slideOutOfContainer",
            navSource.contains("slideIntoContainer") && navSource.contains("slideOutOfContainer")
        )
    }

    @Test
    fun navHost_transitionDurationIs300ms() {
        assertTrue(
            "Transition duration should be 300ms",
            navSource.contains("tween(300)")
        )
    }
}

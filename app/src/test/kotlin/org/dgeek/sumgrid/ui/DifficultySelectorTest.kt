package org.dgeek.sumgrid.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM source-scan tests for DifficultySelector.kt.
 *
 * Verifies that the selector uses a horizontally scrollable LazyRow with
 * fixed-width cards rather than a Row with weight(1f) per card.
 *
 * T001 — RED tests (must fail against the old Row+weight implementation,
 *         pass once T002 is applied).
 */
class DifficultySelectorTest {

    private val source: String by lazy {
        java.io.File("src/main/kotlin/org/dgeek/sumgrid/ui/components/DifficultySelector.kt")
            .readText()
    }

    @Test
    fun difficultySelector_usesLazyRow() {
        assertTrue(
            "DifficultySelector must use LazyRow for horizontal scrolling",
            source.contains("LazyRow")
        )
    }

    @Test
    fun difficultySelector_doesNotUseWeightPerCard() {
        // Current source has weight(1f) on each card — this test will fail until T002 is done
        assertFalse(
            "DifficultySelector must not weight cards in a fixed Row",
            source.contains("Modifier.weight(1f)") && !source.contains("LazyRow")
        )
    }

    @Test
    fun difficultySelector_cardsHaveFixedWidth() {
        assertTrue(
            "Cards must have a fixed width (84.dp)",
            source.contains("84.dp") || source.contains("width = 84")
        )
    }
}

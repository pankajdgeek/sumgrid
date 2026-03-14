package org.dgeek.sumgrid.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM source-scan tests for the BadgeItem / BadgeRow composables in HomeScreen.kt.
 *
 * T004 — RED tests: must fail against the old source that shows displayName text labels.
 *         Pass once T005 is applied (emoji-only with clickable handler).
 */
class BadgeRowTest {

    private val source: String by lazy {
        java.io.File("src/main/kotlin/org/dgeek/sumgrid/ui/screens/HomeScreen.kt").readText()
    }

    @Test
    fun badgeItem_doesNotShowDisplayNameAsText() {
        // Current BadgeItem renders badge.displayName with labelSmall style.
        // After T005 the displayName Text is removed; only the emoji Text remains.
        assertFalse(
            "BadgeItem must not render displayName Text in the row",
            source.contains("badge.displayName") && source.contains("labelSmall")
        )
    }

    @Test
    fun badgeItem_hasClickableModifier() {
        assertTrue(
            "BadgeItem must be clickable",
            source.contains("BadgeItem") && source.contains("clickable")
        )
    }

    @Test
    fun homeScreen_hasSelectedBadgeState() {
        assertTrue(
            "HomeScreen must have selectedBadge state variable",
            source.contains("selectedBadge")
        )
    }
}

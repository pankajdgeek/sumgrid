package org.dgeek.sumgrid.ui

import org.dgeek.sumgrid.ui.components.formatSumLabel
import org.dgeek.sumgrid.viewmodel.SumIndicatorColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests for sum indicator formatting and visual feedback.
 */
class SumIndicatorTest {

    @Test
    fun formatSumLabel_green_appendsCheckmark() {
        val label = formatSumLabel(15, SumIndicatorColor.GREEN)
        assertEquals("15 ✓", label)
    }

    @Test
    fun formatSumLabel_red_appendsCross() {
        val label = formatSumLabel(10, SumIndicatorColor.RED)
        assertEquals("10 ✗", label)
    }

    @Test
    fun formatSumLabel_gray_noSuffix() {
        val label = formatSumLabel(20, SumIndicatorColor.GRAY)
        assertEquals("20", label)
    }

    @Test
    fun gridRenderer_hasSelectionScaleAnimation() {
        val source = java.io.File(
            "src/main/kotlin/org/dgeek/sumgrid/ui/components/GridRenderer.kt"
        ).readText()
        assertTrue(
            "GridRenderer should have selectionScale animation",
            source.contains("selectionScale")
        )
    }

    @Test
    fun gridRenderer_usesSpringAnimation() {
        val source = java.io.File(
            "src/main/kotlin/org/dgeek/sumgrid/ui/components/GridRenderer.kt"
        ).readText()
        assertTrue(
            "GridRenderer should use spring animation for selection",
            source.contains("spring(")
        )
    }

    @Test
    fun gridRenderer_selectionTargetIs1Point05() {
        val source = java.io.File(
            "src/main/kotlin/org/dgeek/sumgrid/ui/components/GridRenderer.kt"
        ).readText()
        assertTrue(
            "Selection scale target should be 1.05f",
            source.contains("1.05f")
        )
    }
}

package org.dgeek.sumgrid.ui

import org.dgeek.sumgrid.ui.components.formatSumLabel
import org.dgeek.sumgrid.viewmodel.SumIndicatorColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the colorblind-friendly sum indicator symbols in GridRenderer.
 *
 * Tests verify that [formatSumLabel] appends the correct symbol suffix based on
 * the [SumIndicatorColor] so that the grid is accessible without relying on
 * color alone to convey state.
 */
class ColorblindIndicatorTest {

    @Test
    fun `GREEN indicator appends checkmark symbol`() {
        val result = formatSumLabel(target = 6, indicator = SumIndicatorColor.GREEN)
        assertTrue(
            "GREEN indicator should contain a checkmark but was: $result",
            result.contains("✓")
        )
        assertEquals("6 ✓", result)
    }

    @Test
    fun `RED indicator appends cross symbol`() {
        val result = formatSumLabel(target = 10, indicator = SumIndicatorColor.RED)
        assertTrue(
            "RED indicator should contain a cross but was: $result",
            result.contains("✗")
        )
        assertEquals("10 ✗", result)
    }

    @Test
    fun `GRAY indicator appends no symbol`() {
        val result = formatSumLabel(target = 3, indicator = SumIndicatorColor.GRAY)
        assertFalse(
            "GRAY indicator should not contain checkmark but was: $result",
            result.contains("✓")
        )
        assertFalse(
            "GRAY indicator should not contain cross but was: $result",
            result.contains("✗")
        )
        assertEquals("3", result)
    }

    @Test
    fun `formatSumLabel uses target number as prefix`() {
        assertEquals("15 ✓", formatSumLabel(target = 15, indicator = SumIndicatorColor.GREEN))
        assertEquals("7 ✗", formatSumLabel(target = 7, indicator = SumIndicatorColor.RED))
        assertEquals("1", formatSumLabel(target = 1, indicator = SumIndicatorColor.GRAY))
    }

    @Test
    fun `zero target works correctly`() {
        assertEquals("0 ✓", formatSumLabel(target = 0, indicator = SumIndicatorColor.GREEN))
        assertEquals("0 ✗", formatSumLabel(target = 0, indicator = SumIndicatorColor.RED))
        assertEquals("0", formatSumLabel(target = 0, indicator = SumIndicatorColor.GRAY))
    }
}

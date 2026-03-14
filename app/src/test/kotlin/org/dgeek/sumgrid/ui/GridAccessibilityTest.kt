package org.dgeek.sumgrid.ui

import org.dgeek.sumgrid.ui.components.cellDescription
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * JVM unit tests for [cellDescription] helper function.
 *
 * These are pure JVM tests — no Compose test rule or Android device required.
 * cellDescription is a pure string-formatting function marked `internal`.
 */
class GridAccessibilityTest {

    @Test
    fun `cellDescription for given cell with value uses 1-based row and col`() {
        val result = cellDescription(row = 0, col = 0, value = 3, isGiven = true)
        assertEquals("Row 1, Column 1, 3, given", result)
    }

    @Test
    fun `cellDescription for empty editable cell`() {
        val result = cellDescription(row = 1, col = 2, value = 0, isGiven = false)
        assertEquals("Row 2, Column 3, empty, editable", result)
    }

    @Test
    fun `cellDescription for non-zero editable cell`() {
        val result = cellDescription(row = 2, col = 1, value = 5, isGiven = false)
        assertEquals("Row 3, Column 2, 5, editable", result)
    }
}

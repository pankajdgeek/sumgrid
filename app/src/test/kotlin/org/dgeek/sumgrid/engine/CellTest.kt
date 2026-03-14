package org.dgeek.sumgrid.engine

import org.dgeek.sumgrid.engine.models.Cell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CellTest {

    @Test
    fun cell_withZeroValue_isEmpty() {
        val cell = Cell(value = 0, isGiven = false)
        assertTrue(cell.isEmpty)
    }

    @Test
    fun cell_withNonZeroValue_isNotEmpty() {
        val cell = Cell(value = 3, isGiven = true)
        assertFalse(cell.isEmpty)
    }

    @Test
    fun cell_isGiven_returnsCorrectFlag() {
        val given = Cell(value = 5, isGiven = true)
        val notGiven = Cell(value = 5, isGiven = false)
        assertTrue(given.isGiven)
        assertFalse(notGiven.isGiven)
    }

    @Test
    fun cell_equality_sameValues() {
        val cell1 = Cell(value = 4, isGiven = true)
        val cell2 = Cell(value = 4, isGiven = true)
        assertEquals(cell1, cell2)
    }

    @Test
    fun cell_equality_differentValue() {
        val cell1 = Cell(value = 2, isGiven = true)
        val cell2 = Cell(value = 3, isGiven = true)
        assertFalse(cell1 == cell2)
    }

    @Test
    fun cell_equality_differentIsGiven() {
        val cell1 = Cell(value = 2, isGiven = true)
        val cell2 = Cell(value = 2, isGiven = false)
        assertFalse(cell1 == cell2)
    }

    @Test
    fun cell_copy_changesValue() {
        val original = Cell(value = 1, isGiven = true)
        val copied = original.copy(value = 9)
        assertEquals(9, copied.value)
        assertTrue(copied.isGiven)
    }

    @Test
    fun cell_copy_changesIsGiven() {
        val original = Cell(value = 3, isGiven = true)
        val copied = original.copy(isGiven = false)
        assertEquals(3, copied.value)
        assertFalse(copied.isGiven)
    }

    @Test
    fun cell_hashCode_equalForEqualCells() {
        val cell1 = Cell(value = 7, isGiven = false)
        val cell2 = Cell(value = 7, isGiven = false)
        assertEquals(cell1.hashCode(), cell2.hashCode())
    }
}

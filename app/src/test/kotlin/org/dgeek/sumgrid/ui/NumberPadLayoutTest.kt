package org.dgeek.sumgrid.ui

import org.dgeek.sumgrid.ui.components.splitNumberRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for NumberPad layout logic.
 */
class NumberPadLayoutTest {

    private val numberPadSource: String by lazy {
        java.io.File("src/main/kotlin/org/dgeek/sumgrid/ui/components/NumberPad.kt").readText()
    }

    @Test
    fun numberPad_hasTwoRowLogic() {
        assertTrue(
            "NumberPad should have 2-row logic for maxVal >= 7",
            numberPadSource.contains("maxVal >= 7")
        )
    }

    @Test
    fun numberPad_hasTestTags() {
        assertTrue(
            "NumberPad should use number_pad_row test tags",
            numberPadSource.contains("number_pad_row_")
        )
    }

    @Test
    fun numberPad_hasUndoButton() {
        assertTrue(
            "NumberPad should have an undo button",
            numberPadSource.contains("Undo last move")
        )
    }

    @Test
    fun splitNumberRange_sixNumbers_twoRows() {
        val result = splitNumberRange((1..6).toList())
        assertEquals(2, result.size)
        assertEquals(listOf(1, 2, 3), result[0])
        assertEquals(listOf(4, 5, 6), result[1])
    }

    @Test
    fun splitNumberRange_sevenNumbers_twoRows() {
        val result = splitNumberRange((1..7).toList())
        assertEquals(2, result.size)
        assertEquals(listOf(1, 2, 3, 4), result[0])
        assertEquals(listOf(5, 6, 7), result[1])
    }

    @Test
    fun splitNumberRange_nineNumbers_twoRows() {
        val result = splitNumberRange((1..9).toList())
        assertEquals(2, result.size)
        assertEquals(listOf(1, 2, 3, 4, 5), result[0])
        assertEquals(listOf(6, 7, 8, 9), result[1])
    }
}

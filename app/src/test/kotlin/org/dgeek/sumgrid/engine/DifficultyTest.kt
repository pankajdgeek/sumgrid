package org.dgeek.sumgrid.engine

import org.dgeek.sumgrid.engine.models.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Test

class DifficultyTest {

    @Test
    fun beginner_hasCorrectSize() {
        assertEquals(3, Difficulty.BEGINNER.size)
    }

    @Test
    fun beginner_hasCorrectMaxVal() {
        assertEquals(5, Difficulty.BEGINNER.maxVal)
    }

    @Test
    fun beginner_hasCorrectEmptyCells() {
        assertEquals(4, Difficulty.BEGINNER.emptyCells)
    }

    @Test
    fun beginner_hasCorrectSeedOffset() {
        assertEquals(0, Difficulty.BEGINNER.seedOffset)
    }

    @Test
    fun easy_hasCorrectSize() {
        assertEquals(4, Difficulty.EASY.size)
    }

    @Test
    fun easy_hasCorrectMaxVal() {
        assertEquals(7, Difficulty.EASY.maxVal)
    }

    @Test
    fun easy_hasCorrectEmptyCells() {
        assertEquals(8, Difficulty.EASY.emptyCells)
    }

    @Test
    fun easy_hasCorrectSeedOffset() {
        assertEquals(1, Difficulty.EASY.seedOffset)
    }

    @Test
    fun medium_hasCorrectSize() {
        assertEquals(5, Difficulty.MEDIUM.size)
    }

    @Test
    fun medium_hasCorrectMaxVal() {
        assertEquals(9, Difficulty.MEDIUM.maxVal)
    }

    @Test
    fun medium_hasCorrectEmptyCells() {
        assertEquals(15, Difficulty.MEDIUM.emptyCells)
    }

    @Test
    fun medium_hasCorrectSeedOffset() {
        assertEquals(2, Difficulty.MEDIUM.seedOffset)
    }

    @Test
    fun allDifficultiesPresent() {
        val values = Difficulty.values()
        assertEquals(3, values.size)
        assertEquals(Difficulty.BEGINNER, values[0])
        assertEquals(Difficulty.EASY, values[1])
        assertEquals(Difficulty.MEDIUM, values[2])
    }
}

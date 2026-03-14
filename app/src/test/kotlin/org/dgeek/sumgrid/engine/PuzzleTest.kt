package org.dgeek.sumgrid.engine

import org.dgeek.sumgrid.engine.models.Cell
import org.dgeek.sumgrid.engine.models.Difficulty
import org.dgeek.sumgrid.engine.models.Puzzle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzleTest {

    private fun makePuzzle(difficulty: Difficulty = Difficulty.BEGINNER, seed: Long = 42L): Puzzle {
        val size = difficulty.size
        val cells = Array(size) { row ->
            Array(size) { col ->
                Cell(value = (row * size + col + 1) % (difficulty.maxVal + 1), isGiven = true)
            }
        }
        val rowTargets = IntArray(size) { it + 1 }
        val colTargets = IntArray(size) { it + 10 }
        return Puzzle(
            size = size,
            cells = cells,
            rowTargets = rowTargets,
            colTargets = colTargets,
            difficulty = difficulty,
            seed = seed
        )
    }

    @Test
    fun puzzle_construction_sizeMatchesDifficulty() {
        val puzzle = makePuzzle(Difficulty.BEGINNER)
        assertEquals(Difficulty.BEGINNER.size, puzzle.size)
    }

    @Test
    fun puzzle_construction_easySize() {
        val puzzle = makePuzzle(Difficulty.EASY)
        assertEquals(4, puzzle.size)
    }

    @Test
    fun puzzle_construction_mediumSize() {
        val puzzle = makePuzzle(Difficulty.MEDIUM)
        assertEquals(5, puzzle.size)
    }

    @Test
    fun puzzle_cellArrayDimensions_matchSize() {
        val puzzle = makePuzzle(Difficulty.BEGINNER)
        assertEquals(puzzle.size, puzzle.cells.size)
        for (row in puzzle.cells) {
            assertEquals(puzzle.size, row.size)
        }
    }

    @Test
    fun puzzle_cellAccess_returnsCorrectCell() {
        val puzzle = makePuzzle(Difficulty.BEGINNER)
        // First cell should be (0*3+0+1) % 6 = 1
        assertEquals(1, puzzle.cells[0][0].value)
        assertTrue(puzzle.cells[0][0].isGiven)
    }

    @Test
    fun puzzle_rowTargets_haveCorrectLength() {
        val puzzle = makePuzzle(Difficulty.BEGINNER)
        assertEquals(puzzle.size, puzzle.rowTargets.size)
    }

    @Test
    fun puzzle_colTargets_haveCorrectLength() {
        val puzzle = makePuzzle(Difficulty.BEGINNER)
        assertEquals(puzzle.size, puzzle.colTargets.size)
    }

    @Test
    fun puzzle_rowTargets_valuesCorrect() {
        val puzzle = makePuzzle(Difficulty.BEGINNER)
        assertEquals(1, puzzle.rowTargets[0])
        assertEquals(2, puzzle.rowTargets[1])
        assertEquals(3, puzzle.rowTargets[2])
    }

    @Test
    fun puzzle_colTargets_valuesCorrect() {
        val puzzle = makePuzzle(Difficulty.BEGINNER)
        assertEquals(10, puzzle.colTargets[0])
        assertEquals(11, puzzle.colTargets[1])
        assertEquals(12, puzzle.colTargets[2])
    }

    @Test
    fun puzzle_difficulty_isStoredCorrectly() {
        val puzzle = makePuzzle(Difficulty.MEDIUM)
        assertEquals(Difficulty.MEDIUM, puzzle.difficulty)
    }

    @Test
    fun puzzle_seed_isStoredCorrectly() {
        val puzzle = makePuzzle(seed = 12345L)
        assertEquals(12345L, puzzle.seed)
    }

    @Test
    fun puzzle_equals_sameContent() {
        val p1 = makePuzzle(Difficulty.BEGINNER, seed = 7L)
        val p2 = makePuzzle(Difficulty.BEGINNER, seed = 7L)
        assertEquals(p1, p2)
    }

    @Test
    fun puzzle_equals_differentSeed() {
        val p1 = makePuzzle(Difficulty.BEGINNER, seed = 1L)
        val p2 = makePuzzle(Difficulty.BEGINNER, seed = 2L)
        assertFalse(p1 == p2)
    }

    @Test
    fun puzzle_hashCode_equalForEqualPuzzles() {
        val p1 = makePuzzle(Difficulty.BEGINNER, seed = 99L)
        val p2 = makePuzzle(Difficulty.BEGINNER, seed = 99L)
        assertEquals(p1.hashCode(), p2.hashCode())
    }
}

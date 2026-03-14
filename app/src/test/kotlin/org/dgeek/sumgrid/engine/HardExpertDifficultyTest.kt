package org.dgeek.sumgrid.engine

import org.dgeek.sumgrid.engine.models.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HardExpertDifficultyTest {

    @Test
    fun difficulty_hard_hasSizeOf6() {
        assertEquals(6, Difficulty.HARD.size)
        assertEquals(9, Difficulty.HARD.maxVal)
    }

    @Test
    fun difficulty_expert_hasSizeOf7() {
        assertEquals(7, Difficulty.EXPERT.size)
        assertEquals(9, Difficulty.EXPERT.maxVal)
    }

    @Test
    fun puzzleGenerator_generates6x6Puzzle_forHard() {
        val puzzle = PuzzleGenerator().generate(seed = 42L, Difficulty.HARD)
        assertEquals(6, puzzle.size)
        assertEquals(6, puzzle.cells.size)
        assertEquals(6, puzzle.cells[0].size)
        assertEquals(Difficulty.HARD, puzzle.difficulty)
    }

    @Test
    fun puzzleGenerator_generates7x7Puzzle_forExpert() {
        val puzzle = PuzzleGenerator().generate(seed = 42L, Difficulty.EXPERT)
        assertEquals(7, puzzle.size)
        assertEquals(7, puzzle.cells.size)
        assertEquals(7, puzzle.cells[0].size)
        assertEquals(Difficulty.EXPERT, puzzle.difficulty)
    }

    @Test
    fun puzzleGenerator_hard_puzzleHasEmptyCells() {
        val puzzle = PuzzleGenerator().generate(seed = 42L, Difficulty.HARD)
        val emptyCells = puzzle.cells.sumOf { row -> row.count { !it.isGiven } }
        assertEquals(Difficulty.HARD.emptyCells, emptyCells)
    }

    @Test
    fun puzzleGenerator_expert_puzzleHasEmptyCells() {
        val puzzle = PuzzleGenerator().generate(seed = 42L, Difficulty.EXPERT)
        val emptyCells = puzzle.cells.sumOf { row -> row.count { !it.isGiven } }
        assertEquals(Difficulty.EXPERT.emptyCells, emptyCells)
    }

    @Test
    fun puzzleGenerator_hard_rowTargetsMatchGrid() {
        val puzzle = PuzzleGenerator().generate(seed = 42L, Difficulty.HARD)
        assertEquals(6, puzzle.rowTargets.size)
        assertEquals(6, puzzle.colTargets.size)
        // Each target should be > 0
        assertTrue(puzzle.rowTargets.all { it > 0 })
        assertTrue(puzzle.colTargets.all { it > 0 })
    }
}

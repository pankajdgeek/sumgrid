package org.dgeek.sumgrid.engine

import org.dgeek.sumgrid.engine.models.Cell
import org.dgeek.sumgrid.engine.models.Difficulty
import org.dgeek.sumgrid.engine.models.Puzzle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DifficultyCalibrationTest {

    private val generator = PuzzleGenerator()
    private val calibrator = DifficultyCalibrator()

    // -----------------------------------------------------------------------
    // 1. countSolveSteps returns >= 0 for all difficulties
    // -----------------------------------------------------------------------

    @Test
    fun `countSolveSteps returns non-negative for BEGINNER puzzle`() {
        val puzzle = generator.generate(1000L, Difficulty.BEGINNER)
        val steps = calibrator.countSolveSteps(puzzle)
        assertTrue("Solve steps must be >= 0, got $steps", steps >= 0)
    }

    @Test
    fun `countSolveSteps returns non-negative for EASY puzzle`() {
        val puzzle = generator.generate(1000L, Difficulty.EASY)
        val steps = calibrator.countSolveSteps(puzzle)
        assertTrue("Solve steps must be >= 0, got $steps", steps >= 0)
    }

    @Test
    fun `countSolveSteps returns non-negative for MEDIUM puzzle`() {
        val puzzle = generator.generate(1000L, Difficulty.MEDIUM)
        val steps = calibrator.countSolveSteps(puzzle)
        assertTrue("Solve steps must be >= 0, got $steps", steps >= 0)
    }

    // -----------------------------------------------------------------------
    // 2. Fully-filled puzzle has 0 solve steps
    // -----------------------------------------------------------------------

    @Test
    fun `fully-filled puzzle has 0 solve steps`() {
        // Build a 3x3 puzzle with no empty cells
        val size = 3
        val values = Array(size) { r -> Array(size) { c -> Cell(value = (r * size + c + 1).coerceAtMost(5), isGiven = true) } }
        val rowTargets = IntArray(size) { r -> (0 until size).sumOf { c -> values[r][c].value } }
        val colTargets = IntArray(size) { c -> (0 until size).sumOf { r -> values[r][c].value } }

        val puzzle = Puzzle(
            size = size,
            cells = values,
            rowTargets = rowTargets,
            colTargets = colTargets,
            difficulty = Difficulty.BEGINNER,
            seed = 0L
        )
        val steps = calibrator.countSolveSteps(puzzle)
        assertEquals("Fully-filled puzzle must have 0 solve steps", 0, steps)
    }

    // -----------------------------------------------------------------------
    // 3. calibrate(puzzle) matches the puzzle's own difficulty
    //    (round-trip: generate at difficulty X, calibrate should return X)
    // -----------------------------------------------------------------------

    @Test
    fun `calibrate BEGINNER puzzle returns BEGINNER`() {
        val puzzle = generator.generate(1000L, Difficulty.BEGINNER)
        val result = calibrator.calibrate(puzzle)
        assertEquals("Calibrated difficulty must match BEGINNER", Difficulty.BEGINNER, result)
    }

    @Test
    fun `calibrate EASY puzzle returns EASY`() {
        val puzzle = generator.generate(1000L, Difficulty.EASY)
        val result = calibrator.calibrate(puzzle)
        assertEquals("Calibrated difficulty must match EASY", Difficulty.EASY, result)
    }

    @Test
    fun `calibrate MEDIUM puzzle returns MEDIUM`() {
        val puzzle = generator.generate(1000L, Difficulty.MEDIUM)
        val result = calibrator.calibrate(puzzle)
        assertEquals("Calibrated difficulty must match MEDIUM", Difficulty.MEDIUM, result)
    }

    // -----------------------------------------------------------------------
    // 4. countSolveSteps is bounded by the number of empty cells
    // -----------------------------------------------------------------------

    @Test
    fun `countSolveSteps does not exceed number of empty cells`() {
        for (difficulty in Difficulty.entries) {
            val puzzle = generator.generate(42L, difficulty)
            val emptyCells = puzzle.cells.sumOf { row -> row.count { it.isEmpty } }
            val steps = calibrator.countSolveSteps(puzzle)
            assertTrue(
                "$difficulty: steps=$steps must be <= emptyCells=$emptyCells",
                steps <= emptyCells
            )
        }
    }
}

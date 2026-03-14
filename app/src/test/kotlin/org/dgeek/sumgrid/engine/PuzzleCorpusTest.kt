package org.dgeek.sumgrid.engine

import org.dgeek.sumgrid.engine.models.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * S01 quality gate — 10,000-puzzle corpus test.
 *
 * Generates 10,000 puzzles (seeds 1..10_000, cycling through
 * BEGINNER / EASY / MEDIUM) and asserts four invariants for each:
 *
 *  1. Unique solution  — UniqueSolutionValidator returns UNIQUE
 *  2. Determinism      — regenerating with the same seed produces identical puzzle
 *  3. Empty cell count — matches difficulty.emptyCells
 *  4. Value range      — all given cell values are in [1..difficulty.maxVal]
 *
 * Must complete in under 60 seconds on CI hardware.
 */
class PuzzleCorpusTest {

    private val generator = PuzzleGenerator()
    private val validator = UniqueSolutionValidator()

    // Limit corpus to fast-generating difficulties; HARD/EXPERT tested separately
    private val difficulties = arrayOf(Difficulty.BEGINNER, Difficulty.EASY, Difficulty.MEDIUM)

    @Test
    fun `10000 puzzles satisfy all S01 invariants`() {
        val startMs = System.currentTimeMillis()

        for (seed in 1L..10_000L) {
            val difficulty = difficulties[((seed - 1) % difficulties.size).toInt()]

            // ----------------------------------------------------------------
            // Generate the puzzle
            // ----------------------------------------------------------------
            val puzzle = generator.generate(seed, difficulty)

            // ----------------------------------------------------------------
            // 1. Unique solution
            // ----------------------------------------------------------------
            val grid = Array(puzzle.size) { r -> IntArray(puzzle.size) { c -> puzzle.cells[r][c].value } }
            val solutionCount = validator.validate(grid, puzzle.rowTargets, puzzle.colTargets, difficulty.maxVal)
            assertEquals(
                "seed=$seed difficulty=$difficulty: expected UNIQUE solution",
                SolutionCount.Unique,
                solutionCount
            )

            // ----------------------------------------------------------------
            // 2. Determinism — regenerate and compare
            // ----------------------------------------------------------------
            val puzzle2 = generator.generate(seed, difficulty)
            assertEquals(
                "seed=$seed difficulty=$difficulty: regenerated puzzle must be identical",
                puzzle,
                puzzle2
            )

            // ----------------------------------------------------------------
            // 3. Empty cell count matches spec
            // ----------------------------------------------------------------
            var emptyCount = 0
            for (r in 0 until puzzle.size)
                for (c in 0 until puzzle.size)
                    if (puzzle.cells[r][c].isEmpty) emptyCount++

            assertEquals(
                "seed=$seed difficulty=$difficulty: empty cell count must be ${difficulty.emptyCells}",
                difficulty.emptyCells,
                emptyCount
            )

            // ----------------------------------------------------------------
            // 4. All cell values in [1..difficulty.maxVal]
            // ----------------------------------------------------------------
            for (r in 0 until puzzle.size) {
                for (c in 0 until puzzle.size) {
                    val cell = puzzle.cells[r][c]
                    if (cell.isGiven) {
                        assertTrue(
                            "seed=$seed difficulty=$difficulty cell($r,$c)=${cell.value} out of [1..${difficulty.maxVal}]",
                            cell.value in 1..difficulty.maxVal
                        )
                    }
                }
            }
        }

        val elapsedMs = System.currentTimeMillis() - startMs
        val elapsedSec = elapsedMs / 1_000.0
        println("PuzzleCorpusTest: 10,000 puzzles passed all invariants in %.2f s".format(elapsedSec))
        assertTrue(
            "10,000-puzzle corpus took ${elapsedSec}s — must complete in < 60s",
            elapsedSec < 60.0
        )
    }
}

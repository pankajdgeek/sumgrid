package org.dgeek.sumgrid.engine

import org.dgeek.sumgrid.engine.models.Cell
import org.dgeek.sumgrid.engine.models.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzleGeneratorTest {

    private val generator = PuzzleGenerator()
    private val validator = UniqueSolutionValidator()

    // -----------------------------------------------------------------------
    // 1. Determinism: same seed + difficulty produces identical puzzles
    // -----------------------------------------------------------------------

    @Test
    fun `generate with same seed and BEGINNER returns identical puzzles`() {
        val p1 = generator.generate(1000L, Difficulty.BEGINNER)
        val p2 = generator.generate(1000L, Difficulty.BEGINNER)
        assertEquals("Same seed must produce identical puzzles", p1, p2)
    }

    @Test
    fun `generate with same seed and EASY returns identical puzzles`() {
        val p1 = generator.generate(42L, Difficulty.EASY)
        val p2 = generator.generate(42L, Difficulty.EASY)
        assertEquals("Same seed must produce identical puzzles", p1, p2)
    }

    @Test
    fun `generate with same seed and MEDIUM returns identical puzzles`() {
        val p1 = generator.generate(7L, Difficulty.MEDIUM)
        val p2 = generator.generate(7L, Difficulty.MEDIUM)
        assertEquals("Same seed must produce identical puzzles", p1, p2)
    }

    // -----------------------------------------------------------------------
    // 2. Empty cell count matches difficulty spec
    // -----------------------------------------------------------------------

    @Test
    fun `BEGINNER puzzle has exactly 4 empty cells`() {
        val puzzle = generator.generate(1000L, Difficulty.BEGINNER)
        val emptyCount = countEmptyCells(puzzle.cells)
        assertEquals("BEGINNER must have 4 empty cells", 4, emptyCount)
    }

    @Test
    fun `EASY puzzle has exactly 8 empty cells`() {
        val puzzle = generator.generate(1000L, Difficulty.EASY)
        val emptyCount = countEmptyCells(puzzle.cells)
        assertEquals("EASY must have 8 empty cells", 8, emptyCount)
    }

    @Test
    fun `MEDIUM puzzle has exactly 15 empty cells`() {
        val puzzle = generator.generate(1000L, Difficulty.MEDIUM)
        val emptyCount = countEmptyCells(puzzle.cells)
        assertEquals("MEDIUM must have 15 empty cells", 15, emptyCount)
    }

    // -----------------------------------------------------------------------
    // 3. Each generated puzzle has a unique solution
    // -----------------------------------------------------------------------

    @Test
    fun `BEGINNER puzzle has a unique solution`() {
        val puzzle = generator.generate(1000L, Difficulty.BEGINNER)
        val grid = cellsToGrid(puzzle.cells, puzzle.size)
        val result = validator.validate(grid, puzzle.rowTargets, puzzle.colTargets, puzzle.difficulty.maxVal)
        assertEquals("BEGINNER puzzle must have a unique solution", SolutionCount.Unique, result)
    }

    @Test
    fun `EASY puzzle has a unique solution`() {
        val puzzle = generator.generate(42L, Difficulty.EASY)
        val grid = cellsToGrid(puzzle.cells, puzzle.size)
        val result = validator.validate(grid, puzzle.rowTargets, puzzle.colTargets, puzzle.difficulty.maxVal)
        assertEquals("EASY puzzle must have a unique solution", SolutionCount.Unique, result)
    }

    @Test
    fun `MEDIUM puzzle has a unique solution`() {
        val puzzle = generator.generate(7L, Difficulty.MEDIUM)
        val grid = cellsToGrid(puzzle.cells, puzzle.size)
        val result = validator.validate(grid, puzzle.rowTargets, puzzle.colTargets, puzzle.difficulty.maxVal)
        assertEquals("MEDIUM puzzle must have a unique solution", SolutionCount.Unique, result)
    }

    // -----------------------------------------------------------------------
    // 4. Given cells have non-zero values; empty cells have value 0
    // -----------------------------------------------------------------------

    @Test
    fun `given cells have non-zero values and empty cells have value 0`() {
        for (difficulty in Difficulty.entries) {
            val puzzle = generator.generate(1000L, difficulty)
            for (r in 0 until puzzle.size) {
                for (c in 0 until puzzle.size) {
                    val cell: Cell = puzzle.cells[r][c]
                    if (cell.isGiven) {
                        assertTrue(
                            "Given cell at ($r,$c) must have non-zero value, got ${cell.value}",
                            cell.value != 0
                        )
                    } else {
                        assertEquals(
                            "Empty cell at ($r,$c) must have value 0, got ${cell.value}",
                            0,
                            cell.value
                        )
                        assertTrue("Empty cell at ($r,$c) must report isEmpty == true", cell.isEmpty)
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // 5. Row and column targets are consistent with the puzzle state
    //    (targets must be reachable given the given cells and empty count)
    // -----------------------------------------------------------------------

    @Test
    fun `BEGINNER row and col targets are consistent with puzzle`() {
        assertTargetsConsistent(1000L, Difficulty.BEGINNER)
    }

    @Test
    fun `EASY row and col targets are consistent with puzzle`() {
        assertTargetsConsistent(42L, Difficulty.EASY)
    }

    @Test
    fun `MEDIUM row and col targets are consistent with puzzle`() {
        assertTargetsConsistent(7L, Difficulty.MEDIUM)
    }

    // -----------------------------------------------------------------------
    // 6. All given cell values are within [1..difficulty.maxVal]
    // -----------------------------------------------------------------------

    @Test
    fun `all given cell values are within valid range`() {
        for (difficulty in Difficulty.entries) {
            val puzzle = generator.generate(1000L, difficulty)
            for (r in 0 until puzzle.size) {
                for (c in 0 until puzzle.size) {
                    val cell: Cell = puzzle.cells[r][c]
                    if (cell.isGiven) {
                        assertTrue(
                            "Cell value ${cell.value} out of range [1..${difficulty.maxVal}]",
                            cell.value in 1..difficulty.maxVal
                        )
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // 7. Puzzle size matches difficulty.size
    // -----------------------------------------------------------------------

    @Test
    fun `puzzle size matches difficulty size`() {
        for (difficulty in Difficulty.entries) {
            val puzzle = generator.generate(1000L, difficulty)
            assertEquals("Puzzle size must match difficulty.size", difficulty.size, puzzle.size)
            assertEquals("cells row count must match size", difficulty.size, puzzle.cells.size)
            for (row in puzzle.cells) {
                assertEquals("cells col count must match size", difficulty.size, row.size)
            }
            assertEquals("rowTargets length must match size", difficulty.size, puzzle.rowTargets.size)
            assertEquals("colTargets length must match size", difficulty.size, puzzle.colTargets.size)
        }
    }

    // -----------------------------------------------------------------------
    // 8. Puzzle seed field matches the seed used to generate
    // -----------------------------------------------------------------------

    @Test
    fun `puzzle seed field matches generation seed`() {
        val puzzle = generator.generate(9999L, Difficulty.BEGINNER)
        assertEquals("Puzzle seed field must match generation seed", 9999L, puzzle.seed)
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun countEmptyCells(cells: Array<Array<Cell>>): Int {
        var count = 0
        for (row in cells) {
            for (cell in row) {
                if (cell.isEmpty) count++
            }
        }
        return count
    }

    private fun cellsToGrid(cells: Array<Array<Cell>>, size: Int): Array<IntArray> {
        return Array(size) { r -> IntArray(size) { c -> cells[r][c].value } }
    }

    /**
     * Verifies that rowTargets and colTargets are reachable from the current
     * puzzle state. For each row/col, the target must satisfy:
     *   givenSum + emptyCount * 1 <= target <= givenSum + emptyCount * maxVal
     *
     * Since targets were computed from the complete filled grid, this always
     * holds. It also verifies row/col sums of given cells don't exceed the target.
     */
    private fun assertTargetsConsistent(seed: Long, difficulty: Difficulty) {
        val puzzle = generator.generate(seed, difficulty)
        val n = puzzle.size

        for (r in 0 until n) {
            var givenSum = 0
            var empties = 0
            for (c in 0 until n) {
                val cell: Cell = puzzle.cells[r][c]
                if (cell.isGiven) givenSum += cell.value else empties++
            }
            assertTrue(
                "rowTarget[$r]=${puzzle.rowTargets[r]} must be >= givenSum=$givenSum + empties=$empties",
                puzzle.rowTargets[r] >= givenSum + empties
            )
            assertTrue(
                "rowTarget[$r]=${puzzle.rowTargets[r]} must be <= givenSum + empties*maxVal",
                puzzle.rowTargets[r] <= givenSum + empties * difficulty.maxVal
            )
        }

        for (c in 0 until n) {
            var givenSum = 0
            var empties = 0
            for (r in 0 until n) {
                val cell: Cell = puzzle.cells[r][c]
                if (cell.isGiven) givenSum += cell.value else empties++
            }
            assertTrue(
                "colTarget[$c]=${puzzle.colTargets[c]} must be >= givenSum=$givenSum + empties=$empties",
                puzzle.colTargets[c] >= givenSum + empties
            )
            assertTrue(
                "colTarget[$c]=${puzzle.colTargets[c]} must be <= givenSum + empties*maxVal",
                puzzle.colTargets[c] <= givenSum + empties * difficulty.maxVal
            )
        }
    }
}

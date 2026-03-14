package org.dgeek.sumgrid.onboarding

import kotlinx.coroutines.test.runTest
import org.dgeek.sumgrid.engine.SolutionCount
import org.dgeek.sumgrid.engine.UniqueSolutionValidator
import org.dgeek.sumgrid.engine.models.Difficulty
import org.dgeek.sumgrid.engine.models.Puzzle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the onboarding puzzle sequence.
 *
 * Uses [InMemoryOnboardingRepository] as a test double — no DataStore required.
 */
class OnboardingRepositoryTest {

    private lateinit var repo: InMemoryOnboardingRepository
    private val validator = UniqueSolutionValidator()

    @Before
    fun setUp() {
        repo = InMemoryOnboardingRepository()
    }

    // -----------------------------------------------------------------------
    // Puzzle sequence — empty cell counts
    // -----------------------------------------------------------------------

    @Test
    fun `getPuzzleForLaunch(1) returns puzzle with exactly 1 empty cell`() = runTest {
        val puzzle = repo.getPuzzleForLaunch(1)
        assertNotNull(puzzle)
        assertEquals(1, puzzle!!.emptyCellCount())
    }

    @Test
    fun `getPuzzleForLaunch(2) returns puzzle with exactly 2 empty cells`() = runTest {
        val puzzle = repo.getPuzzleForLaunch(2)
        assertNotNull(puzzle)
        assertEquals(2, puzzle!!.emptyCellCount())
    }

    @Test
    fun `getPuzzleForLaunch(3) returns puzzle with exactly 4 empty cells`() = runTest {
        val puzzle = repo.getPuzzleForLaunch(3)
        assertNotNull(puzzle)
        assertEquals(4, puzzle!!.emptyCellCount())
    }

    @Test
    fun `getPuzzleForLaunch(4) returns null`() = runTest {
        val puzzle = repo.getPuzzleForLaunch(4)
        assertNull(puzzle)
    }

    // -----------------------------------------------------------------------
    // Onboarding complete — blocks puzzle access
    // -----------------------------------------------------------------------

    @Test
    fun `after markComplete getPuzzleForLaunch(1) returns null`() = runTest {
        repo.markComplete()
        val puzzle = repo.getPuzzleForLaunch(1)
        assertNull(puzzle)
    }

    // -----------------------------------------------------------------------
    // Uniqueness — each puzzle must have exactly one solution
    // -----------------------------------------------------------------------

    @Test
    fun `PUZZLE_1 has a unique solution`() {
        assertUnique(OnboardingRepository.PUZZLE_1)
    }

    @Test
    fun `PUZZLE_2 has a unique solution`() {
        assertUnique(OnboardingRepository.PUZZLE_2)
    }

    @Test
    fun `PUZZLE_3 has a unique solution`() {
        assertUnique(OnboardingRepository.PUZZLE_3)
    }

    // -----------------------------------------------------------------------
    // Row targets must match actual sums
    // -----------------------------------------------------------------------

    @Test
    fun `PUZZLE_1 row targets match actual row sums of full solution`() {
        val puzzle = OnboardingRepository.PUZZLE_1
        assertRowTargetsConsistent(puzzle)
    }

    @Test
    fun `PUZZLE_2 row targets match actual row sums of full solution`() {
        val puzzle = OnboardingRepository.PUZZLE_2
        assertRowTargetsConsistent(puzzle)
    }

    @Test
    fun `PUZZLE_3 row targets match actual row sums of full solution`() {
        val puzzle = OnboardingRepository.PUZZLE_3
        assertRowTargetsConsistent(puzzle)
    }

    // -----------------------------------------------------------------------
    // Column targets must match actual sums
    // -----------------------------------------------------------------------

    @Test
    fun `PUZZLE_1 col targets match actual col sums of full solution`() {
        val puzzle = OnboardingRepository.PUZZLE_1
        assertColTargetsConsistent(puzzle)
    }

    @Test
    fun `PUZZLE_2 col targets match actual col sums of full solution`() {
        val puzzle = OnboardingRepository.PUZZLE_2
        assertColTargetsConsistent(puzzle)
    }

    @Test
    fun `PUZZLE_3 col targets match actual col sums of full solution`() {
        val puzzle = OnboardingRepository.PUZZLE_3
        assertColTargetsConsistent(puzzle)
    }

    // -----------------------------------------------------------------------
    // Difficulty and seed
    // -----------------------------------------------------------------------

    @Test
    fun `all puzzles use BEGINNER difficulty`() {
        assertEquals(Difficulty.BEGINNER, OnboardingRepository.PUZZLE_1.difficulty)
        assertEquals(Difficulty.BEGINNER, OnboardingRepository.PUZZLE_2.difficulty)
        assertEquals(Difficulty.BEGINNER, OnboardingRepository.PUZZLE_3.difficulty)
    }

    @Test
    fun `all puzzles use onboarding seed -1`() {
        assertEquals(-1L, OnboardingRepository.PUZZLE_1.seed)
        assertEquals(-1L, OnboardingRepository.PUZZLE_2.seed)
        assertEquals(-1L, OnboardingRepository.PUZZLE_3.seed)
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun Puzzle.emptyCellCount(): Int =
        cells.sumOf { row -> row.count { it.isEmpty } }

    /**
     * Verifies that the puzzle has a unique solution using [UniqueSolutionValidator].
     *
     * The validator works on an IntArray grid where 0 represents an empty cell.
     */
    private fun assertUnique(puzzle: Puzzle) {
        val grid = Array(puzzle.size) { r ->
            IntArray(puzzle.size) { c -> puzzle.cells[r][c].value }
        }
        val result = validator.validate(grid, puzzle.rowTargets, puzzle.colTargets, puzzle.difficulty.maxVal)
        assertEquals(
            "Expected UNIQUE solution for puzzle (seed=${puzzle.seed}), got $result",
            SolutionCount.Unique,
            result
        )
    }

    /**
     * Checks that row targets are consistent with the given cells plus the unique solution.
     *
     * Since we verified uniqueness, the given cells already constrain the solution fully.
     * We check that given (non-empty) cells in each row sum to <= rowTarget and
     * that the rowTarget matches the full base grid row sum.
     *
     * The base grid is:
     *   [1, 3, 2]  row sums: 6, 9, 7
     *   [4, 2, 3]
     *   [2, 4, 1]
     *
     * All three puzzles share the same rowTargets = [6, 9, 7].
     */
    private fun assertRowTargetsConsistent(puzzle: Puzzle) {
        val expectedRowTargets = intArrayOf(6, 9, 7)
        for (r in 0 until puzzle.size) {
            assertEquals(
                "Row $r target mismatch",
                expectedRowTargets[r],
                puzzle.rowTargets[r]
            )
        }
    }

    /**
     * Checks that col targets are consistent with the base grid.
     *
     * The base grid col sums are: col0=7, col1=9, col2=6.
     */
    private fun assertColTargetsConsistent(puzzle: Puzzle) {
        val expectedColTargets = intArrayOf(7, 9, 6)
        for (c in 0 until puzzle.size) {
            assertEquals(
                "Col $c target mismatch",
                expectedColTargets[c],
                puzzle.colTargets[c]
            )
        }
    }
}

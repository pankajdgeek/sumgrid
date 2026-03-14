package org.dgeek.sumgrid.viewmodel

import org.dgeek.sumgrid.engine.PuzzleGenerator
import org.dgeek.sumgrid.engine.models.Difficulty

/**
 * ViewModel for practice mode.
 *
 * Generates random puzzles using [System.currentTimeMillis] as seed.
 * Does NOT persist to [CompletionStore] — practice puzzles don't affect streak.
 * Reuses [PuzzleViewModel] internally for game state management.
 */
class PracticeViewModel(
    private val puzzleGenerator: PuzzleGenerator = PuzzleGenerator()
) {
    /** Internal puzzle VM with no completion store and no date. */
    val puzzleVm = PuzzleViewModel()

    private var lastDifficulty: Difficulty = Difficulty.BEGINNER
    private var lastSeed: Long = 0L

    /** Generate a new random puzzle for the given difficulty. */
    fun generatePuzzle(difficulty: Difficulty) {
        lastDifficulty = difficulty
        lastSeed = System.currentTimeMillis()
        val puzzle = puzzleGenerator.generate(lastSeed, difficulty)
        puzzleVm.loadPuzzle(puzzle) // no date → no persistence
    }

    /** Generate another puzzle with the same difficulty but a new seed. */
    fun playAgain() {
        generatePuzzle(lastDifficulty)
    }

    /** The seed used for the current puzzle (for testing). */
    val currentSeed: Long get() = lastSeed
}

package org.dgeek.sumgrid.daily

import org.dgeek.sumgrid.engine.PuzzleGenerator
import org.dgeek.sumgrid.engine.models.Difficulty
import org.dgeek.sumgrid.engine.models.Puzzle
import java.time.LocalDate
import java.time.ZoneId

/**
 * Repository that provides a deterministic daily puzzle for a given date and difficulty.
 *
 * Seed formula:
 *   seed = date.toEpochDay() * 3 + difficulty.seedOffset
 *
 * This guarantees:
 *  - Same puzzle on any device for the same calendar day + difficulty.
 *  - Three distinct puzzles per day (one per difficulty level).
 *
 * Completion state is persisted via [CompletionStore].
 * DataStore-backed production implementation: [DataStoreCompletionStore].
 * In-memory test implementation: [InMemoryCompletionStore].
 */
class DailyPuzzleRepository(
    private val generator: PuzzleGenerator,
    private val completionStore: CompletionStore
) {

    // -----------------------------------------------------------------------
    // Puzzle access
    // -----------------------------------------------------------------------

    /**
     * Returns the puzzle for the given [date] and [difficulty].
     *
     * The result is deterministic across all devices — same seed means same
     * generated grid (Xorshift128 is purely arithmetic, no platform RNG).
     */
    suspend fun getPuzzleForDate(date: LocalDate, difficulty: Difficulty): Puzzle {
        val seed = date.toEpochDay() * 3L + difficulty.seedOffset
        return generator.generate(seed, difficulty)
    }

    /**
     * Returns today's date in the device's local timezone.
     */
    suspend fun today(): LocalDate = LocalDate.now(ZoneId.systemDefault())

    // -----------------------------------------------------------------------
    // Completion state
    // -----------------------------------------------------------------------

    /**
     * Persists the completion state for the given date + difficulty.
     * Should be called exactly once per (date, difficulty) combination.
     */
    suspend fun saveCompletionState(date: LocalDate, difficulty: Difficulty, elapsedMillis: Long) {
        val key = completionKey(date, difficulty)
        completionStore.save(
            key,
            CompletionState(
                completed = true,
                elapsedMillis = elapsedMillis,
                completedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Returns the [CompletionState] for the given date + difficulty, or null if
     * the player has not yet completed that puzzle.
     */
    suspend fun getCompletionState(date: LocalDate, difficulty: Difficulty): CompletionState? {
        return completionStore.get(completionKey(date, difficulty))
    }

    // -----------------------------------------------------------------------
    // Key helpers
    // -----------------------------------------------------------------------

    private fun completionKey(date: LocalDate, difficulty: Difficulty): String =
        "completion_${date.toEpochDay()}_${difficulty.name}"
}

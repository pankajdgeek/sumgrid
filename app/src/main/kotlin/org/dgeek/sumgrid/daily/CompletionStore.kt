package org.dgeek.sumgrid.daily

/**
 * Minimal persistence interface for puzzle completion state.
 *
 * The key scheme is: "completion_${epochDay}_${difficulty.name}"
 *
 * Production implementation uses DataStore<Preferences>.
 * Test implementation uses [InMemoryCompletionStore].
 */
interface CompletionStore {
    suspend fun save(key: String, state: CompletionState)
    suspend fun get(key: String): CompletionState?

    /** Save in-progress puzzle state (flattened userValues array). */
    suspend fun saveInProgress(key: String, values: IntArray) {}
    /** Retrieve in-progress puzzle state, or null if none saved. */
    suspend fun getInProgress(key: String): IntArray? = null
    /** Clear in-progress state (called on completion). */
    suspend fun clearInProgress(key: String) {}
}

/**
 * Completion state persisted per (date, difficulty) pair.
 *
 * @property completed      Always true when stored (only saved on completion).
 * @property elapsedMillis  How long the player took to solve the puzzle, in ms.
 * @property completedAt    Wall-clock epoch millis when completion was recorded.
 */
data class CompletionState(
    val completed: Boolean,
    val elapsedMillis: Long,
    val completedAt: Long
)

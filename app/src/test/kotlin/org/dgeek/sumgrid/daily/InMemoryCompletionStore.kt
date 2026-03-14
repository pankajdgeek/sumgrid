package org.dgeek.sumgrid.daily

/**
 * In-memory [CompletionStore] for unit tests.
 *
 * Thread-safe enough for single-threaded coroutine tests.
 * Tracks [saveCount] so tests can assert no double-fire occurred.
 */
class InMemoryCompletionStore : CompletionStore {

    private val map = mutableMapOf<String, CompletionState>()

    /** Number of times [save] has been called — used in double-fire guards. */
    var saveCount: Int = 0
        private set

    override suspend fun save(key: String, state: CompletionState) {
        map[key] = state
        saveCount++
    }

    override suspend fun get(key: String): CompletionState? = map[key]

    override suspend fun getAll(): Map<String, CompletionState> = map.toMap()

    private val inProgress = mutableMapOf<String, IntArray>()

    override suspend fun saveInProgress(key: String, values: IntArray) {
        inProgress[key] = values.copyOf()
    }

    override suspend fun getInProgress(key: String): IntArray? = inProgress[key]?.copyOf()

    override suspend fun clearInProgress(key: String) {
        inProgress.remove(key)
    }
}

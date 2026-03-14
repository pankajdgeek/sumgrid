package org.dgeek.sumgrid.daily

/**
 * In-memory implementation of [CompletionStore] for use in unit tests.
 *
 * Stores completion records in a plain MutableMap, so tests have no dependency
 * on Android DataStore and run on the plain JVM.
 */
class InMemoryCompletionStore : CompletionStore {

    private val store = mutableMapOf<String, CompletionState>()

    /** Number of times [save] has been called. Useful for asserting no double-save. */
    var saveCount: Int = 0
        private set

    override suspend fun save(key: String, state: CompletionState) {
        store[key] = state
        saveCount++
    }

    override suspend fun get(key: String): CompletionState? = store[key]

    override suspend fun getAll(): Map<String, CompletionState> = store.toMap()

    private val inProgress = mutableMapOf<String, IntArray>()

    override suspend fun saveInProgress(key: String, values: IntArray) {
        inProgress[key] = values.copyOf()
    }

    override suspend fun getInProgress(key: String): IntArray? = inProgress[key]?.copyOf()

    override suspend fun clearInProgress(key: String) {
        inProgress.remove(key)
    }
}

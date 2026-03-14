package org.dgeek.sumgrid.daily

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

/**
 * [CompletionStore] backed by DataStore<Preferences>.
 *
 * For each completion key (e.g. "completion_20000_BEGINNER") three preference
 * entries are stored:
 *   - "${key}_completed"   — Boolean, always true when written
 *   - "${key}_elapsed"     — Long, elapsed millis
 *   - "${key}_at"          — Long, wall-clock millis (completedAt)
 */
class DataStoreCompletionStore(
    private val dataStore: DataStore<Preferences>
) : CompletionStore {

    override suspend fun save(key: String, state: CompletionState) {
        dataStore.edit { prefs ->
            prefs[booleanPreferencesKey("${key}_completed")] = state.completed
            prefs[longPreferencesKey("${key}_elapsed")] = state.elapsedMillis
            prefs[longPreferencesKey("${key}_at")] = state.completedAt
        }
    }

    override suspend fun get(key: String): CompletionState? {
        val prefs = dataStore.data.first()
        val completed = prefs[booleanPreferencesKey("${key}_completed")] ?: return null
        val elapsed = prefs[longPreferencesKey("${key}_elapsed")] ?: 0L
        val completedAt = prefs[longPreferencesKey("${key}_at")] ?: 0L
        return CompletionState(
            completed = completed,
            elapsedMillis = elapsed,
            completedAt = completedAt
        )
    }

    override suspend fun saveInProgress(key: String, values: IntArray) {
        val csv = values.joinToString(",")
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("in_progress_$key")] = csv
        }
    }

    override suspend fun getInProgress(key: String): IntArray? {
        val prefs = dataStore.data.first()
        val csv = prefs[stringPreferencesKey("in_progress_$key")] ?: return null
        return csv.split(",").map { it.toInt() }.toIntArray()
    }

    override suspend fun clearInProgress(key: String) {
        dataStore.edit { prefs ->
            prefs.remove(stringPreferencesKey("in_progress_$key"))
        }
    }
}

package org.dgeek.sumgrid.coin

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persists the player's coin balance via DataStore.
 */
class CoinRepository(
    private val dataStore: DataStore<Preferences>
) {

    private val totalCoinsKey = intPreferencesKey("total_coins")

    /** Observable total coin balance. */
    val totalCoins: Flow<Int> = dataStore.data.map { prefs ->
        prefs[totalCoinsKey] ?: 0
    }

    /** Add [amount] coins to the balance. */
    suspend fun addCoins(amount: Int) {
        dataStore.edit { prefs ->
            val current = prefs[totalCoinsKey] ?: 0
            prefs[totalCoinsKey] = current + amount
        }
    }
}

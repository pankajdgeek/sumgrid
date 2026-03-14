package org.dgeek.sumgrid.streak

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * DataStore-backed repository for tracking daily puzzle streaks and milestone badges.
 *
 * Streak rules:
 *  - First ever completion → streak = 1
 *  - Completed yesterday → streak++
 *  - Completed today already → no-op
 *  - Completed with a gap > 1 day → streak = 1
 *  - longestStreak = max(longestStreak, currentStreak) after every update
 *  - Earned badges are accumulated and never removed
 */
class StreakRepository(private val dataStore: DataStore<Preferences>) : StreakDataSource {

    private object Keys {
        val CURRENT_STREAK = intPreferencesKey("streak_current")
        val LONGEST_STREAK = intPreferencesKey("streak_longest")
        val LAST_COMPLETION_DATE = stringPreferencesKey("streak_last_date")
        // Earned badges stored as a comma-separated list of enum names, e.g. "WEEKLY_WARRIOR,MONTHLY_MASTER"
        val EARNED_BADGES = stringPreferencesKey("streak_badges")
    }

    override val streakState: Flow<StreakState> = dataStore.data.map { prefs ->
        val current = prefs[Keys.CURRENT_STREAK] ?: 0
        val longest = prefs[Keys.LONGEST_STREAK] ?: 0
        val dateStr = prefs[Keys.LAST_COMPLETION_DATE]
        val lastDate = dateStr?.let { LocalDate.parse(it) }
        val badgesStr = prefs[Keys.EARNED_BADGES] ?: ""
        val badges = parseBadges(badgesStr)
        StreakState(current, longest, lastDate, badges)
    }

    /**
     * Records a daily puzzle completion for the given [date].
     * This is idempotent for the same calendar day.
     */
    override suspend fun recordCompletion(date: LocalDate) {
        dataStore.edit { prefs ->
            val lastDateStr = prefs[Keys.LAST_COMPLETION_DATE]
            val lastDate = lastDateStr?.let { LocalDate.parse(it) }

            // No-op if already counted today
            if (lastDate == date) return@edit

            val currentStreak = prefs[Keys.CURRENT_STREAK] ?: 0
            val longestStreak = prefs[Keys.LONGEST_STREAK] ?: 0
            val badgesStr = prefs[Keys.EARNED_BADGES] ?: ""
            val earnedBadges = parseBadges(badgesStr).toMutableSet()

            val newStreak = when {
                lastDate == null -> 1                                    // first ever completion
                lastDate == date.minusDays(1) -> currentStreak + 1      // consecutive day
                else -> 1                                                // gap > 1 day, reset
            }

            val newLongest = maxOf(longestStreak, newStreak)

            // Award any newly reached milestone badges
            for (badge in StreakBadge.entries) {
                if (newStreak >= badge.requiredDays) {
                    earnedBadges.add(badge)
                }
            }

            prefs[Keys.CURRENT_STREAK] = newStreak
            prefs[Keys.LONGEST_STREAK] = newLongest
            prefs[Keys.LAST_COMPLETION_DATE] = date.toString()
            prefs[Keys.EARNED_BADGES] = earnedBadges.joinToString(",") { it.name }
        }
    }

    private fun parseBadges(csv: String): Set<StreakBadge> {
        if (csv.isBlank()) return emptySet()
        return csv.split(",")
            .mapNotNull { name ->
                StreakBadge.entries.firstOrNull { it.name == name.trim() }
            }
            .toSet()
    }
}

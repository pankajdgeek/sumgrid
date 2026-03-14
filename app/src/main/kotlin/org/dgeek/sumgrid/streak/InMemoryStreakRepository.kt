package org.dgeek.sumgrid.streak

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate

/**
 * In-memory implementation of streak tracking logic.
 *
 * Shares identical business rules with [StreakRepository] but stores state in a
 * MutableStateFlow instead of DataStore. Used in unit tests to avoid the Android
 * DataStore dependency (which requires a real Context).
 */
class InMemoryStreakRepository : StreakDataSource {

    private val _state = MutableStateFlow(StreakState())

    override val streakState: Flow<StreakState> get() = _state

    /**
     * Records a daily puzzle completion for [date].
     * Applies the same streak rules as [StreakRepository.recordCompletion].
     */
    override suspend fun recordCompletion(date: LocalDate) {
        val current = _state.value

        // No-op if already counted today
        if (current.lastCompletionDate == date) return

        val newStreak = when {
            current.lastCompletionDate == null -> 1
            current.lastCompletionDate == date.minusDays(1) -> current.currentStreak + 1
            else -> 1
        }

        val newLongest = maxOf(current.longestStreak, newStreak)

        val earnedBadges = current.earnedBadges.toMutableSet()
        for (badge in StreakBadge.entries) {
            if (newStreak >= badge.requiredDays) {
                earnedBadges.add(badge)
            }
        }

        _state.value = StreakState(
            currentStreak = newStreak,
            longestStreak = newLongest,
            lastCompletionDate = date,
            earnedBadges = earnedBadges
        )
    }
}

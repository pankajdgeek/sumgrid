package org.dgeek.sumgrid.streak

import java.time.LocalDate

/**
 * Immutable snapshot of a player's streak and badge progress.
 */
data class StreakState(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastCompletionDate: LocalDate? = null,
    val earnedBadges: Set<StreakBadge> = emptySet()
)

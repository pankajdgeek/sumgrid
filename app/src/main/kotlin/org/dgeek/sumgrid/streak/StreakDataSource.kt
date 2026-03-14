package org.dgeek.sumgrid.streak

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Common interface for streak tracking implementations.
 *
 * Production uses [StreakRepository] (DataStore-backed).
 * Tests use [InMemoryStreakRepository] (in-memory).
 */
interface StreakDataSource {
    /** Continuous stream of the current streak state. */
    val streakState: Flow<StreakState>

    /** Records a daily puzzle completion for [date]. Idempotent for the same day. */
    suspend fun recordCompletion(date: LocalDate)
}

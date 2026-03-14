package org.dgeek.sumgrid.onboarding

import org.dgeek.sumgrid.engine.models.Puzzle

/**
 * In-memory implementation of onboarding state for use in unit tests.
 *
 * Mirrors the contract of [OnboardingRepository] without requiring DataStore.
 */
class InMemoryOnboardingRepository {

    private var launchCount: Int = 0
    private var complete: Boolean = false

    suspend fun getLaunchCount(): Int = launchCount

    suspend fun incrementLaunchCount() {
        launchCount++
    }

    suspend fun isComplete(): Boolean = complete

    suspend fun markComplete() {
        complete = true
    }

    /**
     * Returns the onboarding puzzle for the given [launchCount] (1-indexed),
     * or null if [launchCount] exceeds the number of onboarding puzzles or onboarding is complete.
     */
    suspend fun getPuzzleForLaunch(launchCount: Int): Puzzle? {
        if (complete) return null
        return OnboardingRepository.PUZZLES.getOrNull(launchCount - 1)
    }
}

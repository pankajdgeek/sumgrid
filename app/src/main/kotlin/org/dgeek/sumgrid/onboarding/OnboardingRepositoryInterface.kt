package org.dgeek.sumgrid.onboarding

import org.dgeek.sumgrid.engine.models.Puzzle

/**
 * Contract for onboarding state management.
 *
 * Implemented by:
 *  - [OnboardingRepository] — production DataStore-backed implementation.
 *  - [InMemoryOnboardingRepository] — in-memory test double.
 */
interface OnboardingRepositoryInterface {
    suspend fun getLaunchCount(): Int
    suspend fun incrementLaunchCount()
    suspend fun isComplete(): Boolean
    suspend fun markComplete()
    suspend fun getPuzzleForLaunch(launchCount: Int): Puzzle?
}

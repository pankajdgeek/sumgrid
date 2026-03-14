package org.dgeek.sumgrid.analytics

/**
 * No-operation implementation of [AnalyticsTracker].
 *
 * Used in unit tests and in runtime environments where Firebase is unavailable
 * (e.g. no google-services.json present).
 */
class NoOpAnalyticsTracker : AnalyticsTracker {
    override fun puzzleStarted(difficulty: String, date: String) = Unit
    override fun puzzleCompleted(difficulty: String, date: String, elapsedSeconds: Long) = Unit
    override fun puzzleAbandoned(difficulty: String, date: String, cellsFilled: Int) = Unit
    override fun shareTapped(difficulty: String, date: String) = Unit
    override fun streakMilestone(streakDays: Int) = Unit
    override fun onboardingCompleted(attemptNumber: Int) = Unit
    override fun appOpen() = Unit
}

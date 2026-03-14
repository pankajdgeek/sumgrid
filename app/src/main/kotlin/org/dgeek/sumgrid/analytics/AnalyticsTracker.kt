package org.dgeek.sumgrid.analytics

interface AnalyticsTracker {
    fun puzzleStarted(difficulty: String, date: String)
    fun puzzleCompleted(difficulty: String, date: String, elapsedSeconds: Long)
    fun puzzleAbandoned(difficulty: String, date: String, cellsFilled: Int)
    fun shareTapped(difficulty: String, date: String)
    fun streakMilestone(streakDays: Int)
    fun onboardingCompleted(attemptNumber: Int)
    fun appOpen()
}

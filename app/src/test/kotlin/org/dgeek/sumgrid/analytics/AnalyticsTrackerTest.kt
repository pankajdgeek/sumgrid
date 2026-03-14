package org.dgeek.sumgrid.analytics

import org.junit.Assert.assertNotNull
import org.junit.Test

class AnalyticsTrackerTest {

    private val tracker: AnalyticsTracker = NoOpAnalyticsTracker()

    @Test
    fun `NoOpAnalyticsTracker puzzleStarted does not throw`() {
        tracker.puzzleStarted("easy", "2026-03-14")
    }

    @Test
    fun `NoOpAnalyticsTracker puzzleCompleted does not throw`() {
        tracker.puzzleCompleted("medium", "2026-03-14", 120L)
    }

    @Test
    fun `NoOpAnalyticsTracker puzzleAbandoned does not throw`() {
        tracker.puzzleAbandoned("hard", "2026-03-14", 5)
    }

    @Test
    fun `NoOpAnalyticsTracker shareTapped does not throw`() {
        tracker.shareTapped("easy", "2026-03-14")
    }

    @Test
    fun `NoOpAnalyticsTracker streakMilestone does not throw`() {
        tracker.streakMilestone(7)
    }

    @Test
    fun `NoOpAnalyticsTracker onboardingCompleted does not throw`() {
        tracker.onboardingCompleted(1)
    }

    @Test
    fun `NoOpAnalyticsTracker appOpen does not throw`() {
        tracker.appOpen()
    }

    @Test
    fun `NoOpAnalyticsTracker instance is not null`() {
        assertNotNull(tracker)
    }

    @Test
    fun `AnalyticsTracker interface has puzzleStarted method with correct signature`() {
        // Verifies the interface can be implemented without compile errors.
        val customImpl = object : AnalyticsTracker {
            override fun puzzleStarted(difficulty: String, date: String) {}
            override fun puzzleCompleted(difficulty: String, date: String, elapsedSeconds: Long) {}
            override fun puzzleAbandoned(difficulty: String, date: String, cellsFilled: Int) {}
            override fun shareTapped(difficulty: String, date: String) {}
            override fun streakMilestone(streakDays: Int) {}
            override fun onboardingCompleted(attemptNumber: Int) {}
            override fun appOpen() {}
        }
        assertNotNull(customImpl)
    }
}

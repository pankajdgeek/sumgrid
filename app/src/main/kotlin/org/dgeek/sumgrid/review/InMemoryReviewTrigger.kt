package org.dgeek.sumgrid.review

import android.app.Activity
import org.dgeek.sumgrid.streak.StreakBadge

/**
 * In-memory test double for [InAppReviewTrigger].
 *
 * No DataStore, no Play Store — all state lives in plain fields.
 * Suitable for unit tests and Preview scaffolding.
 */
class InMemoryReviewTrigger {

    private var lifetimeCompletions: Int = 0
    private var practiceCompletions: Int = 0
    private var reviewRequested: Boolean = false

    /** Tracks whether [requestReviewIfEligible] actually triggered a review. */
    var reviewLaunched: Boolean = false
        private set

    /** Tracks whether [launchManualReview] was invoked. */
    var manualReviewLaunched: Boolean = false
        private set

    suspend fun recordDailyCompletion() {
        lifetimeCompletions++
    }

    suspend fun recordPracticeCompletion() {
        practiceCompletions++
    }

    suspend fun getLifetimeCompletions(): Int = lifetimeCompletions

    suspend fun getPracticeCompletions(): Int = practiceCompletions

    suspend fun isReviewRequested(): Boolean = reviewRequested

    suspend fun isEligibleAfterDaily(): Boolean =
        !reviewRequested && lifetimeCompletions >= InAppReviewTrigger.DAILY_THRESHOLD

    suspend fun isEligibleAfterPractice(): Boolean =
        !reviewRequested && practiceCompletions >= InAppReviewTrigger.PRACTICE_THRESHOLD

    suspend fun isEligibleAfterStreak(currentStreak: Int): Boolean {
        if (reviewRequested) return false
        return StreakBadge.entries.any { it.requiredDays == currentStreak }
    }

    /**
     * Mirrors the automatic eligibility logic of [InAppReviewTrigger]. Flips
     * [reviewRequested] before "launching" so retries can't re-prompt.
     */
    suspend fun requestReviewIfEligible(activity: Activity? = null) {
        if (reviewRequested) return
        reviewRequested = true
        reviewLaunched = true
    }

    /**
     * Mirrors the manual path — does not consult or modify [reviewRequested].
     */
    suspend fun launchManualReview(activity: Activity? = null) {
        manualReviewLaunched = true
    }
}

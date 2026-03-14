package org.dgeek.sumgrid.review

import android.app.Activity

/**
 * In-memory test double for [InAppReviewTrigger].
 *
 * No DataStore, no Play Store — all state lives in plain fields.
 * Suitable for unit tests and Preview scaffolding.
 */
class InMemoryReviewTrigger {

    private var lifetimeCompletions: Int = 0
    private var reviewRequested: Boolean = false

    /** Tracks whether [requestReviewIfEligible] actually triggered a review. */
    var reviewLaunched: Boolean = false
        private set

    suspend fun recordCompletion() {
        lifetimeCompletions++
    }

    suspend fun getLifetimeCompletions(): Int = lifetimeCompletions

    suspend fun isReviewRequested(): Boolean = reviewRequested

    /**
     * Mirrors the eligibility logic of [InAppReviewTrigger] without any Play Store calls.
     */
    suspend fun requestReviewIfEligible(activity: Activity? = null) {
        if (lifetimeCompletions < InAppReviewTrigger.REVIEW_THRESHOLD) return
        if (reviewRequested) return

        reviewRequested = true
        reviewLaunched = true
    }
}

package org.dgeek.sumgrid.review

import android.app.Activity
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

/**
 * Triggers the Play Store in-app review flow after the user has completed
 * [REVIEW_THRESHOLD] puzzles for the first time.
 *
 * Rules:
 * - Increments a lifetime completion counter on each call to [recordCompletion].
 * - Requests the review dialog once the counter reaches [REVIEW_THRESHOLD].
 * - Never requests again (flag persisted to DataStore).
 * - Wraps all Play-API calls in try-catch so sideloaded / emulator builds never crash.
 */
class InAppReviewTrigger(
    private val context: Context,
    private val dataStore: DataStore<Preferences>,
) {

    companion object {
        val REVIEW_REQUESTED = booleanPreferencesKey("review_requested")
        val LIFETIME_COMPLETIONS = intPreferencesKey("lifetime_completions")
        const val REVIEW_THRESHOLD = 3
    }

    /**
     * Increment the lifetime puzzle completion counter.
     * Called from PuzzleViewModel on each puzzle completion.
     */
    suspend fun recordCompletion() {
        dataStore.edit { prefs ->
            val current = prefs[LIFETIME_COMPLETIONS] ?: 0
            prefs[LIFETIME_COMPLETIONS] = current + 1
        }
    }

    /**
     * Get current lifetime completion count.
     */
    suspend fun getLifetimeCompletions(): Int {
        return dataStore.data.first()[LIFETIME_COMPLETIONS] ?: 0
    }

    /**
     * Check if the review has already been requested.
     */
    suspend fun isReviewRequested(): Boolean {
        return dataStore.data.first()[REVIEW_REQUESTED] ?: false
    }

    /**
     * Request an in-app review if eligible:
     * - lifetime completions >= [REVIEW_THRESHOLD]
     * - review not yet requested
     *
     * @param activity The current Activity for launching the review flow.
     */
    suspend fun requestReviewIfEligible(activity: Activity) {
        val completions = getLifetimeCompletions()
        if (completions < REVIEW_THRESHOLD) return
        if (isReviewRequested()) return

        // Mark as requested before attempting — prevents retry storms even on failure.
        dataStore.edit { prefs -> prefs[REVIEW_REQUESTED] = true }

        try {
            val manager = ReviewManagerFactory.create(context)
            val reviewInfo = manager.requestReviewFlow().await()
            manager.launchReviewFlow(activity, reviewInfo).await()
        } catch (_: Exception) {
            // Play Store unavailable (emulator, sideloaded APK, no network) — ignore.
        }
    }
}

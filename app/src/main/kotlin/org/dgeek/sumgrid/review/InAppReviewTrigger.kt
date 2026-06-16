package org.dgeek.sumgrid.review

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import org.dgeek.sumgrid.streak.StreakBadge

/**
 * Triggers the Play Store in-app review flow.
 *
 * Three automatic trigger paths share a single per-install budget — once any
 * one fires, the [REVIEW_REQUESTED] flag is set and no further automatic
 * prompts are shown:
 *  - Daily puzzle: lifetime completions reach [DAILY_THRESHOLD].
 *  - Practice puzzle: lifetime practice completions reach [PRACTICE_THRESHOLD].
 *  - Streak milestone: current streak hits any [StreakBadge.requiredDays] value.
 *
 * A fourth manual path ([launchManualReview]) is unconditional — it never
 * checks or sets the budget flag, so a user can always re-rate from Settings.
 * If the Play in-app flow is unavailable (sideload, no Play services, quota
 * exhausted), it falls back to opening the Play Store listing via Intent.
 */
class InAppReviewTrigger(
    private val context: Context,
    private val dataStore: DataStore<Preferences>,
) {

    companion object {
        private const val TAG = "InAppReview"
        val REVIEW_REQUESTED = booleanPreferencesKey("review_requested")
        val LIFETIME_COMPLETIONS = intPreferencesKey("lifetime_completions")
        val PRACTICE_COMPLETIONS = intPreferencesKey("practice_completions")
        const val DAILY_THRESHOLD = 3
        const val PRACTICE_THRESHOLD = 3
    }

    suspend fun recordDailyCompletion() {
        dataStore.edit { prefs ->
            val current = prefs[LIFETIME_COMPLETIONS] ?: 0
            prefs[LIFETIME_COMPLETIONS] = current + 1
            Log.d(TAG, "recordDailyCompletion: lifetime=${current + 1}")
        }
    }

    suspend fun recordPracticeCompletion() {
        dataStore.edit { prefs ->
            val current = prefs[PRACTICE_COMPLETIONS] ?: 0
            prefs[PRACTICE_COMPLETIONS] = current + 1
            Log.d(TAG, "recordPracticeCompletion: practice=${current + 1}")
        }
    }

    suspend fun getLifetimeCompletions(): Int =
        dataStore.data.first()[LIFETIME_COMPLETIONS] ?: 0

    suspend fun getPracticeCompletions(): Int =
        dataStore.data.first()[PRACTICE_COMPLETIONS] ?: 0

    suspend fun isReviewRequested(): Boolean =
        dataStore.data.first()[REVIEW_REQUESTED] ?: false

    suspend fun isEligibleAfterDaily(): Boolean =
        !isReviewRequested() && getLifetimeCompletions() >= DAILY_THRESHOLD

    suspend fun isEligibleAfterPractice(): Boolean =
        !isReviewRequested() && getPracticeCompletions() >= PRACTICE_THRESHOLD

    suspend fun isEligibleAfterStreak(currentStreak: Int): Boolean {
        if (isReviewRequested()) return false
        return StreakBadge.entries.any { it.requiredDays == currentStreak }
    }

    /**
     * Automatic path. If the per-install budget is not yet spent and at least
     * one eligibility check holds (daily / practice / streak — caller is
     * expected to verify before invoking), launch the in-app review flow.
     *
     * Flips [REVIEW_REQUESTED] *before* the Play call so a transient failure
     * never causes a re-prompt on the next eligible event.
     */
    suspend fun requestReviewIfEligible(activity: Activity) {
        if (isReviewRequested()) {
            Log.d(TAG, "requestReviewIfEligible: skipped — budget already spent")
            return
        }
        Log.d(TAG, "requestReviewIfEligible: spending budget and launching Play review flow")
        dataStore.edit { prefs -> prefs[REVIEW_REQUESTED] = true }

        try {
            val manager = ReviewManagerFactory.create(context)
            val reviewInfo = manager.requestReviewFlow().await()
            manager.launchReviewFlow(activity, reviewInfo).await()
            Log.d(TAG, "requestReviewIfEligible: launchReviewFlow returned (may be no-op on dev builds)")
        } catch (e: Exception) {
            // Play Store unavailable (emulator, sideloaded APK, no network) — ignore.
            Log.d(TAG, "requestReviewIfEligible: Play API unavailable (${e.javaClass.simpleName}: ${e.message})")
        }
    }

    /**
     * Manual path. Always attempts the Play in-app review flow regardless of
     * [REVIEW_REQUESTED] state, and falls back to opening the Play Store
     * listing if the in-app flow is unavailable. Does not modify the budget
     * flag — users can re-tap "Rate" without preempting automatic prompts
     * that haven't fired yet.
     */
    suspend fun launchManualReview(activity: Activity) {
        Log.d(TAG, "launchManualReview: trying Play in-app review flow")
        try {
            val manager = ReviewManagerFactory.create(context)
            val reviewInfo = manager.requestReviewFlow().await()
            manager.launchReviewFlow(activity, reviewInfo).await()
            Log.d(TAG, "launchManualReview: launchReviewFlow returned")
        } catch (e: Exception) {
            Log.d(TAG, "launchManualReview: Play API failed (${e.javaClass.simpleName}: ${e.message}) — opening Play Store listing")
            openPlayStoreListing(activity)
        }
    }

    private fun openPlayStoreListing(activity: Activity) {
        val pkg = context.packageName
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            activity.startActivity(marketIntent)
            Log.d(TAG, "openPlayStoreListing: market:// intent launched for $pkg")
        } catch (_: ActivityNotFoundException) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$pkg"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                activity.startActivity(webIntent)
                Log.d(TAG, "openPlayStoreListing: https fallback launched for $pkg")
            } catch (_: ActivityNotFoundException) {
                Log.w(TAG, "openPlayStoreListing: no Play Store and no browser available")
            }
        }
    }
}

package org.dgeek.sumgrid.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Firebase-backed implementation of [AnalyticsTracker].
 *
 * Each method maps to a named Firebase event with appropriate parameters.
 * All Firebase calls are wrapped in try-catch so the app continues to function
 * in environments where google-services.json has not been provisioned.
 */
class FirebaseAnalyticsTracker(context: Context) : AnalyticsTracker {

    private val firebaseAnalytics: FirebaseAnalytics? = try {
        FirebaseAnalytics.getInstance(context)
    } catch (e: Exception) {
        null
    }

    override fun puzzleStarted(difficulty: String, date: String) {
        try {
            firebaseAnalytics?.logEvent("puzzle_started", Bundle().apply {
                putString("difficulty", difficulty)
                putString("date", date)
            })
        } catch (e: Exception) {
            // Firebase not available — silently ignore
        }
    }

    override fun puzzleCompleted(difficulty: String, date: String, elapsedSeconds: Long) {
        try {
            firebaseAnalytics?.logEvent("puzzle_completed", Bundle().apply {
                putString("difficulty", difficulty)
                putString("date", date)
                putLong("elapsed_seconds", elapsedSeconds)
            })
        } catch (e: Exception) {
            // Firebase not available — silently ignore
        }
    }

    override fun puzzleAbandoned(difficulty: String, date: String, cellsFilled: Int) {
        try {
            firebaseAnalytics?.logEvent("puzzle_abandoned", Bundle().apply {
                putString("difficulty", difficulty)
                putString("date", date)
                putInt("cells_filled", cellsFilled)
            })
        } catch (e: Exception) {
            // Firebase not available — silently ignore
        }
    }

    override fun shareTapped(difficulty: String, date: String) {
        try {
            firebaseAnalytics?.logEvent("share_tapped", Bundle().apply {
                putString("difficulty", difficulty)
                putString("date", date)
            })
        } catch (e: Exception) {
            // Firebase not available — silently ignore
        }
    }

    override fun streakMilestone(streakDays: Int) {
        try {
            firebaseAnalytics?.logEvent("streak_milestone", Bundle().apply {
                putInt("streak_days", streakDays)
            })
        } catch (e: Exception) {
            // Firebase not available — silently ignore
        }
    }

    override fun onboardingCompleted(attemptNumber: Int) {
        try {
            firebaseAnalytics?.logEvent("onboarding_completed", Bundle().apply {
                putInt("attempt_number", attemptNumber)
            })
        } catch (e: Exception) {
            // Firebase not available — silently ignore
        }
    }

    override fun appOpen() {
        try {
            firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.APP_OPEN, null)
        } catch (e: Exception) {
            // Firebase not available — silently ignore
        }
    }
}

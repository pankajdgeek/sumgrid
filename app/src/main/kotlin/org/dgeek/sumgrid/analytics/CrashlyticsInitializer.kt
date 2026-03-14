package org.dgeek.sumgrid.analytics

import android.content.Context
import android.content.pm.PackageManager

/**
 * Initializes Crashlytics with app-level custom keys for richer crash reports.
 *
 * All Crashlytics calls are wrapped in try-catch to ensure the app functions
 * correctly in environments without google-services.json (CI, local dev without
 * Firebase project configured).
 */
object CrashlyticsInitializer {

    /**
     * Sets well-known custom keys on the Crashlytics session.
     *
     * Call once from [Application.onCreate] after Firebase.initializeApp.
     */
    fun initialize(context: Context) {
        try {
            val crashlytics = com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()

            val versionName = try {
                context.packageManager
                    .getPackageInfo(context.packageName, 0)
                    .versionName ?: "unknown"
            } catch (e: PackageManager.NameNotFoundException) {
                "unknown"
            }

            crashlytics.setCustomKey("current_difficulty", "unknown")
            crashlytics.setCustomKey("puzzle_day_number", -1)
            crashlytics.setCustomKey("app_version", versionName)
        } catch (e: Exception) {
            // Firebase/Crashlytics not available — silently ignore
        }
    }
}

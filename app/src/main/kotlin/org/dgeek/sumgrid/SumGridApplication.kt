package org.dgeek.sumgrid

import android.app.Application

/**
 * Application entry point.
 *
 * Firebase initialization (Analytics + Crashlytics) will be added in Sprint S03
 * once google-services.json is provisioned per-machine.
 */
class SumGridApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Firebase.initialize(this) — added in S03
    }
}

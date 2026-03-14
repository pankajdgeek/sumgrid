package org.dgeek.sumgrid

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import org.dgeek.sumgrid.daily.DataStoreCompletionStore
import org.dgeek.sumgrid.daily.DailyPuzzleRepository
import org.dgeek.sumgrid.engine.PuzzleGenerator
import org.dgeek.sumgrid.engine.UniqueSolutionValidator

// ---------------------------------------------------------------------------
// DataStore singletons (one per named store)
// ---------------------------------------------------------------------------

/** Stores puzzle completion state: completed flag, elapsed millis, completedAt. */
private val Context.puzzleDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "puzzle_completion"
)

/** Stores streak data: currentStreak, longestStreak, lastCompletionDate. */
private val Context.streakDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "streak"
)

/** Stores onboarding / difficulty preference. */
private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "onboarding"
)

// ---------------------------------------------------------------------------
// Dependency container
// ---------------------------------------------------------------------------

/**
 * Lightweight manual DI container.
 *
 * All dependencies are lazy so they are only created when first accessed.
 * This avoids doing expensive work during [Application.onCreate].
 */
class AppContainer(application: SumGridApplication) {

    val validator: UniqueSolutionValidator by lazy { UniqueSolutionValidator() }

    val puzzleGenerator: PuzzleGenerator by lazy { PuzzleGenerator(validator) }

    val completionStore: DataStoreCompletionStore by lazy {
        DataStoreCompletionStore(application.puzzleDataStore)
    }

    val dailyPuzzleRepository: DailyPuzzleRepository by lazy {
        DailyPuzzleRepository(puzzleGenerator, completionStore)
    }
}

// ---------------------------------------------------------------------------
// Application
// ---------------------------------------------------------------------------

/**
 * Application entry point.
 *
 * Exposes [container] so Activities and ViewModels can obtain dependencies
 * without a DI framework. Firebase initialization is added in Sprint S03
 * once google-services.json is provisioned per-machine.
 */
class SumGridApplication : Application() {

    /** Application-scoped dependency container. Initialized lazily on first access. */
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // Firebase.initialize(this) — added in S03
    }
}

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
import org.dgeek.sumgrid.analytics.AnalyticsTracker
import org.dgeek.sumgrid.analytics.NoOpAnalyticsTracker
import org.dgeek.sumgrid.onboarding.OnboardingRepository
import org.dgeek.sumgrid.coin.CoinRepository
import org.dgeek.sumgrid.review.InAppReviewTrigger
import org.dgeek.sumgrid.streak.InMemoryStreakRepository
import org.dgeek.sumgrid.streak.StreakRepository

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

/** Stores in-app review state: lifetime_completions, review_requested. */
private val Context.reviewDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "review"
)

/** Stores coin balance for practice mode rewards. */
private val Context.coinDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "coins"
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

    val streakRepository: StreakRepository by lazy {
        StreakRepository(application.streakDataStore)
    }

    /**
     * In-memory streak repository used by [HomeViewModel].
     * For the MVP this is the primary streak source;
     * [streakRepository] is kept for future DataStore-backed persistence.
     */
    val inMemoryStreakRepository: InMemoryStreakRepository by lazy {
        InMemoryStreakRepository()
    }

    /** Analytics tracker — NoOp by default since Firebase requires google-services.json. */
    val analyticsTracker: AnalyticsTracker by lazy {
        NoOpAnalyticsTracker()
    }

    /** Onboarding repository — tracks launch count and curated puzzle progression. */
    val onboardingRepository: OnboardingRepository by lazy {
        OnboardingRepository(application.onboardingDataStore)
    }

    /**
     * In-app review trigger. Tracks lifetime puzzle completions and requests the
     * Play Store review dialog once the user has completed [InAppReviewTrigger.REVIEW_THRESHOLD]
     * puzzles. The request is made at most once per install.
     */
    val reviewTrigger: InAppReviewTrigger by lazy {
        InAppReviewTrigger(application, application.reviewDataStore)
    }

    /** Coin repository for practice mode rewards. */
    val coinRepository: CoinRepository by lazy {
        CoinRepository(application.coinDataStore)
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

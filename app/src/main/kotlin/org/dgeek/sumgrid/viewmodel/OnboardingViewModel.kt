package org.dgeek.sumgrid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dgeek.sumgrid.engine.models.Puzzle
import org.dgeek.sumgrid.onboarding.OnboardingRepository
import org.dgeek.sumgrid.onboarding.OnboardingRepositoryInterface

/**
 * Manages onboarding state: launch count progression through the three curated
 * onboarding puzzles and the final completion flag.
 *
 * Production usage: instantiate via [ViewModelFactory].
 * Unit-test usage: pass a [CoroutineScope] (e.g. Dispatchers.Unconfined) so
 * suspend calls settle without a Main-thread Looper.
 *
 * @param onboardingRepository  Provides launch count, puzzle sequence, and completion state.
 * @param ioScope               Coroutine scope for DataStore reads/writes.
 *                              Defaults to null — falls back to [viewModelScope] in production.
 */
class OnboardingViewModel(
    private val onboardingRepository: OnboardingRepositoryInterface,
    private val ioScope: CoroutineScope? = null
) : ViewModel() {

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    private val _launchCount = MutableStateFlow(0)
    /** Current launch count (1-indexed once the first puzzle has been loaded). */
    val launchCount: StateFlow<Int> = _launchCount.asStateFlow()

    private val _isOnboardingComplete = MutableStateFlow(false)
    /** True once [markComplete] has been called or the repository already holds a complete flag. */
    val isOnboardingComplete: StateFlow<Boolean> = _isOnboardingComplete.asStateFlow()

    private val _currentPuzzle = MutableStateFlow<Puzzle?>(null)
    /** The puzzle for the current launch, or null when onboarding is complete. */
    val currentPuzzle: StateFlow<Puzzle?> = _currentPuzzle.asStateFlow()

    // -----------------------------------------------------------------------
    // Actions
    // -----------------------------------------------------------------------

    /**
     * Increment the persistent launch count and return the corresponding
     * onboarding puzzle (or null if onboarding is already complete / exhausted).
     *
     * Side effects:
     *  - Increments [launchCount].
     *  - Updates [currentPuzzle].
     *  - Automatically calls [markComplete] if launch 3 is now exhausted
     *    (i.e. the incremented count exceeds the available puzzles).
     */
    suspend fun incrementLaunchAndGetPuzzle(): Puzzle? {
        onboardingRepository.incrementLaunchCount()
        val newCount = onboardingRepository.getLaunchCount()
        _launchCount.value = newCount

        val puzzle = onboardingRepository.getPuzzleForLaunch(newCount)
        _currentPuzzle.value = puzzle

        // If we just exhausted all three puzzles, mark complete automatically.
        if (puzzle == null && newCount > OnboardingRepository.PUZZLES.size) {
            markComplete()
        }

        return puzzle
    }

    /**
     * Mark onboarding as permanently complete.
     * Updates [isOnboardingComplete] and persists to the repository.
     */
    suspend fun markComplete() {
        onboardingRepository.markComplete()
        _isOnboardingComplete.value = true
    }

    /**
     * Initialise state from the repository without advancing the launch count.
     * Call this on ViewModel creation to reflect any pre-existing persisted state.
     */
    fun loadInitialState() {
        val scope = ioScope ?: viewModelScope
        scope.launch {
            _launchCount.value = onboardingRepository.getLaunchCount()
            _isOnboardingComplete.value = onboardingRepository.isComplete()
        }
    }
}

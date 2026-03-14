package org.dgeek.sumgrid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dgeek.sumgrid.daily.CompletionState
import org.dgeek.sumgrid.daily.DailyPuzzleRepository
import org.dgeek.sumgrid.engine.models.Difficulty
import org.dgeek.sumgrid.streak.InMemoryStreakRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

// ---------------------------------------------------------------------------
// UI state
// ---------------------------------------------------------------------------

/**
 * Completion status of a single difficulty's puzzle on the home screen.
 */
data class PuzzleStatus(
    val difficulty: Difficulty,
    val completionState: CompletionState? = null
) {
    val isCompleted: Boolean get() = completionState?.completed == true
}

/**
 * Full UI state for the home screen.
 */
data class HomeUiState(
    /** Completion status for each difficulty (Beginner, Easy, Medium). */
    val puzzleStatuses: List<PuzzleStatus> = Difficulty.entries.map { PuzzleStatus(it) },
    /** Current daily streak. */
    val currentStreak: Int = 0,
    /** Seconds until midnight local time (when new puzzles become available). */
    val secondsUntilMidnight: Long = 0L,
    /** Whether the home screen data has finished loading. */
    val isLoading: Boolean = true
)

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

/**
 * Manages all state for the home screen.
 *
 * Responsibilities:
 *  - Load today's puzzle completion status for all three difficulties.
 *  - Observe the current streak.
 *  - Provide a countdown to midnight (when new puzzles unlock).
 *
 * Dependencies are provided via constructor injection.
 *
 * @param puzzleRepository  Source of puzzle and completion data.
 * @param streakRepository  Source of streak data.
 * @param clock             Supplier of the current [LocalDate] (injectable for testing).
 */
class HomeViewModel(
    private val puzzleRepository: DailyPuzzleRepository,
    private val streakRepository: InMemoryStreakRepository,
    private val clock: () -> LocalDate = { LocalDate.now(ZoneId.systemDefault()) }
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
        observeStreak()
    }

    // -----------------------------------------------------------------------
    // Loading
    // -----------------------------------------------------------------------

    /**
     * Load completion statuses for today's three puzzles.
     * Called once on init; can also be re-invoked after completing a puzzle.
     */
    fun loadHomeData() {
        viewModelScope.launch {
            val today = puzzleRepository.today()
            val statuses = Difficulty.entries.map { difficulty ->
                val completion = puzzleRepository.getCompletionState(today, difficulty)
                PuzzleStatus(difficulty = difficulty, completionState = completion)
            }
            _uiState.value = _uiState.value.copy(
                puzzleStatuses = statuses,
                secondsUntilMidnight = secondsUntilMidnight(),
                isLoading = false
            )
        }
    }

    // -----------------------------------------------------------------------
    // Streak observation
    // -----------------------------------------------------------------------

    private fun observeStreak() {
        viewModelScope.launch {
            streakRepository.streakState.collect { state ->
                _uiState.value = _uiState.value.copy(
                    currentStreak = state.currentStreak
                )
            }
        }
    }

    // -----------------------------------------------------------------------
    // Countdown
    // -----------------------------------------------------------------------

    /**
     * Refresh the countdown value. Call every second from the UI.
     */
    fun tickCountdown() {
        _uiState.value = _uiState.value.copy(
            secondsUntilMidnight = secondsUntilMidnight()
        )
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Seconds remaining until midnight in the device's local timezone.
     * Returns 0 if midnight has passed (should not happen in practice).
     */
    internal fun secondsUntilMidnight(): Long {
        val now = LocalTime.now(ZoneId.systemDefault())
        val midnight = LocalTime.MIDNIGHT
        val secondsElapsed = now.toSecondOfDay().toLong()
        val secondsInDay = 24L * 60L * 60L
        val remaining = secondsInDay - secondsElapsed
        return if (remaining > 0L) remaining else 0L
    }

    /**
     * Format a seconds-until-midnight value as "HH:MM:SS".
     *
     * @param seconds Total seconds remaining.
     * @return Zero-padded hours:minutes:seconds string.
     */
    fun formatCountdown(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }
}

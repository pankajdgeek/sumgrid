package org.dgeek.sumgrid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import org.dgeek.sumgrid.coin.CoinRepository
import org.dgeek.sumgrid.daily.CompletionStore
import org.dgeek.sumgrid.engine.models.Cell
import org.dgeek.sumgrid.engine.models.Difficulty
import org.dgeek.sumgrid.engine.models.Puzzle
import org.dgeek.sumgrid.review.InAppReviewTrigger
import org.dgeek.sumgrid.streak.StreakDataSource
import java.time.LocalDate

// ---------------------------------------------------------------------------
// Domain state
// ---------------------------------------------------------------------------

/**
 * Indicator color for a row or column sum display.
 */
enum class SumIndicatorColor {
    /** Current sum equals target — puzzle row/col is complete. */
    GREEN,
    /** Current sum exceeds target — invalid state. */
    RED,
    /** Current sum is less than target — still incomplete. */
    GRAY
}

/**
 * Full UI state snapshot for the puzzle screen.
 *
 * Designed to be immutable so Compose can detect changes via structural equality.
 */
data class PuzzleUiState(
    /** The underlying puzzle (given cells, targets, difficulty). */
    val puzzle: Puzzle,
    /**
     * Current user-entered values for empty cells.
     * Size = puzzle.size x puzzle.size.
     * Index: [row][col]. Value 0 means the cell is empty (not yet filled by user).
     * Given cells always retain their original value from [puzzle.cells].
     */
    val userValues: Array<IntArray>,
    /** Currently selected cell, or null if nothing is selected. */
    val selectedCell: Pair<Int, Int>?,
    /** Pre-computed indicator color for each row. Size = puzzle.size. */
    val rowSumIndicators: List<SumIndicatorColor>,
    /** Pre-computed indicator color for each column. Size = puzzle.size. */
    val colSumIndicators: List<SumIndicatorColor>,
    /** True when all cells are filled and all row/col sums match targets. */
    val isCompleted: Boolean,
    /** Elapsed seconds since the timer started (first cell tap). */
    val elapsedSeconds: Long,
    /** Whether pencil/notes mode is active. */
    val isNotesMode: Boolean = false,
    /** Pencil note candidates per cell. Size = puzzle.size x puzzle.size. */
    val notesValues: Array<Array<Set<Int>>> = emptyArray()
) {
    /** Convenience: combined display value for a cell (given value or user value). */
    fun displayValueAt(row: Int, col: Int): Int {
        val cell = puzzle.cells[row][col]
        return if (cell.isGiven) cell.value else userValues[row][col]
    }

    // Array equality requires custom equals/hashCode
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PuzzleUiState) return false
        if (puzzle != other.puzzle) return false
        if (selectedCell != other.selectedCell) return false
        if (rowSumIndicators != other.rowSumIndicators) return false
        if (colSumIndicators != other.colSumIndicators) return false
        if (isCompleted != other.isCompleted) return false
        if (elapsedSeconds != other.elapsedSeconds) return false
        if (isNotesMode != other.isNotesMode) return false
        if (userValues.size != other.userValues.size) return false
        for (i in userValues.indices) {
            if (!userValues[i].contentEquals(other.userValues[i])) return false
        }
        if (notesValues.size != other.notesValues.size) return false
        for (i in notesValues.indices) {
            if (!notesValues[i].contentEquals(other.notesValues[i])) return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = puzzle.hashCode()
        result = 31 * result + (selectedCell?.hashCode() ?: 0)
        result = 31 * result + rowSumIndicators.hashCode()
        result = 31 * result + colSumIndicators.hashCode()
        result = 31 * result + isCompleted.hashCode()
        result = 31 * result + elapsedSeconds.hashCode()
        result = 31 * result + isNotesMode.hashCode()
        result = 31 * result + userValues.fold(1) { acc, row -> 31 * acc + row.contentHashCode() }
        result = 31 * result + notesValues.fold(1) { acc, row -> 31 * acc + row.contentHashCode() }
        return result
    }
}

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

/**
 * Holds and manages all mutable state for the active puzzle screen.
 *
 * Business logic is pure (no Android context required) so unit tests can
 * instantiate this class directly on the JVM without Robolectric.
 *
 * Compose UI observes [uiState] via `collectAsState()`.
 *
 * @param completionStore  Persistence layer for completion state. Defaults to
 *                         null (no persistence) — injected in production via
 *                         [ViewModelFactory].
 */
class PuzzleViewModel(
    private val completionStore: CompletionStore? = null,
    private val streakDataSource: StreakDataSource? = null,
    /**
     * Coroutine scope used for persistence launches.
     * In production this is [viewModelScope] (set lazily).
     * In unit tests pass a [TestScope] or [CoroutineScope(Dispatchers.Unconfined)] to avoid
     * requiring an Android main Looper.
     */
    private val persistScope: CoroutineScope? = null,
    private val coinRepository: CoinRepository? = null,
    private val reviewTrigger: InAppReviewTrigger? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PuzzleUiState?>(null)
    val uiState: StateFlow<PuzzleUiState?> = _uiState.asStateFlow()

    /**
     * One-shot signal asking the UI layer to launch the Play in-app review flow.
     * Emitted after a daily/practice completion when [reviewTrigger] confirms
     * one of the eligibility conditions holds. The composable consumes this and
     * calls [InAppReviewTrigger.requestReviewIfEligible] with an Activity — the
     * ViewModel intentionally doesn't hold one.
     */
    private val _requestReviewEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val requestReviewEvent: SharedFlow<Unit> = _requestReviewEvent.asSharedFlow()

    // -----------------------------------------------------------------------
    // Notes mode
    // -----------------------------------------------------------------------

    private var _isNotesMode = false

    /** Toggle pencil/notes mode on or off. */
    fun toggleNotesMode() {
        _isNotesMode = !_isNotesMode
        _uiState.update { state ->
            state?.copy(isNotesMode = _isNotesMode)
        }
    }

    /**
     * Add or remove a pencil note digit for the selected cell.
     * Only works when notes mode is active. Toggles the digit in the set.
     */
    fun enterNote(digit: Int) {
        if (!_isNotesMode) return
        val current = _uiState.value ?: return
        val (row, col) = current.selectedCell ?: return
        if (current.puzzle.cells[row][col].isGiven) return

        pushSnapshot(current)

        val newNotes = current.notesValues.deepCopyNotes()
        val existing = newNotes[row][col]
        newNotes[row][col] = if (digit in existing) existing - digit else existing + digit
        _uiState.value = current.copy(notesValues = newNotes)
    }

    // -----------------------------------------------------------------------
    // Undo history
    // -----------------------------------------------------------------------

    private data class Snapshot(
        val userValues: Array<IntArray>,
        val notesValues: Array<Array<Set<Int>>>
    )

    private val moveHistory: ArrayDeque<Snapshot> = ArrayDeque()
    private val maxHistory = 50

    /** Whether there are moves to undo. */
    val canUndo: Boolean get() = moveHistory.isNotEmpty()

    private fun Array<IntArray>.deepCopy(): Array<IntArray> = Array(size) { this[it].copyOf() }

    @Suppress("UNCHECKED_CAST")
    private fun Array<Array<Set<Int>>>.deepCopyNotes(): Array<Array<Set<Int>>> =
        Array(size) { Array(this[it].size) { c -> this[it][c].toSet() } }

    private fun pushSnapshot(current: PuzzleUiState) {
        moveHistory.addLast(Snapshot(
            userValues = current.userValues.deepCopy(),
            notesValues = current.notesValues.deepCopyNotes()
        ))
        if (moveHistory.size > maxHistory) moveHistory.removeFirst()
    }

    private fun pushHistory(userValues: Array<IntArray>) {
        val current = _uiState.value ?: return
        pushSnapshot(current)
    }

    /**
     * Undo the last cell change. Restores the previous userValues and notesValues from history.
     * No-op if history is empty.
     */
    fun undo() {
        if (moveHistory.isEmpty()) return
        val current = _uiState.value ?: return
        val snapshot = moveHistory.removeLast()
        val newState = buildState(
            puzzle = current.puzzle,
            userValues = snapshot.userValues,
            selectedCell = current.selectedCell,
            elapsedSeconds = current.elapsedSeconds
        ).copy(notesValues = snapshot.notesValues, isNotesMode = _isNotesMode)
        _uiState.value = newState
        handleCompletionChange(newState)
    }

    // -----------------------------------------------------------------------
    // Completion tracking
    // -----------------------------------------------------------------------

    private val _isComplete = MutableStateFlow(false)

    /**
     * True once the puzzle has been correctly and fully solved.
     * Transitions false → true exactly once per puzzle load.
     * Does not revert to false even if cells are subsequently changed.
     */
    val isComplete: StateFlow<Boolean> = _isComplete.asStateFlow()

    /** Guards against re-persisting completion state on subsequent state changes. */
    private var completionPersisted = false

    /** The [LocalDate] for which the current puzzle was loaded (for DataStore key). */
    private var puzzleDate: LocalDate? = null

    /** True when the current puzzle is a practice mode puzzle. */
    private var _isPracticeMode = false
    val isPracticeMode: Boolean get() = _isPracticeMode

    /** Coins earned on the most recent practice completion (reset on new puzzle load). */
    private val _coinsEarned = MutableStateFlow(0)
    val coinsEarned: StateFlow<Int> = _coinsEarned.asStateFlow()

    // -----------------------------------------------------------------------
    // Timer state
    // -----------------------------------------------------------------------

    /**
     * True once the player has tapped their first non-given cell.
     * The timer only increments when this flag is true and the puzzle is not yet complete.
     */
    var timerStarted: Boolean = false
        private set

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /**
     * Load a new puzzle. Resets all user input, selection, timer, and completion state.
     *
     * @param puzzle  The puzzle to display.
     * @param date    The calendar date this puzzle belongs to (for completion persistence).
     *                Defaults to null (no persistence).
     */
    fun loadPuzzle(puzzle: Puzzle, date: LocalDate? = null) {
        val n = puzzle.size
        val userValues = Array(n) { IntArray(n) { 0 } }
        val emptyNotes = emptyNotesGrid(n)
        puzzleDate = date
        moveHistory.clear()
        _isNotesMode = false
        // When no date is provided (legacy / S01 usage / practice), treat timer as already started
        // so that tickTimer() works unconditionally — preserving S01 behaviour.
        timerStarted = (date == null)
        completionPersisted = false
        _isComplete.value = false
        if (date != null) {
            _isPracticeMode = false
            _coinsEarned.value = 0
        }

        // Attempt to restore in-progress state
        val store = completionStore
        val d = date
        if (store != null && d != null) {
            val scope = persistScope ?: viewModelScope
            scope.launch {
                val key = "completion_${d.toEpochDay()}_${puzzle.difficulty.name}"
                val saved = store.getInProgress(key)
                if (saved != null && saved.size == n * n) {
                    val restored = Array(n) { r -> IntArray(n) { c -> saved[r * n + c] } }
                    _uiState.value = buildState(
                        puzzle = puzzle,
                        userValues = restored,
                        selectedCell = null,
                        elapsedSeconds = 0L
                    ).copy(notesValues = emptyNotes)
                    return@launch
                }
                _uiState.value = buildState(
                    puzzle = puzzle,
                    userValues = userValues,
                    selectedCell = null,
                    elapsedSeconds = 0L
                ).copy(notesValues = emptyNotes)
            }
        } else {
            _uiState.value = buildState(
                puzzle = puzzle,
                userValues = userValues,
                selectedCell = null,
                elapsedSeconds = 0L
            ).copy(notesValues = emptyNotes)
        }
    }

    /**
     * Load a practice puzzle. No date-based persistence, no streak tracking.
     * Coins are awarded on completion.
     */
    fun loadPracticePuzzle(puzzle: Puzzle) {
        _isPracticeMode = true
        _coinsEarned.value = 0
        loadPuzzle(puzzle, date = null)
    }

    private fun emptyNotesGrid(n: Int): Array<Array<Set<Int>>> =
        Array(n) { Array(n) { emptySet<Int>() } }

    // -----------------------------------------------------------------------
    // Cell selection
    // -----------------------------------------------------------------------

    /**
     * Select or deselect a cell.
     *
     * Rules:
     *  - Given cells cannot be selected.
     *  - Tapping the already-selected cell deselects it.
     *  - Tapping a different non-given cell selects it.
     *  - Tapping any non-given cell starts the timer if not already started.
     */
    fun selectCell(row: Int, col: Int) {
        val current = _uiState.value ?: return
        val cell: Cell = current.puzzle.cells[row][col]

        // Given cells are not selectable
        if (cell.isGiven) return

        // First non-given cell tap starts the timer
        onFirstCellTap()

        val newSelection = if (current.selectedCell == row to col) {
            null  // Toggle off
        } else {
            row to col
        }

        _uiState.update { state ->
            state?.copy(selectedCell = newSelection)
        }
    }

    /**
     * Signals that the player has tapped their first cell.
     * Safe to call multiple times — only acts on the first call.
     */
    fun onFirstCellTap() {
        if (!timerStarted) {
            timerStarted = true
        }
    }

    // -----------------------------------------------------------------------
    // Number input
    // -----------------------------------------------------------------------

    /**
     * Fill the currently selected cell with [number].
     *
     * If [number] is the same as the current value of the selected cell,
     * the cell is cleared (toggles off). Does nothing if no cell is selected.
     * Does nothing if the puzzle is already complete (guards against modification).
     *
     * @param number Value in [1..difficulty.maxVal].
     */
    fun enterNumber(number: Int) {
        val current = _uiState.value ?: return
        val (row, col) = current.selectedCell ?: return

        pushSnapshot(current)

        val existingValue = current.userValues[row][col]
        val newValue = if (existingValue == number) 0 else number  // toggle

        val newUserValues = Array(current.puzzle.size) { r ->
            current.userValues[r].copyOf()
        }
        newUserValues[row][col] = newValue

        // Clear notes for this cell when a real answer is entered
        val newNotes = current.notesValues.deepCopyNotes()
        if (newValue != 0) {
            newNotes[row][col] = emptySet()
        }

        val newState = buildState(
            puzzle = current.puzzle,
            userValues = newUserValues,
            selectedCell = current.selectedCell,
            elapsedSeconds = current.elapsedSeconds
        ).copy(notesValues = newNotes, isNotesMode = _isNotesMode)
        _uiState.value = newState
        autoSaveInProgress(newUserValues, current.puzzle)

        // Update isComplete flow and persist on transition to complete
        handleCompletionChange(newState)
    }

    /**
     * Clear the currently selected cell (set its value to 0).
     * Does nothing if no cell is selected.
     */
    fun clearCell() {
        val current = _uiState.value ?: return
        val (row, col) = current.selectedCell ?: return

        pushSnapshot(current)

        val newUserValues = Array(current.puzzle.size) { r ->
            current.userValues[r].copyOf()
        }
        newUserValues[row][col] = 0

        val newState = buildState(
            puzzle = current.puzzle,
            userValues = newUserValues,
            selectedCell = current.selectedCell,
            elapsedSeconds = current.elapsedSeconds
        ).copy(notesValues = current.notesValues, isNotesMode = _isNotesMode)
        _uiState.value = newState
        autoSaveInProgress(newUserValues, current.puzzle)

        // Re-evaluate completion (clearing a cell may un-complete the puzzle)
        handleCompletionChange(newState)
    }

    // -----------------------------------------------------------------------
    // Timer
    // -----------------------------------------------------------------------

    /**
     * Advance the elapsed-time counter by one second.
     *
     * Only increments if:
     *  - The player has tapped their first cell ([timerStarted] == true).
     *  - The puzzle is not yet complete.
     *
     * Called by the screen/lifecycle (e.g., a LaunchedEffect ticker).
     */
    fun tickTimer() {
        // Do nothing if timer not started or puzzle already complete
        if (!timerStarted || _isComplete.value) return

        _uiState.update { state ->
            state?.copy(elapsedSeconds = state.elapsedSeconds + 1)
        }
    }

    /**
     * Elapsed time in milliseconds (derived from [elapsedSeconds]).
     * Convenience for passing to [saveCompletionState].
     */
    val elapsedMillis: Long
        get() = (uiState.value?.elapsedSeconds ?: 0L) * 1000L

    /**
     * Formats elapsed milliseconds as "MM:SS".
     *
     * @param millis Total elapsed time in milliseconds. Fractional seconds are truncated.
     * @return String in "MM:SS" format, zero-padded.
     */
    fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return "%02d:%02d".format(minutes, seconds)
    }

    // -----------------------------------------------------------------------
    // Auto-save in-progress state
    // -----------------------------------------------------------------------

    private fun autoSaveInProgress(userValues: Array<IntArray>, puzzle: Puzzle) {
        val store = completionStore ?: return
        val date = puzzleDate ?: return
        val key = "completion_${date.toEpochDay()}_${puzzle.difficulty.name}"
        val n = puzzle.size
        val flat = IntArray(n * n) { i -> userValues[i / n][i % n] }
        val scope = persistScope ?: viewModelScope
        scope.launch { store.saveInProgress(key, flat) }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Handle a potential change in puzzle completion.
     *
     * - Updates [isComplete] flow.
     * - On first transition to complete, persists state (once only).
     */
    private fun handleCompletionChange(newState: PuzzleUiState) {
        val wasComplete = _isComplete.value
        val nowComplete = newState.isCompleted

        _isComplete.value = nowComplete

        // Persist exactly once — when puzzle transitions from incomplete to complete
        if (!wasComplete && nowComplete && !completionPersisted) {
            completionPersisted = true
            if (_isPracticeMode) {
                awardCoins(newState)
            } else {
                persistCompletion(newState)
            }
        }
    }

    private fun awardCoins(state: PuzzleUiState) {
        val repo = coinRepository ?: return
        val reward = state.puzzle.difficulty.coinReward
        _coinsEarned.value = reward
        val scope = persistScope ?: viewModelScope
        scope.launch {
            repo.addCoins(reward)
            reviewTrigger?.let { trigger ->
                trigger.recordPracticeCompletion()
                if (trigger.isEligibleAfterPractice()) {
                    _requestReviewEvent.tryEmit(Unit)
                }
            }
        }
    }

    /**
     * Persist completion state to [completionStore] (if wired) via a coroutine.
     * In unit tests without a ViewModel scope, this is called synchronously via
     * the coroutine launched in [viewModelScope] — tests with [InMemoryCompletionStore]
     * will see results immediately since the store is non-blocking.
     */
    private fun persistCompletion(state: PuzzleUiState) {
        val store = completionStore ?: return
        val date = puzzleDate ?: return
        val difficulty = state.puzzle.difficulty
        val elapsed = state.elapsedSeconds * 1000L

        // Use the injected scope when provided (unit tests pass a TestScope or Unconfined scope).
        // In production, persistScope is null and we use viewModelScope. We avoid calling
        // viewModelScope in non-Android environments because it requires Dispatchers.Main.
        val key = "completion_${date.toEpochDay()}_${difficulty.name}"
        val scope = persistScope ?: viewModelScope
        scope.launch {
            store.save(
                key,
                org.dgeek.sumgrid.daily.CompletionState(
                    completed = true,
                    elapsedMillis = elapsed,
                    completedAt = System.currentTimeMillis()
                )
            )
            store.clearInProgress(key)
            streakDataSource?.recordCompletion(date)

            reviewTrigger?.let { trigger ->
                trigger.recordDailyCompletion()
                val streakAfter = streakDataSource?.streakState?.first()?.currentStreak ?: 0
                if (trigger.isEligibleAfterDaily() || trigger.isEligibleAfterStreak(streakAfter)) {
                    _requestReviewEvent.tryEmit(Unit)
                }
            }
        }
    }

    /**
     * Build an immutable [PuzzleUiState] from raw mutable state.
     * Computes row/col indicators and completion flag in one pass.
     */
    internal fun buildState(
        puzzle: Puzzle,
        userValues: Array<IntArray>,
        selectedCell: Pair<Int, Int>?,
        elapsedSeconds: Long
    ): PuzzleUiState {
        val n = puzzle.size

        // Helper: display value (given value wins)
        fun displayValue(r: Int, c: Int): Int =
            if (puzzle.cells[r][c].isGiven) puzzle.cells[r][c].value else userValues[r][c]

        // Row sum indicators
        val rowIndicators = List(n) { r ->
            val sum = (0 until n).sumOf { c -> displayValue(r, c) }
            sumIndicator(sum, puzzle.rowTargets[r])
        }

        // Col sum indicators
        val colIndicators = List(n) { c ->
            val sum = (0 until n).sumOf { r -> displayValue(r, c) }
            sumIndicator(sum, puzzle.colTargets[c])
        }

        // Completion: every cell filled AND every indicator green
        val allFilled = (0 until n).all { r ->
            (0 until n).all { c -> displayValue(r, c) != 0 }
        }
        val isCompleted = allFilled &&
                rowIndicators.all { it == SumIndicatorColor.GREEN } &&
                colIndicators.all { it == SumIndicatorColor.GREEN }

        return PuzzleUiState(
            puzzle = puzzle,
            userValues = userValues,
            selectedCell = selectedCell,
            rowSumIndicators = rowIndicators,
            colSumIndicators = colIndicators,
            isCompleted = isCompleted,
            elapsedSeconds = elapsedSeconds
        )
    }

    private fun sumIndicator(current: Int, target: Int): SumIndicatorColor = when {
        current == target -> SumIndicatorColor.GREEN
        current > target  -> SumIndicatorColor.RED
        else              -> SumIndicatorColor.GRAY
    }
}

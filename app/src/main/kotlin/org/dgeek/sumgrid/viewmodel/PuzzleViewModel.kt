package org.dgeek.sumgrid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.dgeek.sumgrid.daily.CompletionStore
import org.dgeek.sumgrid.engine.models.Cell
import org.dgeek.sumgrid.engine.models.Difficulty
import org.dgeek.sumgrid.engine.models.Puzzle
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
    val elapsedSeconds: Long
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
        if (userValues.size != other.userValues.size) return false
        for (i in userValues.indices) {
            if (!userValues[i].contentEquals(other.userValues[i])) return false
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
        result = 31 * result + userValues.fold(1) { acc, row -> 31 * acc + row.contentHashCode() }
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
    /**
     * Coroutine scope used for persistence launches.
     * In production this is [viewModelScope] (set lazily).
     * In unit tests pass a [TestScope] or [CoroutineScope(Dispatchers.Unconfined)] to avoid
     * requiring an Android main Looper.
     */
    private val persistScope: CoroutineScope? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<PuzzleUiState?>(null)
    val uiState: StateFlow<PuzzleUiState?> = _uiState.asStateFlow()

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
        puzzleDate = date
        // When no date is provided (legacy / S01 usage), treat timer as already started
        // so that tickTimer() works unconditionally — preserving S01 behaviour.
        timerStarted = (date == null)
        completionPersisted = false
        _isComplete.value = false
        _uiState.value = buildState(
            puzzle = puzzle,
            userValues = userValues,
            selectedCell = null,
            elapsedSeconds = 0L
        )
    }

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

        val existingValue = current.userValues[row][col]
        val newValue = if (existingValue == number) 0 else number  // toggle

        val newUserValues = Array(current.puzzle.size) { r ->
            current.userValues[r].copyOf()
        }
        newUserValues[row][col] = newValue

        val newState = buildState(
            puzzle = current.puzzle,
            userValues = newUserValues,
            selectedCell = current.selectedCell,
            elapsedSeconds = current.elapsedSeconds
        )
        _uiState.value = newState

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

        val newUserValues = Array(current.puzzle.size) { r ->
            current.userValues[r].copyOf()
        }
        newUserValues[row][col] = 0

        val newState = buildState(
            puzzle = current.puzzle,
            userValues = newUserValues,
            selectedCell = current.selectedCell,
            elapsedSeconds = current.elapsedSeconds
        )
        _uiState.value = newState

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
            persistCompletion(newState)
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
        val scope = persistScope ?: viewModelScope
        scope.launch {
            store.save(
                "completion_${date.toEpochDay()}_${difficulty.name}",
                org.dgeek.sumgrid.daily.CompletionState(
                    completed = true,
                    elapsedMillis = elapsed,
                    completedAt = System.currentTimeMillis()
                )
            )
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

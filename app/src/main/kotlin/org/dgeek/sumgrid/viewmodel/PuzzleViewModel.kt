package org.dgeek.sumgrid.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.dgeek.sumgrid.engine.models.Cell
import org.dgeek.sumgrid.engine.models.Difficulty
import org.dgeek.sumgrid.engine.models.Puzzle

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
     * Size = puzzle.size × puzzle.size.
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
    /** Elapsed seconds since the puzzle was loaded. */
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
 */
class PuzzleViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<PuzzleUiState?>(null)
    val uiState: StateFlow<PuzzleUiState?> = _uiState.asStateFlow()

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /** Load a new puzzle. Resets all user input and the selection. */
    fun loadPuzzle(puzzle: Puzzle) {
        val n = puzzle.size
        val userValues = Array(n) { IntArray(n) { 0 } }
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
     */
    fun selectCell(row: Int, col: Int) {
        val current = _uiState.value ?: return
        val cell: Cell = current.puzzle.cells[row][col]

        // Given cells are not selectable
        if (cell.isGiven) return

        val newSelection = if (current.selectedCell == row to col) {
            null  // Toggle off
        } else {
            row to col
        }

        _uiState.update { state ->
            state?.copy(selectedCell = newSelection)
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

        _uiState.value = buildState(
            puzzle = current.puzzle,
            userValues = newUserValues,
            selectedCell = current.selectedCell,
            elapsedSeconds = current.elapsedSeconds
        )
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

        _uiState.value = buildState(
            puzzle = current.puzzle,
            userValues = newUserValues,
            selectedCell = current.selectedCell,
            elapsedSeconds = current.elapsedSeconds
        )
    }

    // -----------------------------------------------------------------------
    // Timer (driven by the screen/lifecycle, not by a coroutine here)
    // -----------------------------------------------------------------------

    /** Update the elapsed-time counter. Called by the screen every second. */
    fun tickTimer() {
        _uiState.update { state ->
            state?.copy(elapsedSeconds = state.elapsedSeconds + 1)
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

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

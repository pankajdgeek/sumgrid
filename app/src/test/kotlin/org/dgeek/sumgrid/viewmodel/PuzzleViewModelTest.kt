package org.dgeek.sumgrid.viewmodel

import org.dgeek.sumgrid.engine.models.Cell
import org.dgeek.sumgrid.engine.models.Difficulty
import org.dgeek.sumgrid.engine.models.Puzzle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [PuzzleViewModel].
 *
 * These run on the JVM (no Android / Robolectric needed) because
 * PuzzleViewModel contains only pure-Kotlin logic; the only Android
 * artefact it extends is [androidx.lifecycle.ViewModel] whose constructor
 * requires no Android context.
 *
 * Test puzzle layout (3x3 BEGINNER, seed=0):
 *
 *   cells (G = given, E = empty):
 *     [ G(2)  G(3)  E(0) ]   row target = 9
 *     [ E(0)  G(4)  G(1) ]   row target = 8
 *     [ G(5)  E(0)  G(2) ]   row target = 10
 *
 *   col targets = [10, 7, 10]  (sum of each column including solution values)
 *
 *   Solution (the missing values):
 *     [0][2] = 4   (row 0 needs 9 - 2 - 3 = 4)
 *     [1][0] = 3   (row 1 needs 8 - 4 - 1 = 3)
 *     [2][1] = 3   (row 2 needs 10 - 5 - 2 = 3)
 *
 *   Verify cols with solution:
 *     col 0: 2+3+5 = 10 ✓   col 1: 3+4+3 = 10? No — let me recalculate.
 *
 * Actual consistent puzzle used in tests (computed manually):
 *
 *   given:
 *     (0,0)=1  (0,1)=2  (0,2)=given(0) — actually let's use a simpler layout.
 *
 * Simple, hand-verified 3x3 puzzle:
 *
 *   Grid (full solution):
 *     [ 1  2  3 ]   row target = 6
 *     [ 4  1  2 ]   row target = 7
 *     [ 2  3  1 ]   row target = 6
 *
 *   col targets = [7, 6, 6]
 *
 *   Given pattern (5 given, 4 empty):
 *     (0,0)=1 given    (0,1)=2 given    (0,2)=0 empty  → user must enter 3
 *     (1,0)=0 empty    (1,1)=1 given    (1,2)=0 empty  → user must enter 4 and 2
 *     (2,0)=2 given    (2,1)=0 empty    (2,2)=1 given  → user must enter 3
 */
class PuzzleViewModelTest {

    private lateinit var vm: PuzzleViewModel
    private lateinit var puzzle: Puzzle

    /**
     * Hand-crafted 3x3 puzzle:
     *
     *   Full solution:   Given mask:
     *    1 2 3            G G E
     *    4 1 2            E G E
     *    2 3 1            G E G
     *
     *   rowTargets = [6, 7, 6]
     *   colTargets = [7, 6, 6]
     *   difficulty = BEGINNER (maxVal=5)
     */
    private fun makePuzzle(): Puzzle {
        val cells = arrayOf(
            arrayOf(Cell(1, isGiven = true),  Cell(2, isGiven = true),  Cell(0, isGiven = false)),
            arrayOf(Cell(0, isGiven = false), Cell(1, isGiven = true),  Cell(0, isGiven = false)),
            arrayOf(Cell(2, isGiven = true),  Cell(0, isGiven = false), Cell(1, isGiven = true))
        )
        return Puzzle(
            size = 3,
            cells = cells,
            rowTargets = intArrayOf(6, 7, 6),
            colTargets  = intArrayOf(7, 6, 6),
            difficulty = Difficulty.BEGINNER,
            seed = 0L
        )
    }

    @Before
    fun setup() {
        vm = PuzzleViewModel()
        puzzle = makePuzzle()
        vm.loadPuzzle(puzzle)
    }

    // -----------------------------------------------------------------------
    // loadPuzzle
    // -----------------------------------------------------------------------

    @Test
    fun loadPuzzle_stateIsNotNull() {
        assertNotNull(vm.uiState.value)
    }

    @Test
    fun loadPuzzle_noSelectionInitially() {
        assertNull(vm.uiState.value?.selectedCell)
    }

    @Test
    fun loadPuzzle_userValuesAllZero() {
        val state = vm.uiState.value!!
        for (r in 0 until puzzle.size) {
            for (c in 0 until puzzle.size) {
                if (!puzzle.cells[r][c].isGiven) {
                    assertEquals(0, state.userValues[r][c])
                }
            }
        }
    }

    @Test
    fun loadPuzzle_elapsedSecondsZero() {
        assertEquals(0L, vm.uiState.value?.elapsedSeconds)
    }

    @Test
    fun loadPuzzle_notCompleted() {
        assertFalse(vm.uiState.value?.isCompleted ?: true)
    }

    // -----------------------------------------------------------------------
    // selectCell — only non-given cells are selectable
    // -----------------------------------------------------------------------

    @Test
    fun selectCell_nonGivenCell_becomesSelected() {
        vm.selectCell(0, 2)  // (0,2) is empty/non-given
        assertEquals(0 to 2, vm.uiState.value?.selectedCell)
    }

    @Test
    fun selectCell_givenCell_remainsUnselected() {
        vm.selectCell(0, 0)  // (0,0) is given
        assertNull(vm.uiState.value?.selectedCell)
    }

    @Test
    fun selectCell_tapSameNonGivenCell_deselects() {
        vm.selectCell(0, 2)
        vm.selectCell(0, 2)  // tap again
        assertNull(vm.uiState.value?.selectedCell)
    }

    @Test
    fun selectCell_tapDifferentNonGivenCell_changesSelection() {
        vm.selectCell(0, 2)  // select (0,2)
        vm.selectCell(1, 0)  // select (1,0)
        assertEquals(1 to 0, vm.uiState.value?.selectedCell)
    }

    @Test
    fun selectCell_multipleGivenCells_noneBecomesSelected() {
        // Tap every given cell — none should be selectable
        val givenPositions = listOf(0 to 0, 0 to 1, 1 to 1, 2 to 0, 2 to 2)
        for ((r, c) in givenPositions) {
            vm.selectCell(r, c)
            assertNull(
                "Given cell ($r,$c) should not be selectable",
                vm.uiState.value?.selectedCell
            )
        }
    }

    // -----------------------------------------------------------------------
    // enterNumber — fills selected cell
    // -----------------------------------------------------------------------

    @Test
    fun enterNumber_withSelection_fillsCell() {
        vm.selectCell(0, 2)
        vm.enterNumber(3)
        assertEquals(3, vm.uiState.value!!.userValues[0][2])
    }

    @Test
    fun enterNumber_withoutSelection_doesNothing() {
        assertNull(vm.uiState.value?.selectedCell)
        vm.enterNumber(3)
        // All user values remain 0
        val state = vm.uiState.value!!
        for (r in 0 until puzzle.size) {
            for (c in 0 until puzzle.size) {
                if (!puzzle.cells[r][c].isGiven) {
                    assertEquals("Cell ($r,$c) should still be 0", 0, state.userValues[r][c])
                }
            }
        }
    }

    @Test
    fun enterNumber_sameNumberTwice_clearsCell() {
        vm.selectCell(0, 2)
        vm.enterNumber(3)
        assertEquals(3, vm.uiState.value!!.userValues[0][2])
        vm.enterNumber(3)  // toggle off
        assertEquals(0, vm.uiState.value!!.userValues[0][2])
    }

    @Test
    fun enterNumber_differentNumber_overwritesCell() {
        vm.selectCell(0, 2)
        vm.enterNumber(3)
        vm.enterNumber(5)  // overwrite with 5
        assertEquals(5, vm.uiState.value!!.userValues[0][2])
    }

    @Test
    fun enterNumber_doesNotChangeGivenCells() {
        vm.selectCell(0, 2)
        vm.enterNumber(3)
        // Given cells must still have original values
        assertEquals(1, vm.uiState.value!!.puzzle.cells[0][0].value)
        assertEquals(2, vm.uiState.value!!.puzzle.cells[0][1].value)
    }

    // -----------------------------------------------------------------------
    // clearCell
    // -----------------------------------------------------------------------

    @Test
    fun clearCell_withFilledCell_setsZero() {
        vm.selectCell(0, 2)
        vm.enterNumber(3)
        vm.clearCell()
        assertEquals(0, vm.uiState.value!!.userValues[0][2])
    }

    @Test
    fun clearCell_withEmptyCell_remainsZero() {
        vm.selectCell(0, 2)
        vm.clearCell()  // already empty
        assertEquals(0, vm.uiState.value!!.userValues[0][2])
    }

    @Test
    fun clearCell_withoutSelection_doesNothing() {
        vm.selectCell(0, 2)
        vm.enterNumber(3)
        vm.selectCell(0, 2)  // deselect
        assertNull(vm.uiState.value?.selectedCell)
        vm.clearCell()
        // Value should remain 3 (no selection)
        assertEquals(3, vm.uiState.value!!.userValues[0][2])
    }

    // -----------------------------------------------------------------------
    // Row/column sum computation
    // -----------------------------------------------------------------------

    @Test
    fun sumIndicators_initialState_rowsAreGray() {
        // All rows are incomplete initially
        val state = vm.uiState.value!!
        for (indicator in state.rowSumIndicators) {
            assertEquals(SumIndicatorColor.GRAY, indicator)
        }
    }

    @Test
    fun sumIndicators_initialState_colsAreGray() {
        val state = vm.uiState.value!!
        for (indicator in state.colSumIndicators) {
            assertEquals(SumIndicatorColor.GRAY, indicator)
        }
    }

    @Test
    fun sumIndicators_row0_turnsGreenWhenCorrect() {
        // Row 0: given(1) + given(2) + empty → target 6, need empty = 3
        vm.selectCell(0, 2)
        vm.enterNumber(3)
        assertEquals(SumIndicatorColor.GREEN, vm.uiState.value!!.rowSumIndicators[0])
    }

    @Test
    fun sumIndicators_row0_turnsRedWhenOver() {
        // Row 0 target = 6; given = 1+2=3; enter 4 → sum = 7 > 6
        vm.selectCell(0, 2)
        vm.enterNumber(4)
        assertEquals(SumIndicatorColor.RED, vm.uiState.value!!.rowSumIndicators[0])
    }

    @Test
    fun sumIndicators_row0_remainsGrayWhenUnder() {
        // Enter 2 → sum = 1+2+2 = 5 < 6
        vm.selectCell(0, 2)
        vm.enterNumber(2)
        assertEquals(SumIndicatorColor.GRAY, vm.uiState.value!!.rowSumIndicators[0])
    }

    @Test
    fun sumIndicators_colUpdateAfterCellChange() {
        // Col 2: given(3) at (0,2) would be target 6; but (0,2) is empty in our puzzle
        // Col 2: cells are (0,2)=empty, (1,2)=empty, (2,2)=given(1), target=6
        // Enter 2 at (0,2) and 3 at (1,2) → col sum = 2+3+1 = 6 → GREEN
        vm.selectCell(0, 2)
        vm.enterNumber(2)
        vm.selectCell(1, 2)
        vm.enterNumber(3)
        assertEquals(SumIndicatorColor.GREEN, vm.uiState.value!!.colSumIndicators[2])
    }

    @Test
    fun sumIndicators_updateImmediatelyOnEachChange() {
        // Verify the indicator updates after every enterNumber call (no batching)
        vm.selectCell(0, 2)

        vm.enterNumber(1)
        assertEquals(SumIndicatorColor.GRAY, vm.uiState.value!!.rowSumIndicators[0])

        vm.enterNumber(3)
        assertEquals(SumIndicatorColor.GREEN, vm.uiState.value!!.rowSumIndicators[0])

        vm.enterNumber(5)
        assertEquals(SumIndicatorColor.RED, vm.uiState.value!!.rowSumIndicators[0])
    }

    // -----------------------------------------------------------------------
    // Completion detection
    // -----------------------------------------------------------------------

    @Test
    fun completion_notDetectedWhenIncomplete() {
        vm.selectCell(0, 2)
        vm.enterNumber(3)
        // Other empty cells still 0
        assertFalse(vm.uiState.value!!.isCompleted)
    }

    @Test
    fun completion_detectedWhenAllCorrect() {
        // Fill all empty cells with correct values:
        //   (0,2) = 3  → row0: 1+2+3=6 ✓
        //   (1,0) = 4  → row1: 4+1+2=7 ✓
        //   (1,2) = 2  → row1 second empty
        //   (2,1) = 3  → row2: 2+3+1=6 ✓
        // Col verification:
        //   col0: 1+4+2=7 ✓  col1: 2+1+3=6 ✓  col2: 3+2+1=6 ✓
        vm.selectCell(0, 2); vm.enterNumber(3)
        vm.selectCell(1, 0); vm.enterNumber(4)
        vm.selectCell(1, 2); vm.enterNumber(2)
        vm.selectCell(2, 1); vm.enterNumber(3)

        assertTrue(vm.uiState.value!!.isCompleted)
    }

    @Test
    fun completion_notDetectedWhenOneCellWrong() {
        vm.selectCell(0, 2); vm.enterNumber(3)
        vm.selectCell(1, 0); vm.enterNumber(4)
        vm.selectCell(1, 2); vm.enterNumber(2)
        vm.selectCell(2, 1); vm.enterNumber(2)  // wrong: should be 3

        assertFalse(vm.uiState.value!!.isCompleted)
    }

    @Test
    fun completion_resetAfterClearingCell() {
        // Complete the puzzle, then clear one cell — should no longer be complete
        vm.selectCell(0, 2); vm.enterNumber(3)
        vm.selectCell(1, 0); vm.enterNumber(4)
        vm.selectCell(1, 2); vm.enterNumber(2)
        vm.selectCell(2, 1); vm.enterNumber(3)
        assertTrue(vm.uiState.value!!.isCompleted)

        // Re-select (2,1) — after enterNumber it is still the selected cell,
        // so selectCell(2,1) would toggle it off. Select a different cell first,
        // then select (2,1) to ensure it is selected, then clear it.
        vm.selectCell(0, 2)       // select another non-given cell
        vm.selectCell(2, 1)       // select (2,1)
        vm.clearCell()            // clear it → value becomes 0
        assertFalse(vm.uiState.value!!.isCompleted)
    }

    // -----------------------------------------------------------------------
    // Timer
    // -----------------------------------------------------------------------

    @Test
    fun tickTimer_incrementsElapsedSeconds() {
        assertEquals(0L, vm.uiState.value?.elapsedSeconds)
        vm.tickTimer()
        assertEquals(1L, vm.uiState.value?.elapsedSeconds)
        vm.tickTimer()
        assertEquals(2L, vm.uiState.value?.elapsedSeconds)
    }

    @Test
    fun tickTimer_doesNotAffectPuzzleState() {
        vm.selectCell(0, 2)
        vm.enterNumber(3)
        vm.tickTimer()
        // Cell value should still be 3
        assertEquals(3, vm.uiState.value!!.userValues[0][2])
        assertEquals(0 to 2, vm.uiState.value?.selectedCell)
    }

    // -----------------------------------------------------------------------
    // displayValueAt helper
    // -----------------------------------------------------------------------

    @Test
    fun displayValueAt_givenCell_returnsGivenValue() {
        val state = vm.uiState.value!!
        assertEquals(1, state.displayValueAt(0, 0))  // given = 1
        assertEquals(2, state.displayValueAt(0, 1))  // given = 2
    }

    @Test
    fun displayValueAt_emptyCell_returnsZeroInitially() {
        val state = vm.uiState.value!!
        assertEquals(0, state.displayValueAt(0, 2))  // empty
    }

    @Test
    fun displayValueAt_emptyCell_returnsUserValueAfterEntry() {
        vm.selectCell(0, 2)
        vm.enterNumber(3)
        val state = vm.uiState.value!!
        assertEquals(3, state.displayValueAt(0, 2))
    }

    // -----------------------------------------------------------------------
    // loadPuzzle — reload resets state
    // -----------------------------------------------------------------------

    @Test
    fun loadPuzzle_reload_resetsUserInput() {
        vm.selectCell(0, 2)
        vm.enterNumber(3)
        vm.loadPuzzle(puzzle)  // reload same puzzle
        assertEquals(0, vm.uiState.value!!.userValues[0][2])
    }

    @Test
    fun loadPuzzle_reload_resetsSelection() {
        vm.selectCell(0, 2)
        vm.loadPuzzle(puzzle)
        assertNull(vm.uiState.value?.selectedCell)
    }

    @Test
    fun loadPuzzle_reload_resetsTimer() {
        vm.tickTimer(); vm.tickTimer()
        vm.loadPuzzle(puzzle)
        assertEquals(0L, vm.uiState.value?.elapsedSeconds)
    }
}

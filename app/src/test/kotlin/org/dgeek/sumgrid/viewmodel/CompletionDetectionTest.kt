package org.dgeek.sumgrid.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.dgeek.sumgrid.daily.InMemoryCompletionStore
import org.dgeek.sumgrid.engine.models.Cell
import org.dgeek.sumgrid.engine.models.Difficulty
import org.dgeek.sumgrid.engine.models.Puzzle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for enhanced completion detection in [PuzzleViewModel].
 *
 * Hand-crafted 3x3 puzzle (BEGINNER):
 *
 *   Full solution:   Given mask:
 *    1 2 3            G G E
 *    4 1 2            E G E
 *    2 3 1            G E G
 *
 *   rowTargets = [6, 7, 6]
 *   colTargets = [7, 6, 6]
 *
 *   Empty cells: (0,2)=3, (1,0)=4, (1,2)=2, (2,1)=3
 */
class CompletionDetectionTest {

    private lateinit var vm: PuzzleViewModel
    private lateinit var puzzle: Puzzle
    private lateinit var store: InMemoryCompletionStore

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
            colTargets = intArrayOf(7, 6, 6),
            difficulty = Difficulty.BEGINNER,
            seed = 0L
        )
    }

    private fun solvePuzzle() {
        vm.selectCell(0, 2); vm.enterNumber(3)
        vm.selectCell(1, 0); vm.enterNumber(4)
        vm.selectCell(1, 2); vm.enterNumber(2)
        vm.selectCell(2, 1); vm.enterNumber(3)
    }

    @Before
    fun setup() {
        store = InMemoryCompletionStore()
        vm = PuzzleViewModel(
            completionStore = store,
            persistScope = CoroutineScope(Dispatchers.Unconfined)
        )
        puzzle = makePuzzle()
        vm.loadPuzzle(puzzle, LocalDate.of(2026, 3, 14))
    }

    // -----------------------------------------------------------------------
    // Correct completion detection
    // -----------------------------------------------------------------------

    @Test
    fun isComplete_falseInitially() {
        assertFalse(vm.isComplete.value)
    }

    @Test
    fun isComplete_trueWhenAllCellsCorrect() = runTest {
        solvePuzzle()
        assertTrue(vm.isComplete.value)
    }

    @Test
    fun isComplete_falseWhenPartiallyFilled() {
        vm.selectCell(0, 2); vm.enterNumber(3)
        assertFalse(vm.isComplete.value)
    }

    @Test
    fun isComplete_falseWhenOneCellWrong() {
        vm.selectCell(0, 2); vm.enterNumber(3)
        vm.selectCell(1, 0); vm.enterNumber(4)
        vm.selectCell(1, 2); vm.enterNumber(2)
        vm.selectCell(2, 1); vm.enterNumber(2)  // wrong — should be 3
        assertFalse(vm.isComplete.value)
    }

    // -----------------------------------------------------------------------
    // Persistence on completion
    // -----------------------------------------------------------------------

    @Test
    fun onCompletion_persistsCompletionState() = runTest {
        solvePuzzle()
        assertTrue(vm.isComplete.value)
        val saved = store.get("completion_${LocalDate.of(2026, 3, 14).toEpochDay()}_BEGINNER")
        assertNotNull(saved)
        assertTrue(saved!!.completed)
    }

    @Test
    fun onCompletion_elapsedMillisPersisted() = runTest {
        // Simulate some timer ticks before solving
        vm.onFirstCellTap()
        repeat(5) { vm.tickTimer() }  // 5 seconds
        solvePuzzle()
        val saved = store.get("completion_${LocalDate.of(2026, 3, 14).toEpochDay()}_BEGINNER")
        assertNotNull(saved)
        assertTrue(saved!!.elapsedMillis >= 5000L)
    }

    // -----------------------------------------------------------------------
    // Guard against double-fire
    // -----------------------------------------------------------------------

    @Test
    fun isComplete_doesNotReFireWhenAlreadyComplete() = runTest {
        solvePuzzle()
        assertTrue(vm.isComplete.value)
        assertEquals(1, store.saveCount)

        // Clear cell (2,1) — puzzle becomes incomplete
        // After solvePuzzle(), cell (2,1) is still selected; clearCell() empties it.
        vm.clearCell()
        assertFalse(vm.isComplete.value)

        // Re-solve by re-entering 3 into (2,1)
        vm.enterNumber(3)
        assertTrue(vm.isComplete.value)

        // The guard must prevent a second persistence call even though the puzzle
        // transitions from incomplete back to complete.
        assertEquals(1, store.saveCount)
    }

    @Test
    fun uiState_isCompleted_matchesIsCompleteFlow() = runTest {
        solvePuzzle()
        val fromFlow = vm.isComplete.value
        val fromState = vm.uiState.value?.isCompleted
        assertEquals(fromFlow, fromState)
    }
}

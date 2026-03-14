package org.dgeek.sumgrid.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.dgeek.sumgrid.daily.InMemoryCompletionStore
import org.dgeek.sumgrid.engine.models.Cell
import org.dgeek.sumgrid.engine.models.Difficulty
import org.dgeek.sumgrid.engine.models.Puzzle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for auto-save / in-progress state persistence in PuzzleViewModel.
 */
class PuzzleAutoSaveTest {

    private lateinit var store: InMemoryCompletionStore
    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private lateinit var vm: PuzzleViewModel
    private lateinit var puzzle: Puzzle
    private val date = LocalDate.of(2025, 6, 15)
    private val key get() = "completion_${date.toEpochDay()}_${puzzle.difficulty.name}"

    @Before
    fun setUp() {
        store = InMemoryCompletionStore()
        vm = PuzzleViewModel(
            completionStore = store,
            persistScope = scope
        )
        puzzle = Puzzle(
            size = 3,
            cells = arrayOf(
                arrayOf(Cell(1, true), Cell(0, false), Cell(0, false)),
                arrayOf(Cell(0, false), Cell(0, false), Cell(0, false)),
                arrayOf(Cell(0, false), Cell(0, false), Cell(0, false))
            ),
            rowTargets = intArrayOf(6, 6, 6),
            colTargets = intArrayOf(6, 6, 6),
            difficulty = Difficulty.BEGINNER,
            seed = 99L
        )
    }

    @Test
    fun autoSave_writesInProgressState() {
        vm.loadPuzzle(puzzle, date)

        vm.selectCell(0, 1)
        vm.enterNumber(2)

        val saved = runBlocking { store.getInProgress(key) }
        assertNotNull("In-progress state should be saved after entering a number", saved)
        assertEquals(9, saved!!.size)
        assertEquals(2, saved[1]) // userValues[0][1] = 2
    }

    @Test
    fun autoSave_restoresMidPuzzleState() {
        val savedValues = intArrayOf(0, 3, 0, 0, 5, 0, 0, 0, 0)
        runBlocking { store.saveInProgress(key, savedValues) }

        vm.loadPuzzle(puzzle, date)

        val state = vm.uiState.value
        assertNotNull("UI state should be loaded", state)
        assertEquals(3, state!!.userValues[0][1])
        assertEquals(5, state.userValues[1][1])
    }

    @Test
    fun autoSave_clearsOnCompletion() {
        vm.loadPuzzle(puzzle, date)

        // Fill grid: all row/col targets are 6, given (0,0)=1
        val moves = listOf(
            Triple(0, 1, 2), Triple(0, 2, 3),
            Triple(1, 0, 2), Triple(1, 1, 3), Triple(1, 2, 1),
            Triple(2, 0, 3), Triple(2, 1, 1), Triple(2, 2, 2)
        )
        for ((r, c, v) in moves) {
            vm.selectCell(r, c)
            vm.enterNumber(v)
        }

        assertEquals(true, vm.uiState.value?.isCompleted)

        val inProgress = runBlocking { store.getInProgress(key) }
        assertNull("In-progress state should be cleared on completion", inProgress)

        val completion = runBlocking { store.get(key) }
        assertNotNull("Completion state should be persisted", completion)
        assertEquals(true, completion!!.completed)
    }
}

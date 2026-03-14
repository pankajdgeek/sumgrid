package org.dgeek.sumgrid.viewmodel

import org.dgeek.sumgrid.engine.models.Cell
import org.dgeek.sumgrid.engine.models.Difficulty
import org.dgeek.sumgrid.engine.models.Puzzle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for undo functionality in PuzzleViewModel.
 */
class PuzzleUndoTest {

    private lateinit var vm: PuzzleViewModel
    private lateinit var puzzle: Puzzle

    @Before
    fun setUp() {
        vm = PuzzleViewModel()
        // 3x3 puzzle with one given cell at (0,0)=1
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
            seed = 12345L
        )
        vm.loadPuzzle(puzzle)
    }

    @Test
    fun canUndo_isFalseInitially() {
        assertFalse(vm.canUndo)
    }

    @Test
    fun canUndo_isTrueAfterMove() {
        vm.selectCell(0, 1)
        vm.enterNumber(2)
        assertTrue(vm.canUndo)
    }

    @Test
    fun undo_onEmptyHistory_isNoOp() {
        val before = vm.uiState.value
        vm.undo()
        assertEquals(before, vm.uiState.value)
    }

    @Test
    fun undo_afterEnterNumber_restoresPreviousValue() {
        vm.selectCell(0, 1)
        vm.enterNumber(3)
        assertEquals(3, vm.uiState.value!!.userValues[0][1])

        vm.undo()
        assertEquals(0, vm.uiState.value!!.userValues[0][1])
    }

    @Test
    fun undo_afterClearCell_restoresNumber() {
        vm.selectCell(0, 1)
        vm.enterNumber(3)
        vm.clearCell()
        assertEquals(0, vm.uiState.value!!.userValues[0][1])

        vm.undo()
        assertEquals(3, vm.uiState.value!!.userValues[0][1])
    }

    @Test
    fun undo_doesNotCrossPuzzleBoundary() {
        vm.selectCell(0, 1)
        vm.enterNumber(3)
        assertTrue(vm.canUndo)

        // Loading a new puzzle clears history
        vm.loadPuzzle(puzzle)
        assertFalse(vm.canUndo)
        vm.undo() // no-op
        assertEquals(0, vm.uiState.value!!.userValues[0][1])
    }
}

package org.dgeek.sumgrid.viewmodel

import org.dgeek.sumgrid.engine.models.Cell
import org.dgeek.sumgrid.engine.models.Difficulty
import org.dgeek.sumgrid.engine.models.Puzzle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PuzzleNotesTest {

    private lateinit var vm: PuzzleViewModel
    private lateinit var puzzle: Puzzle

    @Before
    fun setUp() {
        vm = PuzzleViewModel()
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
            seed = 42L
        )
        vm.loadPuzzle(puzzle)
    }

    @Test
    fun notesMode_toggledOn_by_toggleNotesMode() {
        assertFalse(vm.uiState.value!!.isNotesMode)
        vm.toggleNotesMode()
        assertTrue(vm.uiState.value!!.isNotesMode)
    }

    @Test
    fun notesMode_toggledOff_by_secondToggle() {
        vm.toggleNotesMode()
        vm.toggleNotesMode()
        assertFalse(vm.uiState.value!!.isNotesMode)
    }

    @Test
    fun notesMode_enterNote_addsToCell() {
        vm.toggleNotesMode()
        vm.selectCell(0, 1)
        vm.enterNote(3)

        val notes = vm.uiState.value!!.notesValues[0][1]
        assertTrue("Note 3 should be in cell (0,1)", 3 in notes)
    }

    @Test
    fun notesMode_enterNote_multipleDigits() {
        vm.toggleNotesMode()
        vm.selectCell(0, 1)
        vm.enterNote(1)
        vm.enterNote(3)
        vm.enterNote(5)

        val notes = vm.uiState.value!!.notesValues[0][1]
        assertEquals(setOf(1, 3, 5), notes)
    }

    @Test
    fun notesMode_enterNote_sameDigitTwice_removesIt() {
        vm.toggleNotesMode()
        vm.selectCell(0, 1)
        vm.enterNote(3)
        vm.enterNote(3)

        val notes = vm.uiState.value!!.notesValues[0][1]
        assertFalse("Note 3 should be removed on second tap", 3 in notes)
    }

    @Test
    fun notesMode_enterNumber_clearsNotesForCell() {
        vm.toggleNotesMode()
        vm.selectCell(0, 1)
        vm.enterNote(1)
        vm.enterNote(3)

        // Switch to normal mode and enter a number
        vm.toggleNotesMode()
        vm.enterNumber(2)

        val notes = vm.uiState.value!!.notesValues[0][1]
        assertTrue("Notes should be cleared when a number is entered", notes.isEmpty())
        assertEquals(2, vm.uiState.value!!.userValues[0][1])
    }

    @Test
    fun notesMode_notesCleared_onLoadPuzzle() {
        vm.toggleNotesMode()
        vm.selectCell(0, 1)
        vm.enterNote(3)

        vm.loadPuzzle(puzzle)

        val notes = vm.uiState.value!!.notesValues[0][1]
        assertTrue("Notes should be cleared on puzzle load", notes.isEmpty())
        assertFalse("Notes mode should be off after puzzle load", vm.uiState.value!!.isNotesMode)
    }

    @Test
    fun undo_revertsNoteChange() {
        vm.toggleNotesMode()
        vm.selectCell(0, 1)
        vm.enterNote(3)
        assertEquals(setOf(3), vm.uiState.value!!.notesValues[0][1])

        vm.undo()
        assertTrue("Notes should be reverted by undo", vm.uiState.value!!.notesValues[0][1].isEmpty())
    }

    @Test
    fun enterNote_noOp_whenNotesModeOff() {
        vm.selectCell(0, 1)
        vm.enterNote(3) // notes mode is off
        assertTrue(vm.uiState.value!!.notesValues[0][1].isEmpty())
    }
}

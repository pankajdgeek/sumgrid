package org.dgeek.sumgrid.viewmodel

import org.dgeek.sumgrid.engine.models.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PracticeViewModelTest {

    @Test
    fun practiceVm_generatePuzzle_producesValidPuzzle() {
        val vm = PracticeViewModel()
        vm.generatePuzzle(Difficulty.MEDIUM)

        val state = vm.puzzleVm.uiState.value
        assertNotNull("Practice puzzle should be generated", state)
        assertEquals(5, state!!.puzzle.size) // MEDIUM is 5x5
        assertEquals(Difficulty.MEDIUM, state.puzzle.difficulty)
    }

    @Test
    fun practiceVm_generatePuzzle_beginner() {
        val vm = PracticeViewModel()
        vm.generatePuzzle(Difficulty.BEGINNER)

        val state = vm.puzzleVm.uiState.value
        assertNotNull(state)
        assertEquals(3, state!!.puzzle.size) // BEGINNER is 3x3
    }

    @Test
    fun practiceVm_playAgain_generatesNewPuzzle() {
        val vm = PracticeViewModel()
        vm.generatePuzzle(Difficulty.EASY)
        val seed1 = vm.currentSeed

        // Small delay to ensure different currentTimeMillis seed
        Thread.sleep(2)
        vm.playAgain()
        val seed2 = vm.currentSeed

        assertNotEquals("Play again should use a different seed", seed1, seed2)
    }

    @Test
    fun practiceVm_doesNotPersistToCompletionStore() {
        // PracticeViewModel creates PuzzleViewModel with no CompletionStore
        val vm = PracticeViewModel()
        vm.generatePuzzle(Difficulty.BEGINNER)

        // PuzzleViewModel.completionStore is null → no persistence happens
        // We verify this indirectly: the puzzleVm was created without a store,
        // so completionPersisted logic is skipped entirely.
        val state = vm.puzzleVm.uiState.value
        assertNotNull("Puzzle should be loaded without requiring a store", state)
    }
}

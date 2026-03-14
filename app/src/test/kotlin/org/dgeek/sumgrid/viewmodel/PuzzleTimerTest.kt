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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for the puzzle timer in [PuzzleViewModel].
 *
 * Timer rules:
 *  - Starts only when the first cell is tapped (onFirstCellTap / selectCell)
 *  - Stops on completion (no further increments after puzzle solved)
 *  - formatTime converts millis to "MM:SS"
 */
class PuzzleTimerTest {

    private lateinit var vm: PuzzleViewModel
    private lateinit var puzzle: Puzzle

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

    @Before
    fun setup() {
        vm = PuzzleViewModel(
            completionStore = InMemoryCompletionStore(),
            persistScope = CoroutineScope(Dispatchers.Unconfined)
        )
        puzzle = makePuzzle()
        vm.loadPuzzle(puzzle, LocalDate.of(2026, 3, 14))
    }

    // -----------------------------------------------------------------------
    // Timer starts on first cell tap
    // -----------------------------------------------------------------------

    @Test
    fun timer_notStartedOnLoad() {
        assertFalse(vm.timerStarted)
    }

    @Test
    fun timer_startsOnFirstCellTap() {
        vm.onFirstCellTap()
        assertTrue(vm.timerStarted)
    }

    @Test
    fun timer_startsWhenNonGivenCellSelected() {
        vm.selectCell(0, 2)  // non-given cell tap — should start timer
        assertTrue(vm.timerStarted)
    }

    @Test
    fun timer_doesNotStartWhenGivenCellTapped() {
        vm.selectCell(0, 0)  // given cell — must not start timer
        assertFalse(vm.timerStarted)
    }

    // -----------------------------------------------------------------------
    // tickTimer only increments when timer is running
    // -----------------------------------------------------------------------

    @Test
    fun tickTimer_beforeFirstTap_doesNotIncrement() {
        assertFalse(vm.timerStarted)
        vm.tickTimer()
        assertEquals(0L, vm.uiState.value?.elapsedSeconds)
    }

    @Test
    fun tickTimer_afterFirstTap_increments() {
        vm.onFirstCellTap()
        vm.tickTimer()
        assertEquals(1L, vm.uiState.value?.elapsedSeconds)
        vm.tickTimer()
        assertEquals(2L, vm.uiState.value?.elapsedSeconds)
    }

    // -----------------------------------------------------------------------
    // Timer stops on completion
    // -----------------------------------------------------------------------

    @Test
    fun tickTimer_afterCompletion_doesNotIncrement() = runTest {
        vm.onFirstCellTap()
        vm.tickTimer()
        vm.tickTimer()  // 2 seconds elapsed
        assertEquals(2L, vm.uiState.value?.elapsedSeconds)

        // Solve puzzle
        vm.selectCell(0, 2); vm.enterNumber(3)
        vm.selectCell(1, 0); vm.enterNumber(4)
        vm.selectCell(1, 2); vm.enterNumber(2)
        vm.selectCell(2, 1); vm.enterNumber(3)
        assertTrue(vm.isComplete.value)

        val elapsedAtCompletion = vm.uiState.value?.elapsedSeconds ?: -1L
        // Further ticks must not change elapsed
        vm.tickTimer()
        vm.tickTimer()
        assertEquals(elapsedAtCompletion, vm.uiState.value?.elapsedSeconds)
    }

    // -----------------------------------------------------------------------
    // formatTime
    // -----------------------------------------------------------------------

    @Test
    fun formatTime_zero_returnsZeroZero() {
        assertEquals("00:00", vm.formatTime(0L))
    }

    @Test
    fun formatTime_59seconds() {
        assertEquals("00:59", vm.formatTime(59_000L))
    }

    @Test
    fun formatTime_exactlyOneMinute() {
        assertEquals("01:00", vm.formatTime(60_000L))
    }

    @Test
    fun formatTime_oneMinuteSixSeconds() {
        assertEquals("01:06", vm.formatTime(66_000L))
    }

    @Test
    fun formatTime_tenMinutesTenSeconds() {
        assertEquals("10:10", vm.formatTime(610_000L))
    }

    @Test
    fun formatTime_ninetyNineMinutes() {
        assertEquals("99:59", vm.formatTime(5999_000L))
    }

    @Test
    fun formatTime_millisRoundedDownToSeconds() {
        // 1999ms = 1 second (floor)
        assertEquals("00:01", vm.formatTime(1999L))
    }

    // -----------------------------------------------------------------------
    // elapsedMillis exposure
    // -----------------------------------------------------------------------

    @Test
    fun elapsedMillis_matchesElapsedSeconds() {
        vm.onFirstCellTap()
        repeat(3) { vm.tickTimer() }
        val state = vm.uiState.value!!
        assertEquals(state.elapsedSeconds * 1000L, vm.elapsedMillis)
    }
}

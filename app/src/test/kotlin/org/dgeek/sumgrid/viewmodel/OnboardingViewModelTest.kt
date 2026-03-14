package org.dgeek.sumgrid.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.dgeek.sumgrid.onboarding.InMemoryOnboardingRepository
import org.dgeek.sumgrid.onboarding.OnboardingRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [OnboardingViewModel].
 *
 * Uses [InMemoryOnboardingRepository] — no DataStore or Android context required.
 * All coroutines run on [Dispatchers.Unconfined] to settle immediately without a
 * Main thread Looper.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private lateinit var repo: InMemoryOnboardingRepository
    private lateinit var vm: OnboardingViewModel

    @Before
    fun setUp() {
        repo = InMemoryOnboardingRepository()
        // Pass Dispatchers.Unconfined as ioScope so suspend calls complete inline.
        vm = OnboardingViewModel(
            onboardingRepository = repo,
            ioScope = kotlinx.coroutines.CoroutineScope(Dispatchers.Unconfined)
        )
    }

    // -----------------------------------------------------------------------
    // Initial state
    // -----------------------------------------------------------------------

    @Test
    fun `launchCount starts at 0`() {
        assertEquals(0, vm.launchCount.value)
    }

    @Test
    fun `isOnboardingComplete starts false`() {
        assertFalse(vm.isOnboardingComplete.value)
    }

    // -----------------------------------------------------------------------
    // incrementLaunchAndGetPuzzle
    // -----------------------------------------------------------------------

    @Test
    fun `first call returns puzzle with 1 empty cell`() = runTest {
        val puzzle = vm.incrementLaunchAndGetPuzzle()

        assertNotNull(puzzle)
        val emptyCells = puzzle!!.cells.sumOf { row -> row.count { it.isEmpty } }
        assertEquals(1, emptyCells)
    }

    @Test
    fun `first call sets launchCount to 1`() = runTest {
        vm.incrementLaunchAndGetPuzzle()
        assertEquals(1, vm.launchCount.value)
    }

    @Test
    fun `second call returns puzzle with 2 empty cells`() = runTest {
        vm.incrementLaunchAndGetPuzzle() // launch 1
        val puzzle = vm.incrementLaunchAndGetPuzzle() // launch 2

        assertNotNull(puzzle)
        val emptyCells = puzzle!!.cells.sumOf { row -> row.count { it.isEmpty } }
        assertEquals(2, emptyCells)
    }

    @Test
    fun `third call returns puzzle with 4 empty cells`() = runTest {
        vm.incrementLaunchAndGetPuzzle() // launch 1
        vm.incrementLaunchAndGetPuzzle() // launch 2
        val puzzle = vm.incrementLaunchAndGetPuzzle() // launch 3

        assertNotNull(puzzle)
        val emptyCells = puzzle!!.cells.sumOf { row -> row.count { it.isEmpty } }
        assertEquals(4, emptyCells)
    }

    // -----------------------------------------------------------------------
    // markComplete
    // -----------------------------------------------------------------------

    @Test
    fun `after markComplete isOnboardingComplete is true`() = runTest {
        vm.markComplete()
        assertTrue(vm.isOnboardingComplete.value)
    }

    @Test
    fun `after markComplete getPuzzleForLaunch returns null`() = runTest {
        vm.markComplete()
        val puzzle = repo.getPuzzleForLaunch(1)
        assertNull(puzzle)
    }

    @Test
    fun `after 3 increments and markComplete getPuzzleForLaunch returns null`() = runTest {
        vm.incrementLaunchAndGetPuzzle() // launch 1
        vm.incrementLaunchAndGetPuzzle() // launch 2
        vm.incrementLaunchAndGetPuzzle() // launch 3
        vm.markComplete()

        val puzzle = repo.getPuzzleForLaunch(1)
        assertNull(puzzle)
        assertTrue(vm.isOnboardingComplete.value)
    }

    // -----------------------------------------------------------------------
    // Puzzle sequence matches OnboardingRepository.PUZZLES
    // -----------------------------------------------------------------------

    @Test
    fun `first puzzle matches PUZZLE_1`() = runTest {
        val puzzle = vm.incrementLaunchAndGetPuzzle()
        assertEquals(OnboardingRepository.PUZZLE_1, puzzle)
    }

    @Test
    fun `second puzzle matches PUZZLE_2`() = runTest {
        vm.incrementLaunchAndGetPuzzle()
        val puzzle = vm.incrementLaunchAndGetPuzzle()
        assertEquals(OnboardingRepository.PUZZLE_2, puzzle)
    }

    @Test
    fun `third puzzle matches PUZZLE_3`() = runTest {
        vm.incrementLaunchAndGetPuzzle()
        vm.incrementLaunchAndGetPuzzle()
        val puzzle = vm.incrementLaunchAndGetPuzzle()
        assertEquals(OnboardingRepository.PUZZLE_3, puzzle)
    }

    @Test
    fun `fourth call returns null — sequence exhausted`() = runTest {
        vm.incrementLaunchAndGetPuzzle()
        vm.incrementLaunchAndGetPuzzle()
        vm.incrementLaunchAndGetPuzzle()
        val puzzle = vm.incrementLaunchAndGetPuzzle()
        assertNull(puzzle)
    }
}

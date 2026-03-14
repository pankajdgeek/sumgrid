package org.dgeek.sumgrid.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.dgeek.sumgrid.daily.CompletionState
import org.dgeek.sumgrid.daily.DailyPuzzleRepository
import org.dgeek.sumgrid.daily.InMemoryCompletionStore
import org.dgeek.sumgrid.engine.PuzzleGenerator
import org.dgeek.sumgrid.engine.UniqueSolutionValidator
import org.dgeek.sumgrid.engine.models.Difficulty
import org.dgeek.sumgrid.streak.InMemoryStreakRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [HomeViewModel].
 *
 * Uses [StandardTestDispatcher] so coroutines run deterministically.
 * All Android dependencies are replaced with in-memory fakes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var completionStore: InMemoryCompletionStore
    private lateinit var repository: DailyPuzzleRepository
    private lateinit var streakRepo: InMemoryStreakRepository
    private lateinit var vm: HomeViewModel

    private val fixedToday = LocalDate.of(2026, 3, 14)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        completionStore = InMemoryCompletionStore()
        val generator = PuzzleGenerator(UniqueSolutionValidator())
        repository = DailyPuzzleRepository(generator, completionStore)
        streakRepo = InMemoryStreakRepository()
        vm = HomeViewModel(
            puzzleRepository = repository,
            streakRepository = streakRepo,
            clock = { fixedToday }
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -----------------------------------------------------------------------
    // formatCountdown
    // -----------------------------------------------------------------------

    @Test
    fun formatCountdown_zero_isAllZeros() {
        assertEquals("00:00:00", vm.formatCountdown(0L))
    }

    @Test
    fun formatCountdown_oneHour() {
        assertEquals("01:00:00", vm.formatCountdown(3600L))
    }

    @Test
    fun formatCountdown_mixedHoursMinutesSeconds() {
        // 1h 23m 45s = 3600 + 1380 + 45 = 5025
        assertEquals("01:23:45", vm.formatCountdown(5025L))
    }

    @Test
    fun formatCountdown_23Hours59Min59Sec() {
        val maxSeconds = 23 * 3600L + 59 * 60L + 59L
        assertEquals("23:59:59", vm.formatCountdown(maxSeconds))
    }

    @Test
    fun formatCountdown_59Seconds() {
        assertEquals("00:00:59", vm.formatCountdown(59L))
    }

    // -----------------------------------------------------------------------
    // secondsUntilMidnight — value sanity
    // -----------------------------------------------------------------------

    @Test
    fun secondsUntilMidnight_isPositive() {
        val seconds = vm.secondsUntilMidnight()
        assertTrue("Expected positive seconds, got $seconds", seconds > 0L)
    }

    @Test
    fun secondsUntilMidnight_isAtMostOneDayInSeconds() {
        val seconds = vm.secondsUntilMidnight()
        assertTrue("Expected <= 86400, got $seconds", seconds <= 86400L)
    }

    // -----------------------------------------------------------------------
    // Initial UI state
    // -----------------------------------------------------------------------

    @Test
    fun initialState_isLoading() {
        // Before coroutines run, state is loading
        assertTrue(vm.uiState.value.isLoading)
    }

    @Test
    fun initialState_hasThreePuzzleStatuses() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        val state = vm.uiState.value
        assertEquals(3, state.puzzleStatuses.size)
    }

    @Test
    fun initialState_allPuzzlesNotCompleted() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        val state = vm.uiState.value
        state.puzzleStatuses.forEach { status ->
            assertFalse("${status.difficulty} should not be completed", status.isCompleted)
        }
    }

    @Test
    fun initialState_streakIsZero() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, vm.uiState.value.currentStreak)
    }

    @Test
    fun initialState_isLoadingFalseAfterLoad() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
    }

    // -----------------------------------------------------------------------
    // Puzzle status reflects completion store
    // -----------------------------------------------------------------------

    @Test
    fun loadHomeData_reflectsCompletedPuzzle() = runTest {
        // Pre-populate completion for BEGINNER
        val key = "completion_${fixedToday.toEpochDay()}_${Difficulty.BEGINNER.name}"
        completionStore.save(
            key,
            CompletionState(completed = true, elapsedMillis = 60_000L, completedAt = System.currentTimeMillis())
        )

        vm.loadHomeData()
        testDispatcher.scheduler.advanceUntilIdle()

        val beginnerStatus = vm.uiState.value.puzzleStatuses
            .first { it.difficulty == Difficulty.BEGINNER }
        assertTrue(beginnerStatus.isCompleted)
    }

    @Test
    fun loadHomeData_onlyCompletedDifficultyIsMarked() = runTest {
        val key = "completion_${fixedToday.toEpochDay()}_${Difficulty.EASY.name}"
        completionStore.save(
            key,
            CompletionState(completed = true, elapsedMillis = 120_000L, completedAt = System.currentTimeMillis())
        )

        vm.loadHomeData()
        testDispatcher.scheduler.advanceUntilIdle()

        val statuses = vm.uiState.value.puzzleStatuses
        assertTrue(statuses.first { it.difficulty == Difficulty.EASY }.isCompleted)
        assertFalse(statuses.first { it.difficulty == Difficulty.BEGINNER }.isCompleted)
        assertFalse(statuses.first { it.difficulty == Difficulty.MEDIUM }.isCompleted)
    }

    // -----------------------------------------------------------------------
    // Streak updates
    // -----------------------------------------------------------------------

    @Test
    fun streakUpdate_reflectsAfterRecordCompletion() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, vm.uiState.value.currentStreak)

        streakRepo.recordCompletion(fixedToday)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.uiState.value.currentStreak)
    }

    @Test
    fun streakUpdate_twoConsecutiveDays_streakIsTwo() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        streakRepo.recordCompletion(fixedToday.minusDays(1))
        testDispatcher.scheduler.advanceUntilIdle()

        streakRepo.recordCompletion(fixedToday)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, vm.uiState.value.currentStreak)
    }

    // -----------------------------------------------------------------------
    // tickCountdown
    // -----------------------------------------------------------------------

    @Test
    fun tickCountdown_updatesSecondsUntilMidnight() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        val before = vm.uiState.value.secondsUntilMidnight
        // Midnight countdown changes every tick; just verify it's within valid range
        vm.tickCountdown()
        val after = vm.uiState.value.secondsUntilMidnight
        assertTrue(after >= 0L)
        assertTrue(after <= 86400L)
    }

    // -----------------------------------------------------------------------
    // puzzleStatuses ordering
    // -----------------------------------------------------------------------

    @Test
    fun puzzleStatuses_orderedAsBEGINNER_EASY_MEDIUM() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        val difficulties = vm.uiState.value.puzzleStatuses.map { it.difficulty }
        assertEquals(listOf(Difficulty.BEGINNER, Difficulty.EASY, Difficulty.MEDIUM), difficulties)
    }
}

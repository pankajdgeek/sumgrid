package org.dgeek.sumgrid.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.dgeek.sumgrid.daily.DailyPuzzleRepository
import org.dgeek.sumgrid.daily.InMemoryCompletionStore
import org.dgeek.sumgrid.engine.PuzzleGenerator
import org.dgeek.sumgrid.engine.UniqueSolutionValidator
import org.dgeek.sumgrid.streak.InMemoryStreakRepository
import org.dgeek.sumgrid.streak.StreakBadge
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for badge display and all-done state in HomeViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelBadgeTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var streakRepo: InMemoryStreakRepository
    private lateinit var vm: HomeViewModel
    private val fixedToday = LocalDate.of(2026, 3, 14)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val completionStore = InMemoryCompletionStore()
        val generator = PuzzleGenerator(UniqueSolutionValidator())
        val repository = DailyPuzzleRepository(generator, completionStore)
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

    @Test
    fun earnedBadges_emptyInitially() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.earnedBadges.isEmpty())
    }

    @Test
    fun earnedBadges_weeklyWarriorAfter7Days() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        // Record 7 consecutive completions
        for (i in 6 downTo 0) {
            streakRepo.recordCompletion(fixedToday.minusDays(i.toLong()))
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(
            "Should earn WEEKLY_WARRIOR after 7 completions",
            vm.uiState.value.earnedBadges.contains(StreakBadge.WEEKLY_WARRIOR)
        )
    }

    @Test
    fun allComplete_falseInitially() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.allComplete)
    }
}

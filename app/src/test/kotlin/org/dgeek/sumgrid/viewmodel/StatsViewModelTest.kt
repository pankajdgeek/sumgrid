package org.dgeek.sumgrid.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.dgeek.sumgrid.daily.CompletionState
import org.dgeek.sumgrid.daily.InMemoryCompletionStore
import org.dgeek.sumgrid.engine.models.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class StatsViewModelTest {

    private lateinit var store: InMemoryCompletionStore
    private lateinit var vm: StatsViewModel
    private val scope = CoroutineScope(Dispatchers.Unconfined)

    @Before
    fun setUp() {
        store = InMemoryCompletionStore()
        vm = StatsViewModel(completionStore = store, scope = scope)
    }

    @Test
    fun stats_totalSolvedCounts_allDifficulties() = runBlocking {
        // Seed 10 completions: 3 BEGINNER, 4 EASY, 3 MEDIUM
        repeat(3) { i ->
            val date = LocalDate.of(2025, 6, i + 1)
            store.save("completion_${date.toEpochDay()}_BEGINNER", completionState(60_000L))
        }
        repeat(4) { i ->
            val date = LocalDate.of(2025, 6, i + 1)
            store.save("completion_${date.toEpochDay()}_EASY", completionState(90_000L))
        }
        repeat(3) { i ->
            val date = LocalDate.of(2025, 6, i + 1)
            store.save("completion_${date.toEpochDay()}_MEDIUM", completionState(120_000L))
        }

        vm.loadStats()

        val state = vm.uiState.value
        assertEquals(10, state.totalSolved)
        assertEquals(3, state.countByDifficulty[Difficulty.BEGINNER])
        assertEquals(4, state.countByDifficulty[Difficulty.EASY])
        assertEquals(3, state.countByDifficulty[Difficulty.MEDIUM])
    }

    @Test
    fun stats_averageTime_perDifficulty() = runBlocking {
        val date1 = LocalDate.of(2025, 6, 1)
        val date2 = LocalDate.of(2025, 6, 2)
        store.save("completion_${date1.toEpochDay()}_BEGINNER", completionState(60_000L))
        store.save("completion_${date2.toEpochDay()}_BEGINNER", completionState(80_000L))

        vm.loadStats()

        assertEquals(70_000L, vm.uiState.value.avgTimeMillis[Difficulty.BEGINNER])
    }

    @Test
    fun stats_bestTime_perDifficulty() = runBlocking {
        val date1 = LocalDate.of(2025, 6, 1)
        val date2 = LocalDate.of(2025, 6, 2)
        store.save("completion_${date1.toEpochDay()}_BEGINNER", completionState(60_000L))
        store.save("completion_${date2.toEpochDay()}_BEGINNER", completionState(45_000L))

        vm.loadStats()

        assertEquals(45_000L, vm.uiState.value.bestTimeMillis[Difficulty.BEGINNER])
    }

    @Test
    fun stats_completionDates_asSet() = runBlocking {
        val date1 = LocalDate.of(2025, 6, 15)
        val date2 = LocalDate.of(2025, 6, 16)
        store.save("completion_${date1.toEpochDay()}_BEGINNER", completionState(60_000L))
        store.save("completion_${date2.toEpochDay()}_EASY", completionState(90_000L))

        vm.loadStats()

        assertTrue(vm.uiState.value.calendarDates.contains(date1))
        assertTrue(vm.uiState.value.calendarDates.contains(date2))
        assertEquals(2, vm.uiState.value.calendarDates.size)
    }

    @Test
    fun parseCompletionKey_valid() {
        val result = vm.parseCompletionKey("completion_20254_BEGINNER")
        assertEquals(LocalDate.ofEpochDay(20254L), result?.first)
        assertEquals(Difficulty.BEGINNER, result?.second)
    }

    @Test
    fun parseCompletionKey_invalid() {
        assertEquals(null, vm.parseCompletionKey("in_progress_something"))
        assertEquals(null, vm.parseCompletionKey("completion_abc_BEGINNER"))
        assertEquals(null, vm.parseCompletionKey("completion_20254_UNKNOWN"))
    }

    private fun completionState(elapsed: Long) = CompletionState(
        completed = true,
        elapsedMillis = elapsed,
        completedAt = System.currentTimeMillis()
    )
}

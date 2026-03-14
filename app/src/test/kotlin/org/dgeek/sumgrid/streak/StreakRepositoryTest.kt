package org.dgeek.sumgrid.streak

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for StreakRepository using InMemoryStreakRepository (no DataStore needed).
 *
 * The production StreakRepository is backed by DataStore. For unit tests we use an
 * in-memory implementation that has identical logic but stores data in a MutableMap.
 */
class StreakRepositoryTest {

    private lateinit var repo: InMemoryStreakRepository

    @Before
    fun setUp() {
        repo = InMemoryStreakRepository()
    }

    // --- Basic streak counting ---

    @Test
    fun `day 1 completion sets streak to 1`() = runTest {
        val day1 = LocalDate.of(2026, 1, 1)
        repo.recordCompletion(day1)
        val state = repo.streakState.first()
        assertEquals(1, state.currentStreak)
        assertEquals(1, state.longestStreak)
        assertEquals(day1, state.lastCompletionDate)
    }

    @Test
    fun `consecutive days increments streak`() = runTest {
        val day1 = LocalDate.of(2026, 1, 1)
        val day2 = LocalDate.of(2026, 1, 2)
        repo.recordCompletion(day1)
        repo.recordCompletion(day2)
        val state = repo.streakState.first()
        assertEquals(2, state.currentStreak)
        assertEquals(2, state.longestStreak)
        assertEquals(day2, state.lastCompletionDate)
    }

    @Test
    fun `skipping a day resets streak to 1`() = runTest {
        val day1 = LocalDate.of(2026, 1, 1)
        val day3 = LocalDate.of(2026, 1, 3)
        repo.recordCompletion(day1)
        repo.recordCompletion(day3)
        val state = repo.streakState.first()
        assertEquals(1, state.currentStreak)
        assertEquals(1, state.longestStreak)
        assertEquals(day3, state.lastCompletionDate)
    }

    @Test
    fun `same day twice is a no-op`() = runTest {
        val day1 = LocalDate.of(2026, 1, 1)
        repo.recordCompletion(day1)
        repo.recordCompletion(day1) // second call same day
        val state = repo.streakState.first()
        assertEquals(1, state.currentStreak)
        assertEquals(day1, state.lastCompletionDate)
    }

    @Test
    fun `longest streak is preserved after reset`() = runTest {
        // Build a streak of 3, then break it, then do 1
        repo.recordCompletion(LocalDate.of(2026, 1, 1))
        repo.recordCompletion(LocalDate.of(2026, 1, 2))
        repo.recordCompletion(LocalDate.of(2026, 1, 3))
        // Skip day 4, then complete day 5
        repo.recordCompletion(LocalDate.of(2026, 1, 5))
        val state = repo.streakState.first()
        assertEquals(1, state.currentStreak)
        assertEquals(3, state.longestStreak) // was 3, should stay 3
    }

    @Test
    fun `multiple consecutive days build streak correctly`() = runTest {
        for (day in 1..5) {
            repo.recordCompletion(LocalDate.of(2026, 1, day))
        }
        val state = repo.streakState.first()
        assertEquals(5, state.currentStreak)
        assertEquals(5, state.longestStreak)
    }

    // --- Badge earning ---

    @Test
    fun `streak 7 earns WEEKLY_WARRIOR badge`() = runTest {
        for (day in 1..7) {
            repo.recordCompletion(LocalDate.of(2026, 1, day))
        }
        val state = repo.streakState.first()
        assertTrue(
            "Expected WEEKLY_WARRIOR badge at streak 7",
            state.earnedBadges.contains(StreakBadge.WEEKLY_WARRIOR)
        )
    }

    @Test
    fun `streak 6 does not earn WEEKLY_WARRIOR badge`() = runTest {
        for (day in 1..6) {
            repo.recordCompletion(LocalDate.of(2026, 1, day))
        }
        val state = repo.streakState.first()
        assertFalse(
            "Should not have WEEKLY_WARRIOR badge at streak 6",
            state.earnedBadges.contains(StreakBadge.WEEKLY_WARRIOR)
        )
    }

    @Test
    fun `earned badge persists after streak breaks`() = runTest {
        // Earn WEEKLY_WARRIOR
        for (day in 1..7) {
            repo.recordCompletion(LocalDate.of(2026, 1, day))
        }
        // Break streak (skip day 8, do day 10)
        repo.recordCompletion(LocalDate.of(2026, 1, 10))
        val state = repo.streakState.first()
        assertEquals(1, state.currentStreak) // streak reset
        assertTrue(
            "WEEKLY_WARRIOR badge should persist after streak break",
            state.earnedBadges.contains(StreakBadge.WEEKLY_WARRIOR)
        )
    }

    @Test
    fun `initial state has zero streak and no badges`() = runTest {
        val state = repo.streakState.first()
        assertEquals(0, state.currentStreak)
        assertEquals(0, state.longestStreak)
        assertEquals(null, state.lastCompletionDate)
        assertTrue(state.earnedBadges.isEmpty())
    }
}

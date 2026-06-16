package org.dgeek.sumgrid.review

import kotlinx.coroutines.test.runTest
import org.dgeek.sumgrid.streak.StreakBadge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for the in-app review trigger logic using [InMemoryReviewTrigger].
 *
 * No DataStore or Play Store needed — [InMemoryReviewTrigger] mirrors the
 * eligibility logic of [InAppReviewTrigger] in a pure-JVM fashion.
 *
 * Three automatic paths share one per-install budget; a fourth manual path
 * is unconditional. Tests below cover each path independently.
 */
class InAppReviewTriggerTest {

    private lateinit var trigger: InMemoryReviewTrigger

    @Before
    fun setUp() {
        trigger = InMemoryReviewTrigger()
    }

    // -------------------------------------------------------------------------
    // Counter increments
    // -------------------------------------------------------------------------

    @Test
    fun `recordDailyCompletion increments lifetime count by one each time`() = runTest {
        assertEquals(0, trigger.getLifetimeCompletions())
        trigger.recordDailyCompletion()
        assertEquals(1, trigger.getLifetimeCompletions())
        trigger.recordDailyCompletion()
        assertEquals(2, trigger.getLifetimeCompletions())
    }

    @Test
    fun `recordPracticeCompletion increments practice count by one each time`() = runTest {
        assertEquals(0, trigger.getPracticeCompletions())
        trigger.recordPracticeCompletion()
        assertEquals(1, trigger.getPracticeCompletions())
        trigger.recordPracticeCompletion()
        assertEquals(2, trigger.getPracticeCompletions())
    }

    @Test
    fun `daily and practice counters are independent`() = runTest {
        trigger.recordDailyCompletion()
        trigger.recordPracticeCompletion()
        trigger.recordPracticeCompletion()
        assertEquals(1, trigger.getLifetimeCompletions())
        assertEquals(2, trigger.getPracticeCompletions())
    }

    // -------------------------------------------------------------------------
    // isEligibleAfterDaily — threshold gating
    // -------------------------------------------------------------------------

    @Test
    fun `daily eligibility is false below threshold`() = runTest {
        repeat(InAppReviewTrigger.DAILY_THRESHOLD - 1) { trigger.recordDailyCompletion() }
        assertFalse(trigger.isEligibleAfterDaily())
    }

    @Test
    fun `daily eligibility flips true at threshold`() = runTest {
        repeat(InAppReviewTrigger.DAILY_THRESHOLD) { trigger.recordDailyCompletion() }
        assertTrue(trigger.isEligibleAfterDaily())
    }

    @Test
    fun `daily eligibility is false after budget spent even above threshold`() = runTest {
        repeat(InAppReviewTrigger.DAILY_THRESHOLD + 5) { trigger.recordDailyCompletion() }
        trigger.requestReviewIfEligible()
        assertFalse(trigger.isEligibleAfterDaily())
    }

    // -------------------------------------------------------------------------
    // isEligibleAfterPractice — threshold gating
    // -------------------------------------------------------------------------

    @Test
    fun `practice eligibility is false below threshold`() = runTest {
        repeat(InAppReviewTrigger.PRACTICE_THRESHOLD - 1) { trigger.recordPracticeCompletion() }
        assertFalse(trigger.isEligibleAfterPractice())
    }

    @Test
    fun `practice eligibility flips true at threshold`() = runTest {
        repeat(InAppReviewTrigger.PRACTICE_THRESHOLD) { trigger.recordPracticeCompletion() }
        assertTrue(trigger.isEligibleAfterPractice())
    }

    @Test
    fun `practice eligibility is false after budget spent`() = runTest {
        repeat(InAppReviewTrigger.PRACTICE_THRESHOLD) { trigger.recordPracticeCompletion() }
        trigger.requestReviewIfEligible()
        assertFalse(trigger.isEligibleAfterPractice())
    }

    // -------------------------------------------------------------------------
    // isEligibleAfterStreak — only StreakBadge milestone values qualify
    // -------------------------------------------------------------------------

    @Test
    fun `streak eligibility is true at every StreakBadge milestone`() = runTest {
        for (badge in StreakBadge.entries) {
            val freshTrigger = InMemoryReviewTrigger()
            assertTrue(
                "Expected eligibility at ${badge.name} (${badge.requiredDays}-day streak)",
                freshTrigger.isEligibleAfterStreak(badge.requiredDays),
            )
        }
    }

    @Test
    fun `streak eligibility is false for non-milestone day counts`() = runTest {
        // Pick a few values that aren't milestone targets.
        val milestones = StreakBadge.entries.map { it.requiredDays }.toSet()
        listOf(1, 2, 3, 5, 10, 14, 50, 99, 200).forEach { days ->
            if (days !in milestones) {
                assertFalse(
                    "Day $days should not trigger streak eligibility",
                    trigger.isEligibleAfterStreak(days),
                )
            }
        }
    }

    @Test
    fun `streak eligibility is false after budget spent`() = runTest {
        // Spend the budget via a practice completion path first.
        repeat(InAppReviewTrigger.PRACTICE_THRESHOLD) { trigger.recordPracticeCompletion() }
        trigger.requestReviewIfEligible()
        assertTrue(trigger.isReviewRequested())
        // Streak milestone is now irrelevant.
        assertFalse(trigger.isEligibleAfterStreak(StreakBadge.WEEKLY_WARRIOR.requiredDays))
    }

    // -------------------------------------------------------------------------
    // requestReviewIfEligible — budget flag is the only guard
    // -------------------------------------------------------------------------

    @Test
    fun `requestReviewIfEligible launches when budget is unspent`() = runTest {
        trigger.requestReviewIfEligible()
        assertTrue(trigger.reviewLaunched)
        assertTrue(trigger.isReviewRequested())
    }

    @Test
    fun `requestReviewIfEligible is a no-op once budget is spent`() = runTest {
        trigger.requestReviewIfEligible() // spends budget
        // Reaching budget shouldn't unlatch reviewLaunched, but a second call
        // must not re-flip state — we verify isReviewRequested stays true and
        // no further side effects occur.
        val firstLaunched = trigger.reviewLaunched
        trigger.requestReviewIfEligible()
        assertEquals(firstLaunched, trigger.reviewLaunched)
        assertTrue(trigger.isReviewRequested())
    }

    // -------------------------------------------------------------------------
    // launchManualReview — unconditional, never spends the auto budget
    // -------------------------------------------------------------------------

    @Test
    fun `launchManualReview fires even when budget is unspent`() = runTest {
        trigger.launchManualReview()
        assertTrue(trigger.manualReviewLaunched)
    }

    @Test
    fun `launchManualReview does not spend the automatic budget`() = runTest {
        trigger.launchManualReview()
        assertFalse(
            "Manual launches must not preempt automatic prompts",
            trigger.isReviewRequested(),
        )
    }

    @Test
    fun `launchManualReview still fires after automatic budget is spent`() = runTest {
        trigger.requestReviewIfEligible() // spend budget
        trigger.launchManualReview()
        assertTrue(trigger.manualReviewLaunched)
    }

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    fun `initial state has zero completions and review not requested`() = runTest {
        assertEquals(0, trigger.getLifetimeCompletions())
        assertEquals(0, trigger.getPracticeCompletions())
        assertFalse(trigger.isReviewRequested())
        assertFalse(trigger.reviewLaunched)
        assertFalse(trigger.manualReviewLaunched)
    }
}

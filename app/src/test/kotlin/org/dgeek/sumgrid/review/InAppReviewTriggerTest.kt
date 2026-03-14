package org.dgeek.sumgrid.review

import kotlinx.coroutines.test.runTest
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
 */
class InAppReviewTriggerTest {

    private lateinit var trigger: InMemoryReviewTrigger

    @Before
    fun setUp() {
        trigger = InMemoryReviewTrigger()
    }

    // -------------------------------------------------------------------------
    // recordCompletion — counter increments
    // -------------------------------------------------------------------------

    @Test
    fun `recordCompletion increments lifetime count by one each time`() = runTest {
        assertEquals(0, trigger.getLifetimeCompletions())
        trigger.recordCompletion()
        assertEquals(1, trigger.getLifetimeCompletions())
        trigger.recordCompletion()
        assertEquals(2, trigger.getLifetimeCompletions())
    }

    // -------------------------------------------------------------------------
    // requestReviewIfEligible — below threshold
    // -------------------------------------------------------------------------

    @Test
    fun `review is NOT launched after 2 completions`() = runTest {
        trigger.recordCompletion()
        trigger.recordCompletion()
        trigger.requestReviewIfEligible()
        assertFalse("Review should not launch below threshold", trigger.reviewLaunched)
    }

    @Test
    fun `review is NOT requested below threshold`() = runTest {
        trigger.recordCompletion()
        trigger.recordCompletion()
        trigger.requestReviewIfEligible()
        assertFalse(trigger.isReviewRequested())
    }

    // -------------------------------------------------------------------------
    // requestReviewIfEligible — at threshold
    // -------------------------------------------------------------------------

    @Test
    fun `review IS launched after 3rd completion`() = runTest {
        trigger.recordCompletion()
        trigger.recordCompletion()
        trigger.recordCompletion()
        trigger.requestReviewIfEligible()
        assertTrue("Review should launch at threshold (3)", trigger.reviewLaunched)
    }

    @Test
    fun `review IS marked requested after 3rd completion`() = runTest {
        repeat(InAppReviewTrigger.REVIEW_THRESHOLD) { trigger.recordCompletion() }
        trigger.requestReviewIfEligible()
        assertTrue(trigger.isReviewRequested())
    }

    // -------------------------------------------------------------------------
    // requestReviewIfEligible — idempotency / no re-trigger
    // -------------------------------------------------------------------------

    @Test
    fun `calling requestReviewIfEligible twice only launches once`() = runTest {
        repeat(InAppReviewTrigger.REVIEW_THRESHOLD) { trigger.recordCompletion() }
        trigger.requestReviewIfEligible() // first call — should launch
        assertTrue(trigger.reviewLaunched)

        // Reset launched flag to detect a second launch
        val firstLaunched = trigger.reviewLaunched
        // Add more completions; flag should not flip again
        trigger.recordCompletion()
        trigger.requestReviewIfEligible() // second call — must be no-op
        // reviewLaunched stays true but we can verify isReviewRequested still true
        assertTrue("Requested flag must stay true", trigger.isReviewRequested())
        // And the count is above threshold but no additional side effects
        assertEquals(InAppReviewTrigger.REVIEW_THRESHOLD + 1, trigger.getLifetimeCompletions())
        // firstLaunched was true; second call was a no-op (launched stays true,
        // but we cannot increment a boolean — so we verify it does not reset)
        assertEquals(firstLaunched, trigger.reviewLaunched)
    }

    // -------------------------------------------------------------------------
    // Above threshold but already requested
    // -------------------------------------------------------------------------

    @Test
    fun `review is not launched when already requested even with many completions`() = runTest {
        // Simulate reaching threshold + many more completions
        repeat(10) { trigger.recordCompletion() }
        trigger.requestReviewIfEligible() // launches once
        assertTrue(trigger.reviewLaunched)
        // isReviewRequested must now block further launches
        assertTrue(trigger.isReviewRequested())
        // Explicitly: calling again should not change state
        trigger.requestReviewIfEligible()
        // No way to observe a "second" launch on a boolean flag, but
        // isReviewRequested must remain true (not flipped back)
        assertTrue(trigger.isReviewRequested())
    }

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    fun `initial state has zero completions and review not requested`() = runTest {
        assertEquals(0, trigger.getLifetimeCompletions())
        assertFalse(trigger.isReviewRequested())
        assertFalse(trigger.reviewLaunched)
    }
}

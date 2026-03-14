package org.dgeek.sumgrid.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Xorshift128Test {

    // -----------------------------------------------------------------------
    // Determinism: two instances with the same seed produce identical output
    // -----------------------------------------------------------------------

    @Test
    fun sameSeedException_producesIdenticalSequences() {
        val rng1 = Xorshift128(42L)
        val rng2 = Xorshift128(42L)
        repeat(20) {
            assertEquals(
                "nextInt(100) diverged at call $it",
                rng1.nextInt(100),
                rng2.nextInt(100)
            )
        }
    }

    @Test
    fun differentSeeds_producesDifferentSequences() {
        val rng1 = Xorshift128(1L)
        val rng2 = Xorshift128(2L)
        val seq1 = List(20) { rng1.nextInt(100) }
        val seq2 = List(20) { rng2.nextInt(100) }
        assertTrue("Different seeds should produce different sequences", seq1 != seq2)
    }

    // -----------------------------------------------------------------------
    // Bounds: nextInt(N) always returns a value in [0, N)
    // -----------------------------------------------------------------------

    @Test
    fun nextInt_respectsBounds_bound1() {
        val rng = Xorshift128(123L)
        repeat(1000) {
            val v = rng.nextInt(1)
            assertTrue("nextInt(1) must return 0, got $v", v == 0)
        }
    }

    @Test
    fun nextInt_respectsBounds_bound2() {
        val rng = Xorshift128(123L)
        repeat(1000) {
            val v = rng.nextInt(2)
            assertTrue("nextInt(2) out of range: $v", v in 0 until 2)
        }
    }

    @Test
    fun nextInt_respectsBounds_bound5() {
        val rng = Xorshift128(123L)
        repeat(1000) {
            val v = rng.nextInt(5)
            assertTrue("nextInt(5) out of range: $v", v in 0 until 5)
        }
    }

    @Test
    fun nextInt_respectsBounds_bound9() {
        val rng = Xorshift128(123L)
        repeat(1000) {
            val v = rng.nextInt(9)
            assertTrue("nextInt(9) out of range: $v", v in 0 until 9)
        }
    }

    @Test
    fun nextInt_respectsBounds_bound10() {
        val rng = Xorshift128(123L)
        repeat(1000) {
            val v = rng.nextInt(10)
            assertTrue("nextInt(10) out of range: $v", v in 0 until 10)
        }
    }

    @Test
    fun nextInt_respectsBounds_bound100() {
        val rng = Xorshift128(123L)
        repeat(1000) {
            val v = rng.nextInt(100)
            assertTrue("nextInt(100) out of range: $v", v in 0 until 100)
        }
    }

    // -----------------------------------------------------------------------
    // Seed 0 safety: first output must be non-zero
    // -----------------------------------------------------------------------

    @Test
    fun seedZero_firstOutputIsNonZero() {
        val rng = Xorshift128(0L)
        val first = rng.nextInt(1000000)
        assertTrue("Seed 0 first output must be non-zero, got $first", first != 0)
    }

    // -----------------------------------------------------------------------
    // Distribution: rough uniformity over 10,000 calls to nextInt(10)
    // Each value [0,10) should appear 800–1200 times (10% ± 20%)
    // -----------------------------------------------------------------------

    @Test
    fun distribution_isRoughlyUniform() {
        val rng = Xorshift128(999L)
        val counts = IntArray(10)
        val total = 10_000
        repeat(total) { counts[rng.nextInt(10)]++ }
        for (i in 0 until 10) {
            assertTrue(
                "Value $i appeared ${counts[i]} times (expected 800–1200)",
                counts[i] in 800..1200
            )
        }
    }

    // -----------------------------------------------------------------------
    // Regression (golden values): seed 42L, first 10 calls to nextInt(100)
    //
    // These values were captured after GREEN implementation and are locked
    // here to catch any accidental algorithm changes.
    // -----------------------------------------------------------------------

    @Test
    fun goldenValues_seed42_nextInt100() {
        val rng = Xorshift128(42L)
        val actual = List(10) { rng.nextInt(100) }

        // Golden sequence captured from the reference implementation.
        // If this test fails after a deliberate algorithm change, update
        // the golden list to the new sequence AND bump the version comment.
        // v1 — xorshift128+ with splitmix64 seeding (2026-03-14)
        val golden = listOf(46, 43, 63, 75, 29, 54, 88, 74, 4, 47)

        assertEquals("Golden value regression for seed=42", golden, actual)
    }

    // -----------------------------------------------------------------------
    // API contract: negative bound throws IllegalArgumentException
    // -----------------------------------------------------------------------

    @Test(expected = IllegalArgumentException::class)
    fun nextInt_negativeBound_throws() {
        Xorshift128(1L).nextInt(-1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun nextInt_zeroBound_throws() {
        Xorshift128(1L).nextInt(0)
    }
}

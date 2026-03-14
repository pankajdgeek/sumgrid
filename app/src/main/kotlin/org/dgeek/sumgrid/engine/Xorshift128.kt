package org.dgeek.sumgrid.engine

/**
 * Xorshift128+ PRNG — a fast, deterministic pseudo-random number generator
 * with a 128-bit state and a period of 2^128 - 1.
 *
 * Seeding uses two rounds of splitmix64 to avoid a degenerate all-zero
 * initial state even when [seed] is 0.
 *
 * Zero external dependencies: no kotlin.random.Random, no java.util.Random.
 *
 * @param seed Arbitrary 64-bit seed value. Two instances created with the
 *   same seed will always produce identical sequences.
 */
class Xorshift128(seed: Long) {

    private var state0: Long
    private var state1: Long

    init {
        // splitmix64: two independent derivations for the two state words.
        // Constants are the published splitmix64 mixing values.
        var s = seed

        // --- state0 ---
        s += -7046029254386353131L          // 0x9E3779B97F4A7C15 (Weyl increment)
        s = (s xor (s ushr 30)) * -4658895280553007687L  // 0xBF58476D1CE4E5B9
        s = (s xor (s ushr 27)) * -7723592293110705685L  // 0x94D049BB133111EB
        state0 = s xor (s ushr 31)

        // --- state1 ---
        s += -7046029254386353131L
        s = (s xor (s ushr 30)) * -4658895280553007687L
        s = (s xor (s ushr 27)) * -7723592293110705685L
        state1 = s xor (s ushr 31)

        // Guard against the (astronomically unlikely) all-zero state.
        if (state0 == 0L && state1 == 0L) state0 = 1L
    }

    /**
     * Returns the next pseudo-random 64-bit value and advances the state.
     *
     * Uses the xorshift128+ algorithm as described in:
     * "Further scramblings of Marsaglia's xorshift generators" (Vigna, 2017).
     */
    fun nextLong(): Long {
        var s1 = state0
        val s0 = state1
        state0 = s0
        s1 = s1 xor (s1 shl 23)
        state1 = s1 xor s0 xor (s1 ushr 17) xor (s0 ushr 26)
        return state1 + s0
    }

    /**
     * Returns a pseudo-random [Int] in the range [0, bound).
     *
     * Uses the upper 31 bits of [nextLong] (bits 62..32) to avoid the
     * known weak low bits in xorshift128+, then applies modulo reduction.
     * For small bounds the modulo bias is negligible.
     *
     * @param bound Exclusive upper bound; must be positive.
     * @throws IllegalArgumentException if [bound] is not positive.
     */
    fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be positive, was $bound" }
        // Use bits [62..32] — the top 31 bits are the highest quality.
        val bits = ((nextLong() ushr 32) and 0x7FFF_FFFFL).toInt()
        return bits % bound
    }
}

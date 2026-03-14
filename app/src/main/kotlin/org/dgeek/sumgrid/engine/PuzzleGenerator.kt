package org.dgeek.sumgrid.engine

import org.dgeek.sumgrid.engine.models.Cell
import org.dgeek.sumgrid.engine.models.Difficulty
import org.dgeek.sumgrid.engine.models.Puzzle

/**
 * Generates SumGrid puzzles deterministically from a seed and difficulty.
 *
 * Algorithm:
 *  1. Seed Xorshift128 with seed + difficulty.seedOffset
 *  2. Fill NxN grid with random values in [1..maxVal]
 *  3. Compute rowTargets and colTargets from the complete grid
 *  4. Shuffle cell indices using the PRNG to build a removal order
 *  5. Remove cells one at a time; after each removal verify unique solution
 *  6. Stop when difficulty.emptyCells cells have been removed
 *  7. If uniqueness cannot be preserved for the required count, retry with seed+10
 *
 * Zero Android dependencies — pure Kotlin.
 */
class PuzzleGenerator(
    private val validator: UniqueSolutionValidator = UniqueSolutionValidator()
) {

    fun generate(seed: Long, difficulty: Difficulty): Puzzle {
        var attemptSeed = seed
        while (true) {
            val result = tryGenerate(attemptSeed, difficulty)
            if (result != null) return result
            // Retry with a shifted seed — keeps determinism per (seed, difficulty) pair
            attemptSeed += 10L
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * One generation attempt. Returns null if the uniqueness constraint
     * cannot be satisfied for the required empty-cell count.
     */
    private fun tryGenerate(seed: Long, difficulty: Difficulty): Puzzle? {
        val n = difficulty.size
        val maxVal = difficulty.maxVal
        val targetEmpties = difficulty.emptyCells

        val rng = Xorshift128(seed + difficulty.seedOffset.toLong())

        // Step 1: fill complete grid with random values in [1..maxVal]
        val fullGrid = Array(n) { IntArray(n) { rng.nextInt(maxVal) + 1 } }

        // Step 2: compute row and column targets from the complete grid
        val rowTargets = IntArray(n) { r -> fullGrid[r].sum() }
        val colTargets = IntArray(n) { c -> (0 until n).sumOf { r -> fullGrid[r][c] } }

        // Step 3: build a shuffled removal order (Fisher-Yates with PRNG)
        val indices = (0 until n * n).toMutableList()
        for (i in indices.size - 1 downTo 1) {
            val j = rng.nextInt(i + 1)
            val tmp = indices[i]; indices[i] = indices[j]; indices[j] = tmp
        }

        // Step 4: work on a mutable grid copy; remove cells while unique solution is preserved
        val workGrid = Array(n) { r -> fullGrid[r].copyOf() }
        var removedCount = 0

        for (idx in indices) {
            if (removedCount == targetEmpties) break

            val r = idx / n
            val c = idx % n
            val savedValue = workGrid[r][c]

            // Tentatively remove this cell
            workGrid[r][c] = 0

            // Check uniqueness
            val count = validator.validate(workGrid, rowTargets, colTargets, maxVal)
            if (count == SolutionCount.Unique) {
                removedCount++
            } else {
                // Restore — removing this cell breaks uniqueness
                workGrid[r][c] = savedValue
            }
        }

        // Could not achieve the required number of empty cells
        if (removedCount < targetEmpties) return null

        // Step 5: build Cell objects — given vs. empty
        val cells = Array(n) { r ->
            Array(n) { c ->
                val v = workGrid[r][c]
                if (v != 0) Cell(value = v, isGiven = true)
                else Cell(value = 0, isGiven = false)
            }
        }

        return Puzzle(
            size = n,
            cells = cells,
            rowTargets = rowTargets,
            colTargets = colTargets,
            difficulty = difficulty,
            seed = seed
        )
    }
}

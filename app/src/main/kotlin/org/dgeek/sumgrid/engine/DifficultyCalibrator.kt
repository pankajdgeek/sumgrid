package org.dgeek.sumgrid.engine

import org.dgeek.sumgrid.engine.models.Difficulty
import org.dgeek.sumgrid.engine.models.Puzzle

/**
 * Measures the intrinsic difficulty of a puzzle by counting how many empty
 * cells can be filled purely through constraint propagation (no backtracking).
 *
 * Algorithm for countSolveSteps:
 *  1. Build a mutable working grid and row/col remaining-sum + empty-count arrays.
 *  2. In each pass, scan all unfilled cells.
 *     A cell (r,c) can be deduced by propagation when:
 *       min(maxVal, rowRemaining[r], colRemaining[c]) == 1  (only one value fits), OR
 *       rowEmpties[r] == 1 (this cell is the only empty in its row, value is forced), OR
 *       colEmpties[c] == 1 (this cell is the only empty in its column, value is forced).
 *  3. Fill all such cells with their forced values; count them as "steps".
 *  4. Repeat until no more cells can be filled by propagation.
 *  5. Return the total number of cells resolved this way.
 *
 * Algorithm for calibrate:
 *  Compute the propagation ratio = steps / emptyCells.
 *  Map to Difficulty using empirically sensible thresholds:
 *   - Size 3 → BEGINNER (the only 3x3 difficulty)
 *   - Size 4 → EASY
 *   - Size 5:
 *       ratio >= 0.60 → EASY (but EASY is 4x4 so fall back to MEDIUM minimum)
 *       actual: ratio >= 0.60 → MEDIUM (nearly all propagation), else MEDIUM
 *       (all 5x5 puzzles map to MEDIUM; this is the only 5-size tier)
 *   In practice each Difficulty owns a unique grid size, so calibration by
 *   size alone is reliable. The propagation ratio is used as a secondary
 *   tie-breaker when sizes overlap in future extensions.
 */
class DifficultyCalibrator {

    /**
     * Counts the number of cells that can be uniquely determined by constraint
     * propagation alone (without any backtracking/guessing).
     */
    fun countSolveSteps(puzzle: Puzzle): Int {
        val n = puzzle.size
        val maxVal = puzzle.difficulty.maxVal

        // Working copy of the grid (0 = empty, >0 = filled)
        val work = Array(n) { r -> IntArray(n) { c -> puzzle.cells[r][c].value } }

        // Mutable remaining sums and empty-cell counts per row/col
        val rowRemaining = IntArray(n) { r -> puzzle.rowTargets[r] - work[r].sum() }
        val colRemaining = IntArray(n) { c -> puzzle.colTargets[c] - (0 until n).sumOf { r -> work[r][c] } }
        val rowEmpties = IntArray(n) { r -> work[r].count { it == 0 } }
        val colEmpties = IntArray(n) { c -> (0 until n).count { r -> work[r][c] == 0 } }

        var totalSteps = 0

        // Keep propagating until no more forced cells are found in a full pass
        var changed = true
        while (changed) {
            changed = false
            for (r in 0 until n) {
                for (c in 0 until n) {
                    if (work[r][c] != 0) continue  // already filled

                    val forcedValue = deduceCell(r, c, rowRemaining, colRemaining, rowEmpties, colEmpties, maxVal)
                        ?: continue

                    // Fill the cell
                    work[r][c] = forcedValue
                    rowRemaining[r] -= forcedValue
                    colRemaining[c] -= forcedValue
                    rowEmpties[r]--
                    colEmpties[c]--
                    totalSteps++
                    changed = true
                }
            }
        }

        return totalSteps
    }

    /**
     * Maps a puzzle to the Difficulty tier that best matches its intrinsic
     * difficulty based on grid size and propagation characteristics.
     *
     * Since each current Difficulty maps 1-to-1 to a grid size, the primary
     * discriminator is puzzle.size. The propagation ratio is computed but
     * reserved for future multi-tier same-size extensions.
     */
    fun calibrate(puzzle: Puzzle): Difficulty {
        // Primary: match by grid size — each Difficulty owns a unique size
        val bySize = Difficulty.entries.firstOrNull { it.size == puzzle.size }
        if (bySize != null) return bySize

        // Fallback: use propagation ratio to estimate difficulty
        val emptyCells = puzzle.cells.sumOf { row -> row.count { it.isEmpty } }
        if (emptyCells == 0) return Difficulty.BEGINNER  // trivially solved

        val steps = countSolveSteps(puzzle)
        val ratio = steps.toDouble() / emptyCells

        // Higher propagation ratio = fewer guesses needed = easier
        return when {
            ratio >= 0.80 -> Difficulty.BEGINNER
            ratio >= 0.50 -> Difficulty.EASY
            else          -> Difficulty.MEDIUM
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Returns the forced value for cell (r,c) if it can be uniquely determined
     * by constraint propagation, or null if it cannot.
     */
    private fun deduceCell(
        r: Int, c: Int,
        rowRemaining: IntArray, colRemaining: IntArray,
        rowEmpties: IntArray, colEmpties: IntArray,
        maxVal: Int
    ): Int? {
        // Case 1: Only one empty cell in this row — its value is forced
        if (rowEmpties[r] == 1) {
            val forced = rowRemaining[r]
            if (forced in 1..maxVal) return forced
        }

        // Case 2: Only one empty cell in this column — its value is forced
        if (colEmpties[c] == 1) {
            val forced = colRemaining[c]
            if (forced in 1..maxVal) return forced
        }

        // Case 3: The valid domain is exactly one value
        // Domain = 1 .. min(maxVal, rowRemaining[r], colRemaining[c])
        val hi = minOf(maxVal, rowRemaining[r], colRemaining[c])
        if (hi == 1) return 1

        return null
    }
}

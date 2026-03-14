package org.dgeek.sumgrid.engine

sealed class SolutionCount {
    data object None : SolutionCount()
    data object Unique : SolutionCount()
    data object Multiple : SolutionCount()
}

/**
 * Validates whether a partially filled grid has exactly one solution.
 *
 * Pure Kotlin — zero Android dependencies.
 *
 * Algorithm:
 *  1. Gather all empty cells (value == 0).
 *  2. For each empty cell compute its initial domain:
 *       valid values = 1..min(maxVal, rowRemaining, colRemaining)
 *  3. Use arc-consistency (AC-1 pass) to prune domains:
 *       if a row/col has exactly one empty cell its value is forced.
 *  4. Sort empty cells by domain size ascending (MRV heuristic).
 *  5. Backtrack with forward-checking:
 *       - assign a value to the next MRV cell
 *       - update row/col remaining sums
 *       - propagate: if any row/col with remaining empty cells has
 *         remaining sum ≤ 0 or remaining sum > maxVal * empties → prune
 *       - if second solution found, abort immediately with MULTIPLE
 */
class UniqueSolutionValidator {

    fun validate(
        grid: Array<IntArray>,
        rowTargets: IntArray,
        colTargets: IntArray,
        maxVal: Int
    ): SolutionCount {
        val n = grid.size
        if (n == 0) return SolutionCount.None

        // Working copies so we don't mutate the caller's arrays
        val work = Array(n) { r -> grid[r].copyOf() }

        // Compute initial row/col remaining sums and empty-cell counts
        val rowRemaining = IntArray(n) { r -> rowTargets[r] - work[r].sum() }
        val colRemaining = IntArray(n) { c -> colTargets[c] - (0 until n).sumOf { r -> work[r][c] } }
        val rowEmpties   = IntArray(n) { r -> work[r].count { it == 0 } }
        val colEmpties   = IntArray(n) { c -> (0 until n).count { r -> work[r][c] == 0 } }

        // Sanity check: any negative remaining or filled row/col with wrong sum?
        for (r in 0 until n) {
            if (rowEmpties[r] == 0 && rowRemaining[r] != 0) return SolutionCount.None
            if (rowRemaining[r] < 0) return SolutionCount.None
        }
        for (c in 0 until n) {
            if (colEmpties[c] == 0 && colRemaining[c] != 0) return SolutionCount.None
            if (colRemaining[c] < 0) return SolutionCount.None
        }

        // Collect empty cells
        val emptyCells = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until n)
            for (c in 0 until n)
                if (work[r][c] == 0) emptyCells.add(r to c)

        if (emptyCells.isEmpty()) {
            // Fully filled and passed sanity above → unique (only one assignment)
            return SolutionCount.Unique
        }

        // Count: we stop counting as soon as we reach 2
        var solutionCount = 0

        // Backtracking search
        fun backtrack(index: Int): Unit {
            if (solutionCount >= 2) return     // early exit

            if (index == emptyCells.size) {
                // Verify all remaining sums are zero (should be, but double-check)
                for (r in 0 until n) if (rowRemaining[r] != 0) return
                for (c in 0 until n) if (colRemaining[c] != 0) return
                solutionCount++
                return
            }

            // MRV: pick the unassigned cell (among index..end) with smallest domain
            var bestIdx   = index
            var bestCount = Int.MAX_VALUE
            for (i in index until emptyCells.size) {
                val (r, c) = emptyCells[i]
                // Domain upper bound = min(maxVal, rowRemaining[r], colRemaining[c])
                // Domain lower bound = 1  (only positive integers)
                val hi = minOf(maxVal, rowRemaining[r], colRemaining[c])
                val domainSize = if (hi < 1) 0 else hi   // values 1..hi
                if (domainSize < bestCount) {
                    bestCount = domainSize
                    bestIdx   = i
                }
                if (bestCount == 0) break  // no point looking further
            }

            // Swap chosen cell to current index position
            if (bestIdx != index) {
                val tmp = emptyCells[bestIdx]
                emptyCells[bestIdx] = emptyCells[index]
                emptyCells[index] = tmp
            }

            val (r, c) = emptyCells[index]

            // Iterate over domain 1..hi
            val hi = minOf(maxVal, rowRemaining[r], colRemaining[c])
            if (hi < 1) {
                // No valid value — undo swap and return (dead end)
                if (bestIdx != index) {
                    val tmp = emptyCells[bestIdx]
                    emptyCells[bestIdx] = emptyCells[index]
                    emptyCells[index] = tmp
                }
                return
            }

            for (v in 1..hi) {
                if (solutionCount >= 2) break

                // Check forward feasibility:
                // After assigning v, the row/col remaining decrease.
                // Remaining empties in row r (beyond this cell) must still be fillable.
                val newRowRem = rowRemaining[r] - v
                val newColRem = colRemaining[c] - v
                val newRowEmp = rowEmpties[r] - 1
                val newColEmp = colEmpties[c] - 1

                // If row still has empties: remaining must be ≥ newRowEmp (each at least 1)
                //                           and ≤ newRowEmp * maxVal
                if (newRowEmp > 0 && (newRowRem < newRowEmp || newRowRem > newRowEmp * maxVal)) continue
                if (newRowEmp == 0 && newRowRem != 0) continue

                if (newColEmp > 0 && (newColRem < newColEmp || newColRem > newColEmp * maxVal)) continue
                if (newColEmp == 0 && newColRem != 0) continue

                // Assign
                work[r][c]     = v
                rowRemaining[r] = newRowRem
                colRemaining[c] = newColRem
                rowEmpties[r]   = newRowEmp
                colEmpties[c]   = newColEmp

                backtrack(index + 1)

                // Undo — restore the pre-assignment state explicitly
                work[r][c]      = 0
                rowRemaining[r] = newRowRem + v    // == original rowRemaining[r]
                colRemaining[c] = newColRem + v    // == original colRemaining[c]
                rowEmpties[r]   = newRowEmp + 1
                colEmpties[c]   = newColEmp + 1
            }

            // Restore swap
            if (bestIdx != index) {
                val tmp = emptyCells[bestIdx]
                emptyCells[bestIdx] = emptyCells[index]
                emptyCells[index] = tmp
            }
        }

        backtrack(0)

        return when (solutionCount) {
            0    -> SolutionCount.None
            1    -> SolutionCount.Unique
            else -> SolutionCount.Multiple
        }
    }
}

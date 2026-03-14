package org.dgeek.sumgrid.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class UniqueSolutionValidatorTest {

    private val validator = UniqueSolutionValidator()

    // ---------------------------------------------------------------------------
    // Helper to build a grid from a 2D list
    // ---------------------------------------------------------------------------
    private fun grid(vararg rows: IntArray): Array<IntArray> = arrayOf(*rows)

    // ---------------------------------------------------------------------------
    // 1. Known unique 3x3
    //
    //   Row layout (0 = empty):
    //     [2, 0, 1]  target 6  → missing cell must be 3
    //     [0, 5, 0]  target 12 → missing cells must sum to 7
    //     [4, 0, 6]  target 12 → missing cell must be 2
    //
    //   Col targets: 10, 10, 10
    //     Col 0: 2 + ? + 4 = 10 → ? = 4  [but wait…]
    //
    //   Let's hand-solve:
    //     r0: [2, x, 1]  x = 3    (col0=2, col1=x, col2=1)
    //     r1: [y, 5, z]  y+z = 7
    //     r2: [4, w, 6]  w = 2
    //
    //   Col 0: 2 + y + 4 = 10 → y = 4
    //   Col 2: 1 + z + 6 = 10 → z = 3   (and y+z=7 ✓)
    //   Col 1: 3 + 5 + 2 = 10 ✓
    //
    //   Unique solution: fill (0,1)=3, (1,0)=4, (1,2)=3, (2,1)=2
    // ---------------------------------------------------------------------------
    @Test
    fun `3x3 known unique grid returns Unique`() {
        val g = grid(
            intArrayOf(2, 0, 1),
            intArrayOf(0, 5, 0),
            intArrayOf(4, 0, 6)
        )
        val rowTargets = intArrayOf(6, 12, 12)
        val colTargets = intArrayOf(10, 10, 10)

        val result = validator.validate(g, rowTargets, colTargets, maxVal = 9)

        assertEquals(SolutionCount.Unique, result)
    }

    // ---------------------------------------------------------------------------
    // 2. Multiple solutions: 2x2 fully empty with symmetric targets
    //
    //   Grid: [[0,0],[0,0]]  row targets [3,3]  col targets [3,3]  maxVal=2
    //   Solutions:
    //     [[1,2],[2,1]] and [[2,1],[1,2]] → MULTIPLE
    // ---------------------------------------------------------------------------
    @Test
    fun `2x2 ambiguous grid returns Multiple`() {
        val g = grid(
            intArrayOf(0, 0),
            intArrayOf(0, 0)
        )
        val rowTargets = intArrayOf(3, 3)
        val colTargets = intArrayOf(3, 3)

        val result = validator.validate(g, rowTargets, colTargets, maxVal = 2)

        assertEquals(SolutionCount.Multiple, result)
    }

    // ---------------------------------------------------------------------------
    // 3. Over-constrained: target sum is impossible
    //
    //   Single cell row target = 10, but maxVal = 5 → no value 1..5 satisfies
    // ---------------------------------------------------------------------------
    @Test
    fun `over-constrained grid returns None`() {
        val g = grid(
            intArrayOf(0, 0),
            intArrayOf(0, 0)
        )
        // Row sums that can never be reached with maxVal=2 (max possible row sum = 4)
        val rowTargets = intArrayOf(10, 10)
        val colTargets = intArrayOf(10, 10)

        val result = validator.validate(g, rowTargets, colTargets, maxVal = 2)

        assertEquals(SolutionCount.None, result)
    }

    // ---------------------------------------------------------------------------
    // 4. Already fully filled grid with correct sums → Unique
    // ---------------------------------------------------------------------------
    @Test
    fun `fully filled correct grid returns Unique`() {
        val g = grid(
            intArrayOf(1, 2),
            intArrayOf(3, 4)
        )
        val rowTargets = intArrayOf(3, 7)
        val colTargets = intArrayOf(4, 6)

        val result = validator.validate(g, rowTargets, colTargets, maxVal = 9)

        assertEquals(SolutionCount.Unique, result)
    }

    // ---------------------------------------------------------------------------
    // 5. Fully filled grid with wrong sums → None
    // ---------------------------------------------------------------------------
    @Test
    fun `fully filled incorrect grid returns None`() {
        val g = grid(
            intArrayOf(1, 2),
            intArrayOf(3, 4)
        )
        val rowTargets = intArrayOf(5, 5)   // wrong: actual row sums are 3, 7
        val colTargets = intArrayOf(4, 6)

        val result = validator.validate(g, rowTargets, colTargets, maxVal = 9)

        assertEquals(SolutionCount.None, result)
    }

    // ---------------------------------------------------------------------------
    // 6. 4x4 unique grid
    //
    //   Grid (0 = empty):
    //     [1, 0, 3, 0]   target 10  → missing 2+4=6
    //     [0, 2, 0, 4]   target 12  → missing 3+3=6 ... let's check
    //     [3, 0, 1, 0]   target  8  → missing 2+2=4
    //     [0, 4, 0, 2]   target 10  → missing 2+2=4 ... hmm
    //
    //   Let's use a hand-crafted puzzle where unique solution is guaranteed:
    //
    //   Solution:
    //     [1, 2, 3, 4]  r=10
    //     [2, 3, 4, 1]  r=10
    //     [3, 4, 1, 2]  r=10
    //     [4, 1, 2, 3]  r=10
    //   Col sums: 10, 10, 10, 10
    //
    //   Remove cells with unique forced assignment:
    //     Row 0: [1, 2, 3, 0]  → cell(0,3) must be 4
    //     Row 1: [2, 3, 0, 1]  → cell(1,2) must be 4
    //     Row 2: [3, 4, 1, 0]  → cell(2,3) must be 2
    //     Row 3: [0, 1, 2, 3]  → cell(3,0) must be 4
    //   Verify columns:
    //     Col 3: 4+1+2+3=10 ✓ only one way since each row has exactly one empty
    // ---------------------------------------------------------------------------
    @Test
    fun `4x4 unique grid returns Unique`() {
        val g = grid(
            intArrayOf(1, 2, 3, 0),
            intArrayOf(2, 3, 0, 1),
            intArrayOf(3, 4, 1, 0),
            intArrayOf(0, 1, 2, 3)
        )
        val rowTargets = intArrayOf(10, 10, 10, 10)
        val colTargets = intArrayOf(10, 10, 10, 10)

        val result = validator.validate(g, rowTargets, colTargets, maxVal = 4)

        assertEquals(SolutionCount.Unique, result)
    }

    // ---------------------------------------------------------------------------
    // 7. 5x5 unique grid
    //
    //   Base solution (Latin square style, values 1-5):
    //     [1,2,3,4,5] r=15
    //     [2,3,4,5,1] r=15
    //     [3,4,5,1,2] r=15
    //     [4,5,1,2,3] r=15
    //     [5,1,2,3,4] r=15
    //   Col sums all = 15
    //
    //   Remove one cell per row, choosing cells that each row+col forces uniquely:
    //     Remove (0,0): must be 1
    //     Remove (1,1): must be 3
    //     Remove (2,2): must be 5
    //     Remove (3,3): must be 2
    //     Remove (4,4): must be 4
    //   Each removed cell is forced by its row and column constraints.
    // ---------------------------------------------------------------------------
    @Test
    fun `5x5 unique grid returns Unique`() {
        val g = grid(
            intArrayOf(0, 2, 3, 4, 5),
            intArrayOf(2, 0, 4, 5, 1),
            intArrayOf(3, 4, 0, 1, 2),
            intArrayOf(4, 5, 1, 0, 3),
            intArrayOf(5, 1, 2, 3, 0)
        )
        val rowTargets = intArrayOf(15, 15, 15, 15, 15)
        val colTargets = intArrayOf(15, 15, 15, 15, 15)

        val result = validator.validate(g, rowTargets, colTargets, maxVal = 5)

        assertEquals(SolutionCount.Unique, result)
    }

    // ---------------------------------------------------------------------------
    // 8. Performance: 100 calls on a 5x5 unique grid complete in < 5000ms
    // ---------------------------------------------------------------------------
    @Test
    fun `100 calls on 5x5 unique grid complete within 5000ms`() {
        val g = grid(
            intArrayOf(0, 2, 3, 4, 5),
            intArrayOf(2, 0, 4, 5, 1),
            intArrayOf(3, 4, 0, 1, 2),
            intArrayOf(4, 5, 1, 0, 3),
            intArrayOf(5, 1, 2, 3, 0)
        )
        val rowTargets = intArrayOf(15, 15, 15, 15, 15)
        val colTargets = intArrayOf(15, 15, 15, 15, 15)

        val start = System.currentTimeMillis()
        repeat(100) {
            // Deep-copy grid so each call starts fresh
            val copy = Array(g.size) { r -> g[r].copyOf() }
            validator.validate(copy, rowTargets, colTargets, maxVal = 5)
        }
        val elapsed = System.currentTimeMillis() - start

        assert(elapsed < 5000) {
            "100 calls took ${elapsed}ms, expected < 5000ms"
        }
    }

    // ---------------------------------------------------------------------------
    // 9. 3x3 multiple solutions: two empty cells in same row/col that can swap
    //
    //   Solution base:
    //     [1, 2, 3]  r=6
    //     [4, 5, 6]  r=15
    //     [3, 6, 3]  r=12   (col sums: 8, 13, 12)
    //
    //   That's awkward. Let's use a simpler case:
    //
    //   Grid:
    //     [0, 0, 3]  target=6  → pair must sum to 3: (1,2) or (2,1)
    //     [2, 3, 1]  target=6
    //     [1, 3, 2]  target=6
    //   Col targets: 3, 6, 6
    //     Col 0: ?+2+1=3 → ?=0 ... 0 isn't valid
    //
    //   Cleaner approach — use a grid where two empty cells truly create ambiguity:
    //     [0, 3, 0]  target=6    cell(0,0) + cell(0,2) = 3
    //     [3, 0, 3]  target=6    cell(1,1) = 6-3-3=0 ... no
    //
    //   Simplest ambiguous case: 1x2 style — just use the 2x2 from test 2.
    //   Test 9: explicitly verify a 3x3 with two valid solutions returns Multiple.
    //
    //   Grid:
    //     [0, 1, 0]  target=4   → need a+b=3, many options
    //     [1, 0, 1]  target=4   → middle = 2
    //     [0, 1, 0]  target=4   → need c+d=3
    //   Col targets: [2, 4, 2]
    //     Col 0: a+1+c=2 → a+c=1  → only (0,0)=0... invalid
    //
    //   Let's go more carefully:
    //     Solution A: [[1,1,2],[1,2,1],[2,1,1]]  r=[4,4,4]  c=[4,4,4]
    //     Solution B: [[2,1,1],[1,2,1],[1,1,2]]  r=[4,4,4]  c=[4,4,4]
    //   Remove (0,0),(0,2),(2,0),(2,2):
    //     Grid: [[0,1,0],[1,2,1],[0,1,0]]
    //     Sol A: (0,0)=1,(0,2)=2,(2,0)=2,(2,2)=1
    //     Sol B: (0,0)=2,(0,2)=1,(2,0)=1,(2,2)=2
    //   Both satisfy row/col sums → MULTIPLE
    // ---------------------------------------------------------------------------
    @Test
    fun `3x3 ambiguous grid returns Multiple`() {
        val g = grid(
            intArrayOf(0, 1, 0),
            intArrayOf(1, 2, 1),
            intArrayOf(0, 1, 0)
        )
        val rowTargets = intArrayOf(4, 4, 4)
        val colTargets = intArrayOf(4, 4, 4)

        val result = validator.validate(g, rowTargets, colTargets, maxVal = 2)

        assertEquals(SolutionCount.Multiple, result)
    }

    // ---------------------------------------------------------------------------
    // 10. Single-cell grid, unique solution
    // ---------------------------------------------------------------------------
    @Test
    fun `1x1 single empty cell with unique value returns Unique`() {
        val g = grid(intArrayOf(0))
        val rowTargets = intArrayOf(5)
        val colTargets = intArrayOf(5)

        val result = validator.validate(g, rowTargets, colTargets, maxVal = 9)

        assertEquals(SolutionCount.Unique, result)
    }

    // ---------------------------------------------------------------------------
    // 11. Single-cell grid, no valid value (target > maxVal) → None
    // ---------------------------------------------------------------------------
    @Test
    fun `1x1 single empty cell with no valid value returns None`() {
        val g = grid(intArrayOf(0))
        val rowTargets = intArrayOf(10)
        val colTargets = intArrayOf(10)

        val result = validator.validate(g, rowTargets, colTargets, maxVal = 9)

        assertEquals(SolutionCount.None, result)
    }
}

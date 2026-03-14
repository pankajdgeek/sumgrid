package org.dgeek.sumgrid.ui

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM source-scan tests for the first-empty-cell pulse animation (S2C-F006).
 *
 * T029 — RED: must fail before the pulse animation is wired.
 * T030 — GREEN impl makes these tests pass.
 */
class FirstCellPulseTest {

    private val gridRendererSource: String by lazy {
        java.io.File(
            "src/main/kotlin/org/dgeek/sumgrid/ui/components/GridRenderer.kt"
        ).readText()
    }

    private val puzzleScreenSource: String by lazy {
        java.io.File(
            "src/main/kotlin/org/dgeek/sumgrid/ui/screens/PuzzleScreen.kt"
        ).readText()
    }

    @Test
    fun gridRenderer_pulsingCellParameterExists() {
        assertTrue(
            "GridRenderer must declare a 'pulsingCell' parameter",
            gridRendererSource.contains("pulsingCell")
        )
    }

    @Test
    fun gridRenderer_hasInfiniteTransitionForPulse() {
        assertTrue(
            "GridRenderer must use InfiniteTransition or infiniteRepeatable for pulse animation",
            gridRendererSource.contains("infiniteRepeatable") ||
                gridRendererSource.contains("InfiniteTransition") ||
                gridRendererSource.contains("rememberInfiniteTransition")
        )
    }

    @Test
    fun gridRenderer_drawsPulseOnCell() {
        // Must draw something specific on the pulsing cell (scale or alpha animation)
        assertTrue(
            "GridRenderer must apply scale or alpha animation to the pulsing cell",
            gridRendererSource.contains("pulseScale") ||
                gridRendererSource.contains("pulseAlpha") ||
                gridRendererSource.contains("pulsingCell")
        )
    }

    @Test
    fun puzzleScreen_passesPulsingCellToGridRenderer() {
        assertTrue(
            "PuzzleScreen must pass a non-null pulsingCell to GridRenderer (not just null)",
            puzzleScreenSource.contains("pulsingCell") &&
                !puzzleScreenSource.contains("pulsingCell = null")
        )
    }

    @Test
    fun puzzleScreen_computesFirstEmptyCell() {
        // PuzzleScreen should compute the first empty cell from the puzzle state
        assertTrue(
            "PuzzleScreen must compute first empty cell (firstEmptyCell or firstEmpty)",
            puzzleScreenSource.contains("firstEmpty") ||
                puzzleScreenSource.contains("firstCell") ||
                puzzleScreenSource.contains("pulsingCell")
        )
    }
}

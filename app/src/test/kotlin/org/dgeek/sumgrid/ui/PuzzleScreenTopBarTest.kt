package org.dgeek.sumgrid.ui

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests verifying PuzzleScreen TopAppBar integration.
 */
class PuzzleScreenTopBarTest {

    private val puzzleScreenSource: String by lazy {
        java.io.File("src/main/kotlin/org/dgeek/sumgrid/ui/screens/PuzzleScreen.kt").readText()
    }

    @Test
    fun puzzleScreen_hasTopAppBar() {
        assertTrue(
            "PuzzleScreen should include a TopAppBar",
            puzzleScreenSource.contains("TopAppBar")
        )
    }

    @Test
    fun puzzleScreen_hasBackArrowIcon() {
        assertTrue(
            "PuzzleScreen should use ArrowBack icon",
            puzzleScreenSource.contains("ArrowBack")
        )
    }

    @Test
    fun puzzleScreen_hasNavigateBackContentDescription() {
        assertTrue(
            "Back button should have 'Navigate back' content description",
            puzzleScreenSource.contains("Navigate back")
        )
    }

    @Test
    fun puzzleScreen_hasOnBackParam() {
        assertTrue(
            "PuzzleScreen should accept an onBack parameter",
            puzzleScreenSource.contains("onBack")
        )
    }

    @Test
    fun puzzleScreen_hasShareButton() {
        assertTrue(
            "PuzzleScreen should have a Share button",
            puzzleScreenSource.contains("Share")
        )
    }

    @Test
    fun puzzleScreen_usesShareCardGenerator() {
        assertTrue(
            "PuzzleScreen should call ShareCardGenerator.generate",
            puzzleScreenSource.contains("ShareCardGenerator.generate")
        )
    }

    // ── T009 — Tap hint source-scan tests ────────────────────────────────────

    @Test
    fun puzzleScreen_hasTapHintText() {
        assertTrue(
            "PuzzleScreen must show 'Tap an empty cell to start' hint",
            puzzleScreenSource.contains("Tap an empty cell to start")
        )
    }

    @Test
    fun puzzleScreen_hintGatedOnSelectedCell() {
        assertTrue(
            "Hint must be gated on selectedCell == null",
            puzzleScreenSource.contains("selectedCell == null")
        )
    }

    @Test
    fun puzzleScreen_hintUsesAnimatedVisibility() {
        assertTrue(
            "Hint should use AnimatedVisibility for smooth appear/disappear",
            puzzleScreenSource.contains("AnimatedVisibility")
        )
    }
}

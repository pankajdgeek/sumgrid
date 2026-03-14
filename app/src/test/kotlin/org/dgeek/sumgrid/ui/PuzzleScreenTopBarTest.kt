package org.dgeek.sumgrid.ui

import org.junit.Assert.assertFalse
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

    // ── T011 — Layout restructure source-scan tests ───────────────────────────

    @Test
    fun puzzleScreen_doesNotHaveWeightSpacer() {
        assertFalse(
            "PuzzleScreen must not have Spacer(weight(1f)) between grid and numpad",
            puzzleScreenSource.contains("Modifier.weight(1f)")
        )
    }

    @Test
    fun puzzleScreen_hasCenteredLayout() {
        assertTrue(
            "PuzzleScreen must use contentAlignment = Alignment.Center",
            puzzleScreenSource.contains("contentAlignment = Alignment.Center") ||
            puzzleScreenSource.contains("verticalArrangement = Arrangement.Center")
        )
    }

    // ── T015 — Timer in TopAppBar source-scan tests ───────────────────────────

    @Test
    fun puzzleScreen_timerIsInTopAppBarActions() {
        val topBarSection = puzzleScreenSource
            .substringAfter("TopAppBar(")
            .substringBefore("}) { innerPadding ->")
        assertTrue(
            "Timer must be in TopAppBar actions slot",
            topBarSection.contains("formatElapsed") || topBarSection.contains("elapsedSeconds")
        )
    }

    @Test
    fun puzzleScreen_noStandaloneTimerText() {
        // The timer must appear ONLY inside topBar = { ... } (the TopAppBar actions slot).
        // It must NOT appear as a standalone Text in the Scaffold body Column.
        // Verify: timer Text is inside the topBar lambda. We check by confirming
        // the Box(contentAlignment = Alignment.Center) body does not contain "formatElapsed(".
        val scaffoldBodySection = puzzleScreenSource
            .substringAfter("contentAlignment = Alignment.Center")
            .substringBefore("private fun formatElapsed")
        assertFalse(
            "No standalone timer Text using formatElapsed in the puzzle screen body",
            scaffoldBodySection.contains("formatElapsed(")
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

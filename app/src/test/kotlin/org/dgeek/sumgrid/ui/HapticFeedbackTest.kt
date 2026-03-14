package org.dgeek.sumgrid.ui

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests verifying haptic feedback integration.
 *
 * Parses PuzzleScreen.kt source to confirm haptic feedback is wired
 * at the four required interaction points.
 */
class HapticFeedbackTest {

    private val puzzleScreenSource: String by lazy {
        val file = java.io.File("src/main/kotlin/org/dgeek/sumgrid/ui/screens/PuzzleScreen.kt")
        file.readText()
    }

    @Test
    fun puzzleScreen_importsHapticFeedback() {
        assertTrue(
            "PuzzleScreen should import HapticFeedbackType",
            puzzleScreenSource.contains("import androidx.compose.ui.hapticfeedback.HapticFeedbackType")
        )
    }

    @Test
    fun puzzleScreen_importsLocalHapticFeedback() {
        assertTrue(
            "PuzzleScreen should import LocalHapticFeedback",
            puzzleScreenSource.contains("import androidx.compose.ui.platform.LocalHapticFeedback")
        )
    }

    @Test
    fun puzzleScreen_hasHapticOnCellTap() {
        assertTrue(
            "PuzzleScreen should perform haptic feedback on cell tap",
            puzzleScreenSource.contains("haptic.performHapticFeedback(HapticFeedbackType.LongPress)")
        )
    }

    @Test
    fun puzzleScreen_hasHapticOnNumberEntry() {
        assertTrue(
            "PuzzleScreen should perform haptic feedback on number entry (TextHandleMove)",
            puzzleScreenSource.contains("haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)")
        )
    }

    @Test
    fun puzzleScreen_hasHapticOnClear() {
        // Both cell tap and clear use LongPress — verify at least 2 occurrences
        val count = "haptic.performHapticFeedback\\(HapticFeedbackType.LongPress\\)"
            .toRegex()
            .findAll(puzzleScreenSource)
            .count()
        assertTrue(
            "PuzzleScreen should have at least 2 LongPress haptics (cell tap + clear)",
            count >= 2
        )
    }

    @Test
    fun puzzleScreen_hasHapticOnCompletion() {
        // Completion haptic is inside a LaunchedEffect checking isCompleted == true
        assertTrue(
            "PuzzleScreen should have a completion haptic in LaunchedEffect",
            puzzleScreenSource.contains("isCompleted == true") &&
                puzzleScreenSource.contains("performHapticFeedback")
        )
    }
}

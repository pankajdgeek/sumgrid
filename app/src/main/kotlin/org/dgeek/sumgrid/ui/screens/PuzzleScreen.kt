package org.dgeek.sumgrid.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.zIndex
import org.dgeek.sumgrid.ui.components.CelebrationCellWrapper
import org.dgeek.sumgrid.ui.components.GridRenderer
import org.dgeek.sumgrid.ui.components.NumberPad
import org.dgeek.sumgrid.ui.components.rememberCelebrationState
import org.dgeek.sumgrid.viewmodel.PuzzleViewModel

/**
 * Root screen composable for an active SumGrid puzzle.
 *
 * Composes:
 *  - A top timer display showing elapsed seconds.
 *  - [GridRenderer]: Canvas-drawn NxN grid with row/col sum indicators.
 *  - [NumberPad]: Bottom-anchored digit buttons.
 *  - A completion banner when [PuzzleViewModel.uiState] reports [isCompleted].
 *
 * The timer is driven by a [LaunchedEffect] coroutine that ticks every second
 * as long as the puzzle is not yet complete.
 */
@Composable
fun PuzzleScreen(
    vm: PuzzleViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by vm.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current

    // Fire haptic on puzzle completion
    LaunchedEffect(state?.isCompleted) {
        if (state?.isCompleted == true) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // Tick the elapsed timer every second while the puzzle is active
    LaunchedEffect(state?.isCompleted) {
        if (state?.isCompleted == false) {
            while (true) {
                delay(1_000)
                vm.tickTimer()
            }
        }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        val currentState = state

        if (currentState == null) {
            // No puzzle loaded yet — show placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = "Loading puzzle…",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Timer display ────────────────────────────────────────────────
            Text(
                text      = formatElapsed(currentState.elapsedSeconds),
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                textAlign = TextAlign.Center,
                style     = MaterialTheme.typography.labelLarge,
                color     = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Celebration state (drives overlay when puzzle is complete) ────
            val celebrationState = rememberCelebrationState(
                isComplete = currentState.isCompleted,
                gridSize   = currentState.puzzle.size
            )

            // ── Grid + celebration overlay ────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth()) {
                GridRenderer(
                    state     = currentState,
                    onCellTap = { row, col ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.selectCell(row, col)
                    },
                    modifier  = Modifier.fillMaxWidth()
                )

                // Celebration overlay — visible only when puzzle is complete
                if (currentState.isCompleted) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .zIndex(1f)
                            .testTag("celebration_overlay")
                    ) {
                        // The celebration state drives per-cell scale animations
                        // via CelebrationCellWrapper. For now, this overlay triggers
                        // the animation; enhanced visuals come in S03-F003.
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── Completion banner ────────────────────────────────────────────
            if (currentState.isCompleted) {
                Text(
                    text      = "Puzzle complete! \uD83C\uDF89",
                    modifier  = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    textAlign = TextAlign.Center,
                    fontSize  = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color     = MaterialTheme.colorScheme.primary
                )
            }

            // ── Number pad ───────────────────────────────────────────────────
            NumberPad(
                difficulty   = currentState.puzzle.difficulty,
                selectedCell = currentState.selectedCell,
                onNumberTap  = { number ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    vm.enterNumber(number)
                },
                onClearTap   = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.clearCell()
                },
                isVisible    = !currentState.isCompleted,
                modifier     = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/** Format elapsed seconds as MM:SS. */
private fun formatElapsed(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}

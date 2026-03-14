package org.dgeek.sumgrid.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.zIndex
import org.dgeek.sumgrid.share.ShareCardGenerator
import org.dgeek.sumgrid.share.shareResult
import org.dgeek.sumgrid.ui.components.CelebrationCellWrapper
import org.dgeek.sumgrid.ui.components.GridRenderer
import org.dgeek.sumgrid.ui.components.NumberPad
import org.dgeek.sumgrid.ui.components.rememberCelebrationState
import org.dgeek.sumgrid.viewmodel.PuzzleViewModel

/**
 * Root screen composable for an active SumGrid puzzle.
 *
 * Composes:
 *  - A TopAppBar with difficulty title and elapsed timer in the trailing actions slot.
 *  - [GridRenderer]: Canvas-drawn NxN grid with row/col sum indicators.
 *  - [NumberPad]: Digit buttons grouped with the grid in a centered layout.
 *  - A completion banner when [PuzzleViewModel.uiState] reports [isCompleted].
 *
 * The timer is driven by a [LaunchedEffect] coroutine that ticks every second
 * as long as the puzzle is not yet complete.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PuzzleScreen(
    vm: PuzzleViewModel = viewModel(),
    onBack: (() -> Unit)? = null,
    puzzleDate: java.time.LocalDate? = null,
    modifier: Modifier = Modifier
) {
    val state by vm.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state?.puzzle?.difficulty?.name
                            ?.lowercase()
                            ?.replaceFirstChar { it.uppercaseChar() }
                            ?: ""
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Navigate back"
                            )
                        }
                    }
                },
                actions = {
                    val currentState = state
                    if (currentState != null) {
                        Text(
                            text = formatElapsed(currentState.elapsedSeconds),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0f)
                )
            )
        }
    ) { innerPadding ->
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

        // Outer box fills screen and centers grid+numpad group vertically
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            // Celebration state (drives overlay when puzzle is complete)
            val celebrationState = rememberCelebrationState(
                isComplete = currentState.isCompleted,
                gridSize   = currentState.puzzle.size
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // ── Grid + celebration overlay ──────────────────────────────
                // Compute first empty (non-given, value == 0) cell for onboarding pulse.
                // Stop pulsing once the player has made their first move (selectedCell != null
                // acts as a proxy for "interaction started").
                val firstEmptyCell: Pair<Int, Int>? = if (currentState.selectedCell == null && !vm.timerStarted) {
                    val n = currentState.puzzle.size
                    var found: Pair<Int, Int>? = null
                    outer@ for (r in 0 until n) {
                        for (c in 0 until n) {
                            if (!currentState.puzzle.cells[r][c].isGiven &&
                                currentState.userValues[r][c] == 0
                            ) {
                                found = r to c
                                break@outer
                            }
                        }
                    }
                    found
                } else {
                    null
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    GridRenderer(
                        state       = currentState,
                        onCellTap   = { row, col ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.selectCell(row, col)
                        },
                        modifier    = Modifier.fillMaxWidth(),
                        outerPaddingDp = 4f,
                        pulsingCell = firstEmptyCell
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

                // ── Tap hint — visible when no cell is selected ─────────────
                AnimatedVisibility(
                    visible = currentState.selectedCell == null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = "Tap an empty cell to start",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ── Completion banner + share ────────────────────────────────
                if (currentState.isCompleted) {
                    Text(
                        text      = "Puzzle complete! \uD83C\uDF89",
                        modifier  = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize  = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color     = MaterialTheme.colorScheme.primary
                    )

                    val onShare = {
                        val date = puzzleDate ?: java.time.LocalDate.now()
                        val shareText = ShareCardGenerator.generate(
                            puzzle = currentState.puzzle,
                            elapsedMillis = vm.elapsedMillis,
                            date = date
                        )
                        shareResult(context, shareText)
                    }

                    OutlinedButton(
                        onClick = onShare,
                        modifier = Modifier
                            .semantics { contentDescription = "Share your result" }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Share")
                    }
                }

                // ── Number pad ──────────────────────────────────────────────
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
                    onUndoTap    = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        vm.undo()
                    },
                    canUndo      = vm.canUndo,
                    isVisible    = !currentState.isCompleted,
                    modifier     = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }
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

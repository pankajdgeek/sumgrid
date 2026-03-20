package org.dgeek.sumgrid.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import org.dgeek.sumgrid.viewmodel.PuzzleUiState
import org.dgeek.sumgrid.viewmodel.PuzzleViewModel

/**
 * Root screen composable for an active SumGrid puzzle.
 *
 * Adapts layout based on available space:
 *  - **Portrait**: Vertical stack — grid on top, number pad below (scrollable).
 *  - **Landscape**: Side-by-side — grid on left, number pad on right.
 *
 * @param vm                  The [PuzzleViewModel] driving this screen.
 * @param onBack              Callback for back navigation.
 * @param puzzleDate          Calendar date for share card (null for practice).
 * @param onPlayAgain         Practice mode: start a new puzzle at same difficulty.
 * @param onChangeDifficulty  Practice mode: go back to difficulty picker.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PuzzleScreen(
    vm: PuzzleViewModel = viewModel(),
    onBack: (() -> Unit)? = null,
    puzzleDate: java.time.LocalDate? = null,
    onPlayAgain: (() -> Unit)? = null,
    onChangeDifficulty: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by vm.uiState.collectAsState()
    val coinsEarned by vm.coinsEarned.collectAsState()
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

        BoxWithConstraints(
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
                .padding(innerPadding)
        ) {
            val isLandscape = maxWidth > maxHeight

            if (isLandscape) {
                LandscapePuzzleLayout(
                    currentState = currentState,
                    vm = vm,
                    coinsEarned = coinsEarned,
                    puzzleDate = puzzleDate,
                    onPlayAgain = onPlayAgain,
                    onChangeDifficulty = onChangeDifficulty,
                    haptic = haptic,
                    context = context
                )
            } else {
                PortraitPuzzleLayout(
                    currentState = currentState,
                    vm = vm,
                    coinsEarned = coinsEarned,
                    puzzleDate = puzzleDate,
                    onPlayAgain = onPlayAgain,
                    onChangeDifficulty = onChangeDifficulty,
                    haptic = haptic,
                    context = context
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Portrait layout — vertical stack with scroll
// ---------------------------------------------------------------------------

@Composable
private fun PortraitPuzzleLayout(
    currentState: PuzzleUiState,
    vm: PuzzleViewModel,
    coinsEarned: Int,
    puzzleDate: java.time.LocalDate?,
    onPlayAgain: (() -> Unit)?,
    onChangeDifficulty: (() -> Unit)?,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    context: android.content.Context
) {
    val celebrationState = rememberCelebrationState(
        isComplete = currentState.isCompleted,
        gridSize   = currentState.puzzle.size
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp)
    ) {
        // ── Grid + celebration overlay ──────────────────────────────
        GridWithOverlay(currentState, vm, haptic)

        // ── Tap hint ────────────────────────────────────────────────
        TapHint(currentState)

        // ── Completion section ──────────────────────────────────────
        CompletionSection(
            currentState = currentState,
            vm = vm,
            coinsEarned = coinsEarned,
            puzzleDate = puzzleDate,
            onPlayAgain = onPlayAgain,
            onChangeDifficulty = onChangeDifficulty,
            context = context
        )

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

// ---------------------------------------------------------------------------
// Landscape layout — side-by-side grid + controls
// ---------------------------------------------------------------------------

@Composable
private fun LandscapePuzzleLayout(
    currentState: PuzzleUiState,
    vm: PuzzleViewModel,
    coinsEarned: Int,
    puzzleDate: java.time.LocalDate?,
    onPlayAgain: (() -> Unit)?,
    onChangeDifficulty: (() -> Unit)?,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    context: android.content.Context
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Left: Grid (takes ~60% width) ───────────────────────────
        Box(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            GridWithOverlay(currentState, vm, haptic)
        }

        // ── Right: Controls (takes ~40% width) ─────────────────────
        Column(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Tap hint ────────────────────────────────────────────
            TapHint(currentState)

            // ── Completion section ──────────────────────────────────
            CompletionSection(
                currentState = currentState,
                vm = vm,
                coinsEarned = coinsEarned,
                puzzleDate = puzzleDate,
                onPlayAgain = onPlayAgain,
                onChangeDifficulty = onChangeDifficulty,
                context = context
            )

            // ── Number pad ──────────────────────────────────────────
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
                modifier     = Modifier.fillMaxWidth()
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Shared sub-components
// ---------------------------------------------------------------------------

@Composable
private fun GridWithOverlay(
    currentState: PuzzleUiState,
    vm: PuzzleViewModel,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
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

        if (currentState.isCompleted) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .zIndex(1f)
                    .testTag("celebration_overlay")
            )
        }
    }
}

@Composable
private fun TapHint(currentState: PuzzleUiState) {
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
}

@Composable
private fun CompletionSection(
    currentState: PuzzleUiState,
    vm: PuzzleViewModel,
    coinsEarned: Int,
    puzzleDate: java.time.LocalDate?,
    onPlayAgain: (() -> Unit)?,
    onChangeDifficulty: (() -> Unit)?,
    context: android.content.Context
) {
    if (!currentState.isCompleted) return

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text      = "Puzzle complete! \uD83C\uDF89",
            modifier  = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize  = 22.sp,
            fontWeight = FontWeight.Bold,
            color     = MaterialTheme.colorScheme.primary
        )

        if (vm.isPracticeMode && coinsEarned > 0) {
            Text(
                text      = "+$coinsEarned coins earned! \uD83E\uDE99",
                modifier  = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style     = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color     = MaterialTheme.colorScheme.tertiary
            )

            Button(
                onClick = { onPlayAgain?.invoke() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Play Again", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { onChangeDifficulty?.invoke() },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Change Difficulty")
            }
        } else {
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

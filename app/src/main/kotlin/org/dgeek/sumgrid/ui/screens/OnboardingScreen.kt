package org.dgeek.sumgrid.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dgeek.sumgrid.ui.components.GridRenderer
import org.dgeek.sumgrid.ui.components.NumberPad
import org.dgeek.sumgrid.viewmodel.OnboardingViewModel
import org.dgeek.sumgrid.viewmodel.PuzzleViewModel

/**
 * Onboarding screen that guides new users through a curated puzzle sequence.
 *
 * Adapts to landscape by placing grid and controls side-by-side.
 */
@Composable
fun OnboardingScreen(
    onboardingViewModel: OnboardingViewModel,
    puzzleViewModel: PuzzleViewModel,
    onOnboardingComplete: () -> Unit,
    isFirstLaunch: Boolean = false,
    modifier: Modifier = Modifier
) {
    val puzzleUiState by puzzleViewModel.uiState.collectAsState()
    val launchCount by onboardingViewModel.launchCount.collectAsState()
    val scope = rememberCoroutineScope()
    var rulesAcknowledged by remember { mutableStateOf(false) }

    // Tick timer while puzzle is active
    LaunchedEffect(puzzleUiState?.isCompleted) {
        if (puzzleUiState?.isCompleted == false) {
            while (true) {
                delay(1_000)
                puzzleViewModel.tickTimer()
            }
        }
    }

    // When puzzle completes, mark onboarding done and go home.
    // Onboarding is a one-time experience — completing any puzzle finishes it.
    LaunchedEffect(puzzleUiState?.isCompleted) {
        if (puzzleUiState?.isCompleted == true) {
            onboardingViewModel.markComplete()
            onOnboardingComplete()
        }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        val currentState = puzzleUiState

        if (currentState == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading puzzle…",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            return@Scaffold
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isLandscape = maxWidth > maxHeight

            // ── Skip tutorial button (top-right) ────────────────────────
            TextButton(
                onClick = {
                    scope.launch {
                        onboardingViewModel.skipOnboarding()
                        onOnboardingComplete()
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
            ) {
                Text("Skip tutorial")
            }

            if (isLandscape) {
                // ── Landscape: grid left, controls right ────────────────
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Grid
                    Box(
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        GridRenderer(
                            state = currentState,
                            onCellTap = { row, col -> puzzleViewModel.selectCell(row, col) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Right: Rules + completion + numpad
                    Column(
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Title
                        Text(
                            text = "Let's learn SumGrid",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Rules card
                        RulesCard(
                            visible = (isFirstLaunch || launchCount <= 1) && !rulesAcknowledged,
                            onAcknowledge = { rulesAcknowledged = true }
                        )

                        // Completion
                        OnboardingCompletion(
                            currentState = currentState,
                            isFirstLaunch = isFirstLaunch,
                            launchCount = launchCount,
                            onComplete = { scope.launch { onOnboardingComplete() } }
                        )

                        // Number pad
                        NumberPad(
                            difficulty = currentState.puzzle.difficulty,
                            selectedCell = currentState.selectedCell,
                            onNumberTap = { number -> puzzleViewModel.enterNumber(number) },
                            onClearTap = { puzzleViewModel.clearCell() },
                            isVisible = !currentState.isCompleted,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                // ── Portrait: vertical stack with scroll ────────────────
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Title
                    Text(
                        text = "Let's learn SumGrid",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 4.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Rules card
                    RulesCard(
                        visible = (isFirstLaunch || launchCount <= 1) && !rulesAcknowledged,
                        onAcknowledge = { rulesAcknowledged = true }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Grid
                    GridRenderer(
                        state = currentState,
                        onCellTap = { row, col -> puzzleViewModel.selectCell(row, col) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Completion
                    OnboardingCompletion(
                        currentState = currentState,
                        isFirstLaunch = isFirstLaunch,
                        launchCount = launchCount,
                        onComplete = { scope.launch { onOnboardingComplete() } }
                    )

                    // Number pad
                    NumberPad(
                        difficulty = currentState.puzzle.difficulty,
                        selectedCell = currentState.selectedCell,
                        onNumberTap = { number -> puzzleViewModel.enterNumber(number) },
                        onClearTap = { puzzleViewModel.clearCell() },
                        isVisible = !currentState.isCompleted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Sub-components
// ---------------------------------------------------------------------------

@Composable
private fun RulesCard(
    visible: Boolean,
    onAcknowledge: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "How to play SumGrid",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Fill each empty cell with a number. Every row and column must add up to the target sum shown on the edges.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onAcknowledge,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Got it")
                }
            }
        }
    }
}

@Composable
private fun OnboardingCompletion(
    currentState: org.dgeek.sumgrid.viewmodel.PuzzleUiState,
    isFirstLaunch: Boolean,
    launchCount: Int,
    onComplete: () -> Unit
) {
    if (!currentState.isCompleted) return

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Puzzle complete! \uD83C\uDF89",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            textAlign = TextAlign.Center,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "You're ready to play!",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

package org.dgeek.sumgrid.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
 * Reuses the same [GridRenderer] and [NumberPad] as [PuzzleScreen].
 *
 * Behaviour per launch:
 *  - Launch 1: Show puzzle. On completion show "Come back tomorrow…" subtitle + "Got it" button.
 *  - Launch 2: Show puzzle. On completion navigate directly to home.
 *  - Launch 3: Show puzzle. On completion call [OnboardingViewModel.markComplete] then navigate home.
 *
 * @param onboardingViewModel   Tracks launch count and completion state.
 * @param puzzleViewModel       Manages the active puzzle UI state.
 * @param onOnboardingComplete  Callback to navigate to the home screen.
 * @param isFirstLaunch         True when this is the very first app launch (launch 1).
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

    // When puzzle completes, handle navigation / completion logic
    LaunchedEffect(puzzleUiState?.isCompleted) {
        if (puzzleUiState?.isCompleted == true) {
            when {
                launchCount >= 3 -> {
                    // Final onboarding puzzle — mark complete then navigate
                    onboardingViewModel.markComplete()
                    onOnboardingComplete()
                }
                launchCount == 2 -> {
                    // Second puzzle — navigate directly home
                    onOnboardingComplete()
                }
                // launchCount == 1: show "Got it" UI, navigation handled by button
            }
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Skip tutorial button (top-right) ────────────────────────────
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

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
            // ── Screen title ─────────────────────────────────────────────────
            Text(
                text = "Let's learn SumGrid",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 4.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // ── Rules overlay (first step only) ──────────────────────────────
            AnimatedVisibility(
                visible = (isFirstLaunch || launchCount <= 1) && !rulesAcknowledged,
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
                            onClick = { rulesAcknowledged = true },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Got it")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Grid ─────────────────────────────────────────────────────────
            GridRenderer(
                state = currentState,
                onCellTap = { row, col -> puzzleViewModel.selectCell(row, col) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            // ── Completion banner ────────────────────────────────────────────
            if (currentState.isCompleted) {
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

                // Launch 1 only: show "Come back tomorrow" info + "Got it" button
                if (isFirstLaunch || launchCount == 1) {
                    Text(
                        text = "Come back tomorrow for a new puzzle.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            scope.launch { onOnboardingComplete() }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 16.dp)
                    ) {
                        Text(text = "Got it")
                    }
                }
            }

            // ── Number pad ───────────────────────────────────────────────────
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
            } // Column
        } // Box
    }
}

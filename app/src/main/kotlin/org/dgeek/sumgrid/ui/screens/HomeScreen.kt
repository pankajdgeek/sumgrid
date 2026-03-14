package org.dgeek.sumgrid.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.dgeek.sumgrid.engine.models.Difficulty
import org.dgeek.sumgrid.ui.components.DifficultySelector
import org.dgeek.sumgrid.viewmodel.HomeViewModel

/**
 * Home screen composable.
 *
 * Displays:
 *  - App title
 *  - Current streak counter
 *  - Puzzle status row (completed / available per difficulty)
 *  - Countdown timer to midnight (when new puzzles unlock)
 *  - Difficulty selector
 *  - "Play" button to start the selected puzzle
 *  - "More by dgeek" footer
 *
 * @param vm                    The [HomeViewModel] driving this screen.
 * @param onStartPuzzle         Called with the selected [Difficulty] when the Play button is tapped.
 * @param modifier              Optional modifier.
 */
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onStartPuzzle: (Difficulty) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by vm.uiState.collectAsState()
    var selectedDifficulty by remember { mutableStateOf<Difficulty?>(Difficulty.BEGINNER) }

    // Tick the countdown every second
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            vm.tickCountdown()
        }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // ── App title ──────────────────────────────────────────────────
            Text(
                text = "SumGrid",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Daily Logic Puzzle",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── Streak ────────────────────────────────────────────────────
            StreakDisplay(streak = state.currentStreak)

            Spacer(modifier = Modifier.height(24.dp))

            // ── Puzzle status per difficulty ──────────────────────────────
            Text(
                text = "Today's Puzzles",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.puzzleStatuses.forEach { status ->
                    PuzzleStatusChip(
                        label = status.difficulty.name
                            .lowercase()
                            .replaceFirstChar { it.uppercaseChar() },
                        isCompleted = status.isCompleted,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Countdown ─────────────────────────────────────────────────
            CountdownDisplay(countdown = vm.formatCountdown(state.secondsUntilMidnight))

            Spacer(modifier = Modifier.height(28.dp))

            // ── Difficulty selector ───────────────────────────────────────
            Text(
                text = "Choose Difficulty",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            DifficultySelector(
                selectedDifficulty = selectedDifficulty,
                onDifficultySelected = { selectedDifficulty = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Play button ───────────────────────────────────────────────
            Button(
                onClick = {
                    selectedDifficulty?.let { onStartPuzzle(it) }
                },
                enabled = selectedDifficulty != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Play",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── Footer ─────────────────────────────────────────────────────
            Text(
                text = "More by dgeek",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Sub-components
// ---------------------------------------------------------------------------

@Composable
private fun StreakDisplay(streak: Int, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "\uD83D\uDD25",  // fire emoji
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = " $streak",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = if (streak == 1) "  day streak" else "  days streak",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun PuzzleStatusChip(
    label: String,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isCompleted) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isCompleted) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val icon = if (isCompleted) "\u2713" else "\u25CB"  // checkmark vs circle

    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor
            )
        }
    }
}

@Composable
private fun CountdownDisplay(countdown: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Next puzzle in",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = countdown,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

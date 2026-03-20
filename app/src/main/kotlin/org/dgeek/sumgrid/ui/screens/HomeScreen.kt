package org.dgeek.sumgrid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.dgeek.sumgrid.engine.models.Difficulty
import org.dgeek.sumgrid.streak.StreakBadge
import org.dgeek.sumgrid.ui.components.BadgeDetailBottomSheet
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
    onOpenStats: () -> Unit = {},
    onStartPractice: (Difficulty) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by vm.uiState.collectAsState()
    var selectedDifficulty by remember { mutableStateOf<Difficulty?>(Difficulty.BEGINNER) }
    var selectedBadge by remember { mutableStateOf<StreakBadge?>(null) }

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

        // Center content and cap width on wide screens (landscape/tablet)
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
            contentAlignment = Alignment.TopCenter
        ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
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

            Spacer(modifier = Modifier.height(12.dp))

            // ── Badges ──────────────────────────────────────────────────
            // On short screens (height <= 560dp), collapse the badge row into a single chip.
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isShortScreen = maxHeight <= 560.dp
                if (isShortScreen) {
                    // Collapsed: single "🏆 Badges" chip that opens the bottom sheet
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedBadge = StreakBadge.entries.firstOrNull() }
                    ) {
                        Text(
                            text = "\uD83C\uDFC6 Badges",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                        )
                    }
                } else {
                    // Full emoji row on tall screens
                    BadgeRow(
                        earnedBadges = state.earnedBadges,
                        onBadgeTap = { badge -> selectedBadge = badge }
                    )
                }
            }

            // Badge detail bottom sheet — opened when user taps a badge (S2C-F002)
            if (selectedBadge != null) {
                BadgeDetailBottomSheet(
                    badge = selectedBadge!!,
                    earned = selectedBadge!! in state.earnedBadges,
                    onDismiss = { selectedBadge = null }
                )
            }

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
                    val abbreviatedLabel = when (status.difficulty) {
                        Difficulty.BEGINNER -> "BEG"
                        Difficulty.EASY     -> "EASY"
                        Difficulty.MEDIUM   -> "MED"
                        Difficulty.HARD     -> "HARD"
                        Difficulty.EXPERT   -> "EXP"
                    }
                    PuzzleStatusChip(
                        label = abbreviatedLabel,
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
                onDifficultySelected = { selectedDifficulty = it },
                completedDifficulties = state.puzzleStatuses
                    .filter { it.isCompleted }
                    .map { it.difficulty }
                    .toSet()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Play button or All-done state ────────────────────────────
            if (state.allComplete) {
                AllDoneSection()
            } else {
                Button(
                    onClick = {
                        selectedDifficulty?.let { onStartPuzzle(it) }
                    },
                    enabled = selectedDifficulty != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Practice Mode ──────────────────────────────────────────────
            PracticeModeSection(
                totalCoins = state.totalCoins,
                selectedDifficulty = selectedDifficulty,
                onStartPractice = onStartPractice
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
        } // Box
    }
}

// ---------------------------------------------------------------------------
// Sub-components
// ---------------------------------------------------------------------------

@Composable
private fun StreakDisplay(streak: Int, modifier: Modifier = Modifier) {
    // Scale-up animation on streak number: 1.0 -> 1.2 -> 1.0 with spring
    val streakScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 300f),
        label = "streakScale"
    )

    // Flame icon pulse: alpha 1.0 -> 0.6 -> 1.0 repeating
    val infiniteTransition = rememberInfiniteTransition(label = "flamePulse")
    val flameAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flameAlpha"
    )

    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        if (streak == 0) {
            Text(
                text = "Start your streak!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            )
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "\uD83D\uDD25",  // fire emoji
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.alpha(flameAlpha)
                )
                Text(
                    text = " $streak",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.scale(streakScale)
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

@Composable
private fun BadgeRow(
    earnedBadges: Set<StreakBadge>,
    onBadgeTap: (StreakBadge) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StreakBadge.entries.forEach { badge ->
            val earned = badge in earnedBadges
            // Build accessibility label: e.g. "Weekly Warrior — Earned"
            val earnedSuffix = if (earned) "Earned" else "Locked"
            val accessibilityLabel = "${badge.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercaseChar() }} — $earnedSuffix"
            BadgeItem(
                badge = badge,
                earned = earned,
                accessibilityLabel = accessibilityLabel,
                onClick = { onBadgeTap(badge) }
            )
        }
    }
}

@Composable
private fun BadgeItem(
    badge: StreakBadge,
    earned: Boolean,
    accessibilityLabel: String = badge.icon,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Wrap emoji in a Box with graphicsLayer: unearned badges get reduced alpha AND
        // full desaturation via ColorMatrix.setToSaturation(0f) to appear grayscale/locked.
        val badgeAlpha = if (earned) 1f else 0.38f
        val badgeColorFilter = if (earned) null else ColorFilter.colorMatrix(
            ColorMatrix().apply { setToSaturation(0f) }
        )
        Box(
            modifier = Modifier
                .alpha(badgeAlpha)
                .drawWithCache {
                    val paint = androidx.compose.ui.graphics.Paint().apply {
                        colorFilter = badgeColorFilter
                    }
                    onDrawWithContent {
                        if (badgeColorFilter != null) {
                            drawContext.canvas.saveLayer(
                                bounds = androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height),
                                paint = paint
                            )
                            drawContent()
                            drawContext.canvas.restore()
                        } else {
                            drawContent()
                        }
                    }
                }
        ) {
            Text(
                text = badge.icon,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics {
                    contentDescription = accessibilityLabel
                }
            )
        }
        // displayName text label intentionally removed — emoji-only display per S2A-F002
    }
}

@Composable
private fun PracticeModeSection(
    totalCoins: Int,
    selectedDifficulty: Difficulty?,
    onStartPractice: (Difficulty) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Practice Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "\uD83E\uDE99 $totalCoins",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Unlimited random puzzles. Earn coins!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { selectedDifficulty?.let { onStartPractice(it) } },
                enabled = selectedDifficulty != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                val reward = selectedDifficulty?.coinReward ?: 0
                Text(
                    text = "Practice (+$reward coins)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AllDoneSection(modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "All done for today! \uD83C\uDF89",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Come back tomorrow for new puzzles.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

package org.dgeek.sumgrid.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.dgeek.sumgrid.engine.models.Difficulty

/**
 * Horizontally scrollable row of difficulty selection cards: Beginner, Easy, Medium, Hard, Expert.
 *
 * Each card displays:
 *  - Difficulty label
 *  - Grid size (e.g., 3×3)
 *  - Estimated solve time
 *
 * Cards have a fixed width of 84dp so all 5 labels render without truncation.
 * Approximately 3–4 cards are visible at 320dp width; swipe reveals the rest.
 *
 * The selected card auto-scrolls into view via [LaunchedEffect].
 *
 * @param selectedDifficulty     The currently active difficulty, or null if none selected.
 * @param onDifficultySelected   Callback invoked when a card is tapped.
 * @param modifier               Optional modifier for the outer LazyRow.
 */
@Composable
fun DifficultySelector(
    selectedDifficulty: Difficulty?,
    onDifficultySelected: (Difficulty) -> Unit,
    modifier: Modifier = Modifier
) {
    val difficulties = Difficulty.entries
    val listState = rememberLazyListState()

    // Auto-scroll to keep the selected card in view whenever selection changes.
    LaunchedEffect(selectedDifficulty) {
        val index = difficulties.indexOf(selectedDifficulty)
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = 0.dp)
    ) {
        itemsIndexed(difficulties) { _, difficulty ->
            DifficultyCard(
                difficulty = difficulty,
                isSelected = difficulty == selectedDifficulty,
                onClick = { onDifficultySelected(difficulty) },
                modifier = Modifier.width(84.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Private helpers
// ---------------------------------------------------------------------------

@Composable
private fun DifficultyCard(
    difficulty: Difficulty,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = difficulty.displayLabel()
    val gridSize = "${difficulty.size}\u00D7${difficulty.size}"  // e.g. "3×3"
    val estimatedTime = difficulty.estimatedTime()
    val description = "$label, $gridSize grid, ~$estimatedTime"

    val borderWidth = if (isSelected) 2.dp else 1.5.dp
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val labelColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val subLabelColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = description
                role = Role.Button
                selected = isSelected
            }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = labelColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = gridSize,
                style = MaterialTheme.typography.bodySmall,
                color = subLabelColor
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "~$estimatedTime",
                style = MaterialTheme.typography.bodySmall,
                color = subLabelColor
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Difficulty display helpers
// ---------------------------------------------------------------------------

private fun Difficulty.displayLabel(): String = when (this) {
    Difficulty.BEGINNER -> "Beginner"
    Difficulty.EASY     -> "Easy"
    Difficulty.MEDIUM   -> "Medium"
    Difficulty.HARD     -> "Hard"
    Difficulty.EXPERT   -> "Expert"
}

private fun Difficulty.estimatedTime(): String = when (this) {
    Difficulty.BEGINNER -> "1-2 min"
    Difficulty.EASY     -> "3-5 min"
    Difficulty.MEDIUM   -> "5-10 min"
    Difficulty.HARD     -> "10-20 min"
    Difficulty.EXPERT   -> "15-30 min"
}

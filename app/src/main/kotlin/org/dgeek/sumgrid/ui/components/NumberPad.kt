package org.dgeek.sumgrid.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dgeek.sumgrid.engine.models.Difficulty

/**
 * Bottom-anchored number pad for SumGrid puzzle input.
 *
 * Shows digit buttons 1 through [difficulty.maxVal] plus clear and optional undo.
 * When maxVal >= 7, buttons split into 2 rows for comfortable touch targets.
 *
 * @param difficulty     The active puzzle difficulty (determines which numbers to show).
 * @param selectedCell   The currently selected cell, or null if nothing is selected.
 * @param onNumberTap    Callback when a number button is tapped.
 * @param onClearTap     Callback when the clear button is tapped.
 * @param onUndoTap      Optional callback for undo button. Shown when non-null.
 * @param canUndo        Whether undo is available (button disabled when false).
 * @param isVisible      Controls visibility — hidden when puzzle is complete.
 */
@Composable
fun NumberPad(
    difficulty: Difficulty,
    selectedCell: Pair<Int, Int>?,
    onNumberTap: (Int) -> Unit,
    onClearTap: () -> Unit,
    modifier: Modifier = Modifier,
    onUndoTap: (() -> Unit)? = null,
    canUndo: Boolean = false,
    isVisible: Boolean = true
) {
    if (!isVisible) return

    val hasSelection = selectedCell != null
    val maxVal = difficulty.maxVal
    val numbers = (1..maxVal).toList()

    if (maxVal >= 7) {
        // 2-row layout for larger grids
        val rows = splitNumberRange(numbers)
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            rows.forEachIndexed { index, rowNumbers ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("number_pad_row_$index"),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (number in rowNumbers) {
                        NumberButton(
                            label = number.toString(),
                            enabled = hasSelection,
                            contentDescription = "Enter $number",
                            onClick = { onNumberTap(number) },
                            modifier = Modifier
                                .weight(1f)
                                .size(48.dp)
                        )
                    }
                    // Put action buttons on the second row with visual separator
                    if (index == rows.lastIndex) {
                        if (onUndoTap != null) {
                            // Visual gap separating digits from action buttons
                            Spacer(modifier = Modifier.width(8.dp))
                            UndoButton(
                                enabled = canUndo,
                                onClick = onUndoTap,
                                modifier = Modifier.weight(1f).size(48.dp)
                            )
                        }
                        ClearButton(
                            enabled = hasSelection,
                            onClick = onClearTap,
                            modifier = Modifier.weight(1f).size(48.dp)
                        )
                    }
                }
            }
        }
    } else {
        // Single row for small grids
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .height(56.dp)
                .testTag("number_pad_row_0"),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (number in numbers) {
                NumberButton(
                    label = number.toString(),
                    enabled = hasSelection,
                    contentDescription = "Enter $number",
                    onClick = { onNumberTap(number) },
                    modifier = Modifier
                        .weight(1f)
                        .size(48.dp)
                )
            }
            if (onUndoTap != null) {
                UndoButton(
                    enabled = canUndo,
                    onClick = onUndoTap,
                    modifier = Modifier.weight(1f).size(48.dp)
                )
            }
            ClearButton(
                enabled = hasSelection,
                onClick = onClearTap,
                modifier = Modifier.weight(1f).size(48.dp)
            )
        }
    }
}

/**
 * Split numbers into two rows: first row gets ceil(n/2) items, second gets the rest.
 */
internal fun splitNumberRange(numbers: List<Int>): List<List<Int>> {
    val mid = (numbers.size + 1) / 2
    return listOf(numbers.take(mid), numbers.drop(mid))
}

// ---------------------------------------------------------------------------
// Private sub-components
// ---------------------------------------------------------------------------

@Composable
private fun NumberButton(
    label: String,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (enabled) MaterialTheme.colorScheme.primaryContainer
                      else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(durationMillis = 150),
        label = "numpadContainerColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
                      else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 150),
        label = "numpadContentColor"
    )
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor   = contentColor,
            disabledContainerColor = containerColor,
            disabledContentColor   = contentColor
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        modifier = modifier.semantics { this.contentDescription = contentDescription }
    ) {
        Text(
            text       = label,
            fontSize   = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ClearButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clearContentColor by animateColorAsState(
        targetValue = if (enabled) MaterialTheme.colorScheme.error
                      else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 150),
        label = "clearContentColor"
    )
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor         = clearContentColor,
            disabledContentColor = clearContentColor
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        modifier = modifier.semantics { contentDescription = "Clear cell" }
    ) {
        Text(
            text       = "\u2715",
            fontSize   = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun UndoButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor   = MaterialTheme.colorScheme.onSecondaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor   = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        modifier = modifier.semantics { contentDescription = "Undo last move" }
    ) {
        Text(
            text       = "\u21B6",
            fontSize   = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

package org.dgeek.sumgrid.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dgeek.sumgrid.engine.models.Difficulty

/**
 * Bottom-anchored number pad for SumGrid puzzle input.
 *
 * Adapts button size to available width instead of using fixed 48dp.
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

    BoxWithConstraints(modifier = modifier) {
        val horizontalPad = 16.dp  // 8dp each side
        val availableWidth = maxWidth - horizontalPad

        if (maxVal >= 7) {
            // 2-row layout for larger grids
            val rows = splitNumberRange(numbers)
            // Second row has digits + possibly undo + clear
            val secondRowCount = rows[1].size + (if (onUndoTap != null) 1 else 0) + 1
            val maxItemsPerRow = maxOf(rows[0].size, secondRowCount)
            val gapTotal = (maxItemsPerRow - 1).coerceAtLeast(0) * 4  // 4dp gaps
            val buttonSize = ((availableWidth - gapTotal.dp) / maxItemsPerRow)
                .coerceIn(32.dp, 52.dp)
            val fontSize = (buttonSize.value * 0.38f).coerceIn(12f, 20f).sp

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                rows.forEachIndexed { index, rowNumbers ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(buttonSize + 4.dp)
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
                                buttonSize = buttonSize,
                                fontSize = fontSize,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (index == rows.lastIndex) {
                            if (onUndoTap != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                UndoButton(
                                    enabled = canUndo,
                                    onClick = onUndoTap,
                                    buttonSize = buttonSize,
                                    fontSize = fontSize,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            ClearButton(
                                enabled = hasSelection,
                                onClick = onClearTap,
                                buttonSize = buttonSize,
                                fontSize = fontSize,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        } else {
            // Single row for small grids
            val totalItems = numbers.size + (if (onUndoTap != null) 1 else 0) + 1
            val gapTotal = (totalItems - 1).coerceAtLeast(0) * 4
            val buttonSize = ((availableWidth - gapTotal.dp) / totalItems)
                .coerceIn(32.dp, 56.dp)
            val fontSize = (buttonSize.value * 0.38f).coerceIn(12f, 20f).sp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .height(buttonSize + 8.dp)
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
                        buttonSize = buttonSize,
                        fontSize = fontSize,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (onUndoTap != null) {
                    UndoButton(
                        enabled = canUndo,
                        onClick = onUndoTap,
                        buttonSize = buttonSize,
                        fontSize = fontSize,
                        modifier = Modifier.weight(1f)
                    )
                }
                ClearButton(
                    enabled = hasSelection,
                    onClick = onClearTap,
                    buttonSize = buttonSize,
                    fontSize = fontSize,
                    modifier = Modifier.weight(1f)
                )
            }
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
    buttonSize: Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
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
        modifier = modifier
            .size(buttonSize)
            .semantics { this.contentDescription = contentDescription }
    ) {
        Text(
            text       = label,
            fontSize   = fontSize,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ClearButton(
    enabled: Boolean,
    onClick: () -> Unit,
    buttonSize: Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
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
        modifier = modifier
            .size(buttonSize)
            .semantics { contentDescription = "Clear cell" }
    ) {
        Text(
            text       = "\u2715",
            fontSize   = fontSize,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun UndoButton(
    enabled: Boolean,
    onClick: () -> Unit,
    buttonSize: Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
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
        modifier = modifier
            .size(buttonSize)
            .semantics { contentDescription = "Undo last move" }
    ) {
        Text(
            text       = "\u21B6",
            fontSize   = fontSize,
            fontWeight = FontWeight.Medium
        )
    }
}

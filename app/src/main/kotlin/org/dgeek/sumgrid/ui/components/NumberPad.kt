package org.dgeek.sumgrid.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dgeek.sumgrid.engine.models.Difficulty

/**
 * Bottom-anchored number pad for SumGrid puzzle input.
 *
 * Shows digit buttons 1 through [difficulty.maxVal] plus a clear ("✕") button.
 * Designed for one-handed operation — positioned at the bottom of the screen
 * within comfortable thumb reach on 360dp+ phones.
 *
 * @param difficulty     The active puzzle difficulty (determines which numbers to show).
 * @param selectedCell   The currently selected cell, or null if nothing is selected.
 * @param onNumberTap    Callback when a number button is tapped.
 * @param onClearTap     Callback when the clear button is tapped.
 * @param isVisible      Controls visibility — hidden when puzzle is complete.
 */
@Composable
fun NumberPad(
    difficulty: Difficulty,
    selectedCell: Pair<Int, Int>?,
    onNumberTap: (Int) -> Unit,
    onClearTap: () -> Unit,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    if (!isVisible) return

    val hasSelection = selectedCell != null
    val numbers = (1..difficulty.maxVal).toList()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .height(56.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Number buttons
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

        // Clear button
        ClearButton(
            enabled = hasSelection,
            onClick = onClearTap,
            modifier = Modifier
                .weight(1f)
                .size(48.dp)
        )
    }
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
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor   = MaterialTheme.colorScheme.onPrimaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor   = MaterialTheme.colorScheme.onSurfaceVariant
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
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor   = MaterialTheme.colorScheme.error,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        modifier = modifier.semantics { contentDescription = "Clear cell" }
    ) {
        Text(
            text       = "✕",
            fontSize   = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

package org.dgeek.sumgrid.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.dgeek.sumgrid.streak.StreakBadge

/**
 * A [ModalBottomSheet] that shows details about a [StreakBadge].
 *
 * Displays:
 *  - Badge emoji (full color if earned, grayscale + reduced alpha if not yet earned)
 *  - Badge display name
 *  - Required streak milestone (e.g., "Maintain a 7-day streak")
 *  - Earned / Locked status label
 *
 * @param badge      The [StreakBadge] to display.
 * @param earned     Whether the player has already earned this badge.
 * @param onDismiss  Called when the sheet is dismissed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgeDetailBottomSheet(
    badge: StreakBadge,
    earned: Boolean,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Badge emoji ───────────────────────────────────────────────────
            val badgeAlpha = if (earned) 1f else 0.38f
            val badgeColorFilter = if (earned) null else ColorFilter.colorMatrix(
                ColorMatrix().apply { setToSaturation(0f) }
            )

            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .alpha(badgeAlpha)
                    .drawWithCache {
                        val paint = androidx.compose.ui.graphics.Paint().apply {
                            colorFilter = badgeColorFilter
                        }
                        onDrawWithContent {
                            if (badgeColorFilter != null) {
                                drawContext.canvas.saveLayer(
                                    bounds = androidx.compose.ui.geometry.Rect(
                                        0f, 0f, size.width, size.height
                                    ),
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
                    style = MaterialTheme.typography.displayMedium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Badge name ────────────────────────────────────────────────────
            Text(
                text = badge.displayName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Milestone description ─────────────────────────────────────────
            val milestone = "Maintain a ${badge.requiredDays}-day streak"
            Text(
                text = milestone,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Earned / Locked status pill ───────────────────────────────────
            val statusText = if (earned) "Earned" else "Locked"
            val statusContainerColor = if (earned) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
            val statusContentColor = if (earned) {
                MaterialTheme.colorScheme.onTertiaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            androidx.compose.material3.Surface(
                color = statusContainerColor,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = statusContentColor,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }
}

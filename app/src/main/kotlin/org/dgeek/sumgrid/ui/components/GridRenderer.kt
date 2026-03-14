package org.dgeek.sumgrid.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dgeek.sumgrid.viewmodel.PuzzleUiState
import org.dgeek.sumgrid.viewmodel.SumIndicatorColor

// ---------------------------------------------------------------------------
// GridColors — theme-derived color parameters for the grid
// ---------------------------------------------------------------------------

/**
 * Holds all 9 colors used to render the SumGrid Canvas grid.
 *
 * Use [defaults] for the original hardcoded palette, or [gridColorsFromTheme]
 * inside a Composable to derive colors from [MaterialTheme.colorScheme].
 */
data class GridColors(
    val givenCellBg: Color,
    val userCellBg: Color,
    val gridLine: Color,
    val selectedBorder: Color,
    val cellText: Color,
    val givenText: Color,
    val sumGreen: Color,
    val sumRed: Color,
    val sumGray: Color
) {
    companion object {
        /** Returns a [GridColors] populated with the original SumGrid design-token palette. */
        fun defaults(): GridColors = GridColors(
            givenCellBg    = Color(0xFFDDE1FF),  // IndigoContainer90 — given cell tint
            userCellBg     = Color(0xFFFFFBFF),  // Neutral99 — user-fillable cell
            gridLine       = Color(0xFF1B1B1F),  // Neutral10 — dark grid lines
            selectedBorder = Color(0xFFEFC400),  // Amber80 — selected cell highlight
            cellText       = Color(0xFF1B1B1F),  // Neutral10 — cell number text
            givenText      = Color(0xFF0001AC),  // Indigo20 — slightly blue given numbers
            sumGreen       = Color(0xFF1B6C2E),  // semantic success green
            sumRed         = Color(0xFFBA1A1A),  // ErrorRed40
            sumGray        = Color(0xFF49454F)   // muted gray for incomplete sums
        )
    }
}

/**
 * Returns a [GridColors] derived from the current [MaterialTheme.colorScheme].
 *
 * Must be called inside a Composable context.
 */
@Composable
fun gridColorsFromTheme(): GridColors {
    val cs = MaterialTheme.colorScheme
    return GridColors(
        givenCellBg    = cs.primaryContainer,
        userCellBg     = cs.surfaceContainerHigh,
        gridLine       = cs.onSurface,
        selectedBorder = cs.secondary,
        cellText       = cs.onSurface,
        givenText      = cs.primary,
        sumGreen       = cs.tertiary,
        sumRed         = cs.error,
        sumGray        = cs.onSurfaceVariant
    )
}

/**
 * Generates an accessibility content description for a grid cell.
 * Uses 1-based row/column numbering for user-facing descriptions.
 */
internal fun cellDescription(row: Int, col: Int, value: Int, isGiven: Boolean): String {
    val valueStr = if (value == 0) "empty" else value.toString()
    val stateStr = if (isGiven) "given" else "editable"
    return "Row ${row + 1}, Column ${col + 1}, $valueStr, $stateStr"
}

/**
 * Compose Canvas-based grid renderer for a SumGrid puzzle.
 *
 * Layout:
 *   - The NxN grid fills the available width (minus [outerPaddingDp]).
 *   - Row sum targets are drawn to the right of each row.
 *   - Column sum targets are drawn below each column.
 *   - The overall height is calculated to accommodate grid + col label row.
 *
 * Tapping a non-given cell invokes [onCellTap].
 *
 * An invisible overlay of [Box] nodes is placed over the grid cells to expose
 * semantics to TalkBack. Each cell node carries a [contentDescription] of the
 * form "Row N, Column M, value|empty, given|editable".
 *
 * @param state        Current UI state snapshot. If null, nothing is drawn.
 * @param onCellTap    Callback with (row, col) when a cell is tapped.
 * @param outerPaddingDp Horizontal padding outside the grid (in dp).
 */
@Composable
fun GridRenderer(
    state: PuzzleUiState,
    onCellTap: (row: Int, col: Int) -> Unit,
    modifier: Modifier = Modifier,
    outerPaddingDp: Float = 16f,
    // When non-null, an infinite scale/alpha pulse is drawn on this cell as an onboarding cue
    pulsingCell: Pair<Int, Int>? = null
) {
    val textMeasurer = rememberTextMeasurer()
    val colors = gridColorsFromTheme()
    val n = state.puzzle.size

    // Animate selection scale: spring pulse 1.0 -> 1.05 -> 1.0 over ~200ms
    val hasSelection = state.selectedCell != null
    val selectionScale by animateFloatAsState(
        targetValue = if (hasSelection) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 800f),
        label = "selectionScale"
    )

    // Pulse animation for first-empty-cell onboarding cue (S2C-F006)
    val pulseTransition = rememberInfiniteTransition(label = "firstCellPulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = outerPaddingDp.dp)
    ) {
        // Calculate cell size in dp for overlay positioning
        val cellSizeDp: Dp = maxWidth / (n + 0.6f)

        // Reserve space: grid is NxN cells; below add one extra row for col-sum labels.
        // aspectRatio = (n + label fraction) columns wide : (n + label fraction) rows tall
        // We use a square canvas and let Canvas handle the maths.
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                // Aspect ratio: width / height. We leave 10% extra height for col labels.
                .aspectRatio(n.toFloat() / (n + 1).toFloat())
                .pointerInput(state) {
                    detectTapGestures { offset ->
                        val cellSize = size.width.toFloat() / (n + 0.6f)  // cell + label gutter
                        val tapRow = (offset.y / cellSize).toInt()
                        val tapCol = (offset.x / cellSize).toInt()
                        if (tapRow in 0 until n && tapCol in 0 until n) {
                            onCellTap(tapRow, tapCol)
                        }
                    }
                }
        ) {
            val totalWidth = size.width
            // Divide width into n cells + a right gutter for row sums
            val gutterFraction = 0.6f
            val cellSize = totalWidth / (n + gutterFraction)

            drawGrid(
                state = state,
                n = n,
                cellSize = cellSize,
                textMeasurer = textMeasurer,
                colors = colors,
                selectionScale = selectionScale,
                pulsingCell = pulsingCell,
                pulseScale = pulseScale,
                pulseAlpha = pulseAlpha
            )
        }

        // Accessibility overlay — invisible Box nodes for TalkBack.
        // These have zero visual impact (alpha = 0) but expose semantics
        // so screen readers can navigate cell-by-cell.
        for (r in 0 until n) {
            for (c in 0 until n) {
                val cell = state.puzzle.cells[r][c]
                val value = state.displayValueAt(r, c)
                Box(
                    modifier = Modifier
                        .size(cellSizeDp)
                        .offset(x = cellSizeDp * c, y = cellSizeDp * r)
                        .alpha(0f)
                        .semantics {
                            contentDescription = cellDescription(r, c, value, cell.isGiven)
                        }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Drawing helpers (all called from within a DrawScope)
// ---------------------------------------------------------------------------

private fun DrawScope.drawGrid(
    state: PuzzleUiState,
    n: Int,
    cellSize: Float,
    textMeasurer: TextMeasurer,
    colors: GridColors,
    selectionScale: Float = 1f,
    pulsingCell: Pair<Int, Int>? = null,
    pulseScale: Float = 1f,
    pulseAlpha: Float = 1f
) {
    // ── Cell backgrounds ────────────────────────────────────────────────────
    for (r in 0 until n) {
        for (c in 0 until n) {
            val isGiven    = state.puzzle.cells[r][c].isGiven
            val isSelected = state.selectedCell == r to c
            val bg = when {
                isGiven    -> colors.givenCellBg
                else       -> colors.userCellBg
            }
            drawRect(
                color = bg,
                topLeft = Offset(c * cellSize, r * cellSize),
                size    = Size(cellSize, cellSize)
            )

            // Selected cell — amber border overlay with spring pulse
            if (isSelected) {
                val scaledSize = cellSize * selectionScale
                val offset = (scaledSize - cellSize) / 2f
                drawRect(
                    color = colors.selectedBorder,
                    topLeft = Offset(c * cellSize - offset, r * cellSize - offset),
                    size    = Size(scaledSize, scaledSize),
                    style   = Stroke(width = 4f)
                )
            }

            // Pulsing cell — onboarding cue for first empty cell (S2C-F006)
            if (pulsingCell != null && pulsingCell == r to c && !isSelected) {
                val scaledSize = cellSize * pulseScale
                val offset = (scaledSize - cellSize) / 2f
                drawRect(
                    color = colors.selectedBorder.copy(alpha = pulseAlpha),
                    topLeft = Offset(c * cellSize - offset, r * cellSize - offset),
                    size    = Size(scaledSize, scaledSize),
                    style   = Stroke(width = 3f)
                )
            }
        }
    }

    // ── Cell numbers ────────────────────────────────────────────────────────
    for (r in 0 until n) {
        for (c in 0 until n) {
            val value = state.displayValueAt(r, c)
            if (value != 0) {
                val isGiven = state.puzzle.cells[r][c].isGiven
                val textColor = if (isGiven) colors.givenText else colors.cellText
                drawCenteredText(
                    text        = value.toString(),
                    textColor   = textColor,
                    cellX       = c * cellSize,
                    cellY       = r * cellSize,
                    cellSize    = cellSize,
                    fontSize    = (cellSize * 0.40f).coerceIn(12f, 28f),
                    fontWeight  = if (isGiven) FontWeight.Bold else FontWeight.Normal,
                    textMeasurer = textMeasurer
                )
            }
        }
    }

    // ── Grid lines ──────────────────────────────────────────────────────────
    val gridWidth  = cellSize * n
    val gridHeight = cellSize * n
    val strokeWidth = 1.5f

    // Horizontal lines
    for (r in 0..n) {
        drawLine(
            color       = colors.gridLine,
            start       = Offset(0f, r * cellSize),
            end         = Offset(gridWidth, r * cellSize),
            strokeWidth = strokeWidth
        )
    }
    // Vertical lines
    for (c in 0..n) {
        drawLine(
            color       = colors.gridLine,
            start       = Offset(c * cellSize, 0f),
            end         = Offset(c * cellSize, gridHeight),
            strokeWidth = strokeWidth
        )
    }

    // ── Row sum labels (right of grid) ──────────────────────────────────────
    val labelFontSize = (cellSize * 0.32f).coerceIn(10f, 22f)
    for (r in 0 until n) {
        val indicator = state.rowSumIndicators[r]
        val labelColor = indicatorColor(indicator, colors)
        val labelText = formatSumLabel(state.puzzle.rowTargets[r], indicator)
        // Position: right of the last cell, centred vertically in the row
        val labelX = n * cellSize + cellSize * 0.08f
        val labelY = r * cellSize
        drawCenteredText(
            text         = labelText,
            textColor    = labelColor,
            cellX        = labelX,
            cellY        = labelY,
            cellSize     = cellSize,
            fontSize     = labelFontSize,
            fontWeight   = FontWeight.SemiBold,
            textMeasurer = textMeasurer
        )
    }

    // ── Column sum labels (below grid) ──────────────────────────────────────
    for (c in 0 until n) {
        val indicator = state.colSumIndicators[c]
        val labelColor = indicatorColor(indicator, colors)
        val labelText = formatSumLabel(state.puzzle.colTargets[c], indicator)
        val labelX = c * cellSize
        val labelY = n * cellSize + cellSize * 0.05f
        drawCenteredText(
            text         = labelText,
            textColor    = labelColor,
            cellX        = labelX,
            cellY        = labelY,
            cellSize     = cellSize,
            fontSize     = labelFontSize,
            fontWeight   = FontWeight.SemiBold,
            textMeasurer = textMeasurer
        )
    }
}

/** Draw text centred within a cell-sized rectangle at (cellX, cellY). */
private fun DrawScope.drawCenteredText(
    text: String,
    textColor: Color,
    cellX: Float,
    cellY: Float,
    cellSize: Float,
    fontSize: Float,
    fontWeight: FontWeight,
    textMeasurer: TextMeasurer
) {
    val style = TextStyle(
        color      = textColor,
        fontSize   = fontSize.sp,
        fontWeight = fontWeight
    )
    val measured = textMeasurer.measure(text, style)
    val offsetX  = cellX + (cellSize - measured.size.width) / 2f
    val offsetY  = cellY + (cellSize - measured.size.height) / 2f
    drawText(
        textLayoutResult = measured,
        topLeft          = Offset(offsetX, offsetY)
    )
}

private fun indicatorColor(indicator: SumIndicatorColor, colors: GridColors): Color = when (indicator) {
    SumIndicatorColor.GREEN -> colors.sumGreen
    SumIndicatorColor.RED   -> colors.sumRed
    SumIndicatorColor.GRAY  -> colors.sumGray
}

/**
 * Format a sum label with a colorblind-friendly symbol suffix.
 *
 * - GREEN (exact match): appends " ✓"
 * - RED (over target):   appends " ✗"
 * - GRAY (under target): no suffix
 *
 * Extracted as an `internal` pure function for unit-testability.
 */
internal fun formatSumLabel(target: Int, indicator: SumIndicatorColor): String {
    val suffix = when (indicator) {
        SumIndicatorColor.GREEN -> " ✓"
        SumIndicatorColor.RED   -> " ✗"
        SumIndicatorColor.GRAY  -> ""
    }
    return "$target$suffix"
}

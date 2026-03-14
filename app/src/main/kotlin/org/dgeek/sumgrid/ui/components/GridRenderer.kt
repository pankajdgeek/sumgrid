package org.dgeek.sumgrid.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dgeek.sumgrid.viewmodel.PuzzleUiState
import org.dgeek.sumgrid.viewmodel.SumIndicatorColor

// ---------------------------------------------------------------------------
// Color constants derived from the SumGrid design token palette
// ---------------------------------------------------------------------------

private val ColorGivenCellBg   = Color(0xFFDDE1FF)  // IndigoContainer90 — given cell tint
private val ColorUserCellBg    = Color(0xFFFFFBFF)  // Neutral99 — user-fillable cell
private val ColorGridLine      = Color(0xFF1B1B1F)  // Neutral10 — dark grid lines
private val ColorSelectedBorder = Color(0xFFEFC400) // Amber80 — selected cell highlight
private val ColorCellText      = Color(0xFF1B1B1F)  // Neutral10 — cell number text
private val ColorGivenText     = Color(0xFF0001AC)  // Indigo20 — slightly blue given numbers
private val ColorSumGreen      = Color(0xFF1B6C2E)  // semantic success green
private val ColorSumRed        = Color(0xFFBA1A1A)  // ErrorRed40
private val ColorSumGray       = Color(0xFF49454F)  // muted gray for incomplete sums

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
 * @param state        Current UI state snapshot. If null, nothing is drawn.
 * @param onCellTap    Callback with (row, col) when a cell is tapped.
 * @param outerPaddingDp Horizontal padding outside the grid (in dp).
 */
@Composable
fun GridRenderer(
    state: PuzzleUiState,
    onCellTap: (row: Int, col: Int) -> Unit,
    modifier: Modifier = Modifier,
    outerPaddingDp: Float = 16f
) {
    val textMeasurer = rememberTextMeasurer()
    val n = state.puzzle.size

    // Reserve space: grid is NxN cells; below add one extra row for col-sum labels.
    // aspectRatio = (n + label fraction) columns wide : (n + label fraction) rows tall
    // We use a square canvas and let Canvas handle the maths.
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = outerPaddingDp.dp)
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
            textMeasurer = textMeasurer
        )
    }
}

// ---------------------------------------------------------------------------
// Drawing helpers (all called from within a DrawScope)
// ---------------------------------------------------------------------------

private fun DrawScope.drawGrid(
    state: PuzzleUiState,
    n: Int,
    cellSize: Float,
    textMeasurer: TextMeasurer
) {
    // ── Cell backgrounds ────────────────────────────────────────────────────
    for (r in 0 until n) {
        for (c in 0 until n) {
            val isGiven    = state.puzzle.cells[r][c].isGiven
            val isSelected = state.selectedCell == r to c
            val bg = when {
                isGiven    -> ColorGivenCellBg
                else       -> ColorUserCellBg
            }
            drawRect(
                color = bg,
                topLeft = Offset(c * cellSize, r * cellSize),
                size    = Size(cellSize, cellSize)
            )

            // Selected cell — amber border overlay
            if (isSelected) {
                drawRect(
                    color = ColorSelectedBorder,
                    topLeft = Offset(c * cellSize, r * cellSize),
                    size    = Size(cellSize, cellSize),
                    style   = Stroke(width = 4f)
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
                val textColor = if (isGiven) ColorGivenText else ColorCellText
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
            color       = ColorGridLine,
            start       = Offset(0f, r * cellSize),
            end         = Offset(gridWidth, r * cellSize),
            strokeWidth = strokeWidth
        )
    }
    // Vertical lines
    for (c in 0..n) {
        drawLine(
            color       = ColorGridLine,
            start       = Offset(c * cellSize, 0f),
            end         = Offset(c * cellSize, gridHeight),
            strokeWidth = strokeWidth
        )
    }

    // ── Row sum labels (right of grid) ──────────────────────────────────────
    val labelFontSize = (cellSize * 0.32f).coerceIn(10f, 22f)
    for (r in 0 until n) {
        val indicator = state.rowSumIndicators[r]
        val labelColor = indicatorColor(indicator)
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
        val labelColor = indicatorColor(indicator)
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

private fun indicatorColor(indicator: SumIndicatorColor): Color = when (indicator) {
    SumIndicatorColor.GREEN -> ColorSumGreen
    SumIndicatorColor.RED   -> ColorSumRed
    SumIndicatorColor.GRAY  -> ColorSumGray
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

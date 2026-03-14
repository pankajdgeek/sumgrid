package org.dgeek.sumgrid.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Celebration animation state holder.
 *
 * Holds per-cell [Animatable] scale values and a color progress value.
 * Re-created whenever [isComplete] transitions to true.
 *
 * @param cellCount Total cells in the grid (size * size).
 */
class CelebrationState(val cellCount: Int) {
    /** Scale animatables — one per cell in reading order (row-major). */
    val scales: List<Animatable<Float, *>> = List(cellCount) { Animatable(1f) }

    /** Color progress [0..1]: 0 = original surface color, 1 = celebration green. */
    val colorProgress: Animatable<Float, *> = Animatable(0f)
}

/**
 * Returns a [CelebrationState] that animates when [isComplete] becomes true.
 *
 * Cells animate in reading order with a 50ms stagger.
 * Each cell: scale 1.0 → 1.2 → 1.0, total per-cell duration 300ms.
 * Total animation duration: stagger * cellCount + 300ms ≤ 1500ms for 5x5 (25 cells).
 *
 * Color shifts from the surface color toward [celebrationColor] over the full duration.
 *
 * @param isComplete     Trigger: animation starts when this becomes true.
 * @param gridSize       N for an NxN grid.
 * @param staggerMillis  Delay between consecutive cells (default 50ms).
 * @param celebrationColor Color to animate toward (default: green from color scheme).
 */
@Composable
fun rememberCelebrationState(
    isComplete: Boolean,
    gridSize: Int,
    staggerMillis: Int = 50,
    celebrationColor: Color? = null
): CelebrationState {
    val cellCount = gridSize * gridSize
    val state = remember(cellCount) { CelebrationState(cellCount) }
    val green = celebrationColor ?: MaterialTheme.colorScheme.tertiary

    LaunchedEffect(isComplete) {
        if (!isComplete) return@LaunchedEffect

        // Animate color progress in parallel
        state.colorProgress.snapTo(0f)
        val totalDuration = staggerMillis * cellCount + 300
        state.colorProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = totalDuration)
        )
    }

    LaunchedEffect(isComplete) {
        if (!isComplete) return@LaunchedEffect

        // Reset all scales
        state.scales.forEach { it.snapTo(1f) }

        // Launch all staggered animations in parallel inside a single coroutineScope
        // so the LaunchedEffect suspends until all cells finish.
        coroutineScope {
            for (i in 0 until cellCount) {
                val scale = state.scales[i]
                launch {
                    delay((i * staggerMillis).toLong())
                    scale.animateTo(1.2f, animationSpec = tween(durationMillis = 150))
                    scale.animateTo(1.0f, animationSpec = tween(durationMillis = 150))
                }
            }
        }
    }

    return state
}

/**
 * A single grid cell wrapper that applies the celebration scale/color animation.
 *
 * Use this to wrap each cell in [GridRenderer] when [isComplete] is true.
 *
 * @param celebrationState  State obtained from [rememberCelebrationState].
 * @param cellIndex         Row-major index of this cell (row * gridSize + col).
 * @param cellSizeDp        Visual size of the cell.
 * @param baseColor         Cell background color before animation.
 * @param celebrationColor  Cell background color at full celebration (default: tertiary).
 * @param content           Cell content composable.
 */
@Composable
fun CelebrationCellWrapper(
    celebrationState: CelebrationState,
    cellIndex: Int,
    cellSizeDp: Dp,
    baseColor: Color,
    celebrationColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scale = celebrationState.scales[cellIndex].value
    val colorT = celebrationState.colorProgress.value
    val animatedColor = lerp(baseColor, celebrationColor, colorT)

    Box(
        modifier = modifier
            .size(cellSizeDp)
            .scale(scale)
            .background(animatedColor)
    ) {
        content()
    }
}

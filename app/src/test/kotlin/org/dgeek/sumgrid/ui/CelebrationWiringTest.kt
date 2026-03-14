package org.dgeek.sumgrid.ui

import org.dgeek.sumgrid.ui.components.CelebrationState
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * JVM unit tests for [CelebrationState].
 *
 * These tests verify the plain-class invariants of CelebrationState —
 * no Compose runtime or Android framework required.
 */
class CelebrationWiringTest {

    @Test
    fun celebrationState_canBeConstructedWithCellCount() {
        val state = CelebrationState(cellCount = 9)
        assertEquals(9, state.cellCount)
    }

    @Test
    fun celebrationState_scalesListHasSizeEqualToCellCount() {
        val cellCount = 16
        val state = CelebrationState(cellCount = cellCount)
        assertEquals(cellCount, state.scales.size)
    }

    @Test
    fun celebrationState_colorProgressStartsAtZero() {
        val state = CelebrationState(cellCount = 4)
        assertEquals(0f, state.colorProgress.value, 0.0001f)
    }

    @Test
    fun celebrationState_scalesInitializedToOnePointZero() {
        val state = CelebrationState(cellCount = 25)
        for (i in state.scales.indices) {
            assertEquals(
                "scales[$i] should start at 1.0f",
                1.0f,
                state.scales[i].value,
                0.0001f
            )
        }
    }
}

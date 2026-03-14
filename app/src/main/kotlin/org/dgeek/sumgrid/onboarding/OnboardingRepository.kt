package org.dgeek.sumgrid.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.first
import org.dgeek.sumgrid.engine.models.Cell
import org.dgeek.sumgrid.engine.models.Difficulty
import org.dgeek.sumgrid.engine.models.Puzzle

/**
 * Repository that provides a curated onboarding puzzle sequence and tracks progress.
 *
 * Puzzle sequence (increasing difficulty by number of empty cells):
 *   Launch 1 → PUZZLE_1: 1 empty cell  (trivially forced)
 *   Launch 2 → PUZZLE_2: 2 empty cells (each forced by row constraint)
 *   Launch 3 → PUZZLE_3: 4 empty cells (all uniquely determined via row+col constraints)
 *   Launch 4+ → null (onboarding complete)
 *
 * All puzzles use the same 3×3 base grid:
 *   [1, 3, 2]  row targets: [6, 9, 7]
 *   [4, 2, 3]
 *   [2, 4, 1]  col targets: [7, 9, 6]
 *
 * DataStore-backed production implementation.
 * In-memory test implementation: [InMemoryOnboardingRepository].
 */
class OnboardingRepository(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val LAUNCH_COUNT = intPreferencesKey("onboarding_launch_count")
        val COMPLETE = booleanPreferencesKey("onboarding_complete")
    }

    companion object {
        private const val ONBOARDING_SEED = -1L

        // Base complete grid (all values in 1..5, BEGINNER maxVal):
        //   [1, 3, 2]   row sums: 6, 9, 7
        //   [4, 2, 3]
        //   [2, 4, 1]   col sums: 7, 9, 6
        private val ROW_TARGETS = intArrayOf(6, 9, 7)
        private val COL_TARGETS = intArrayOf(7, 9, 6)

        /**
         * Puzzle 1: 1 empty cell — [2][2] = 1 is removed.
         * row2 target=7, given 2+4=6 → must be 1. Forced.
         * col2 target=6, given 2+3=5 → must be 1. Forced.
         */
        val PUZZLE_1: Puzzle = Puzzle(
            size = 3,
            cells = arrayOf(
                arrayOf(Cell(1, true),  Cell(3, true),  Cell(2, true)),
                arrayOf(Cell(4, true),  Cell(2, true),  Cell(3, true)),
                arrayOf(Cell(2, true),  Cell(4, true),  Cell(0, false))
            ),
            rowTargets = ROW_TARGETS,
            colTargets = COL_TARGETS,
            difficulty = Difficulty.BEGINNER,
            seed = ONBOARDING_SEED
        )

        /**
         * Puzzle 2: 2 empty cells — [1][0]=4 and [2][2]=1 are removed.
         * row1 target=9, given 0+2+3=5, one empty → needs 4. Forced.
         * row2 target=7, given 2+4+0=6, one empty → needs 1. Forced.
         */
        val PUZZLE_2: Puzzle = Puzzle(
            size = 3,
            cells = arrayOf(
                arrayOf(Cell(1, true),  Cell(3, true),  Cell(2, true)),
                arrayOf(Cell(0, false), Cell(2, true),  Cell(3, true)),
                arrayOf(Cell(2, true),  Cell(4, true),  Cell(0, false))
            ),
            rowTargets = ROW_TARGETS,
            colTargets = COL_TARGETS,
            difficulty = Difficulty.BEGINNER,
            seed = ONBOARDING_SEED
        )

        /**
         * Puzzle 3: 4 empty cells — [0][0]=1, [1][0]=4, [1][2]=3, [2][2]=1 are removed.
         * row0 target=6, given 0+3+2=5, one empty [0][0] → needs 1. Forced.
         * col0 target=7, [0][0]=1 (forced), [2][0]=2 given → [1][0]=7-1-2=4. Forced.
         * row1 target=9, [1][0]=4 (forced), given 4+2+0=6 → [1][2]=3. Forced.
         * row2 target=7, given 2+4+0=6, one empty [2][2] → needs 1. Forced.
         */
        val PUZZLE_3: Puzzle = Puzzle(
            size = 3,
            cells = arrayOf(
                arrayOf(Cell(0, false), Cell(3, true),  Cell(2, true)),
                arrayOf(Cell(0, false), Cell(2, true),  Cell(0, false)),
                arrayOf(Cell(2, true),  Cell(4, true),  Cell(0, false))
            ),
            rowTargets = ROW_TARGETS,
            colTargets = COL_TARGETS,
            difficulty = Difficulty.BEGINNER,
            seed = ONBOARDING_SEED
        )

        val PUZZLES = listOf(PUZZLE_1, PUZZLE_2, PUZZLE_3)
    }

    suspend fun getLaunchCount(): Int =
        dataStore.data.first()[Keys.LAUNCH_COUNT] ?: 0

    suspend fun incrementLaunchCount() {
        dataStore.edit { prefs ->
            val current = prefs[Keys.LAUNCH_COUNT] ?: 0
            prefs[Keys.LAUNCH_COUNT] = current + 1
        }
    }

    suspend fun isComplete(): Boolean =
        dataStore.data.first()[Keys.COMPLETE] ?: false

    suspend fun markComplete() {
        dataStore.edit { prefs ->
            prefs[Keys.COMPLETE] = true
        }
    }

    /**
     * Returns the onboarding puzzle for the given [launchCount] (1-indexed),
     * or null if [launchCount] exceeds the number of onboarding puzzles or onboarding is complete.
     */
    suspend fun getPuzzleForLaunch(launchCount: Int): Puzzle? {
        if (isComplete()) return null
        return PUZZLES.getOrNull(launchCount - 1)
    }
}

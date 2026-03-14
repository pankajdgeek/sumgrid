package org.dgeek.sumgrid.share

import org.dgeek.sumgrid.engine.models.Puzzle
import java.time.LocalDate

/**
 * Generates a plain-text share card for a completed SumGrid puzzle.
 *
 * Format:
 *   SumGrid #N — Difficulty ⏱ MM:SS
 *
 *   [NxN emoji grid — ⬜ for given cells, 🟩 for user-solved cells]
 *
 *   Play free → sumgrid.dgeek.org
 *
 * No numbers are ever included — the grid uses only Unicode emoji squares,
 * keeping the share card spoiler-free.
 */
object ShareCardGenerator {

    private val EPOCH_START: LocalDate = LocalDate.of(2026, 1, 1)

    /** Unicode white large square — represents a given (pre-filled) cell. */
    private const val GIVEN_EMOJI = "\u2B1C"

    /** Unicode green square — represents a cell solved by the user. */
    private const val USER_SOLVED_EMOJI = "\uD83D\uDFE9"

    /**
     * Generates the full share card string.
     *
     * @param puzzle       The completed puzzle (cells carry isGiven flags and user values).
     * @param elapsedMillis Time taken to solve in milliseconds.
     * @param date         The calendar date the puzzle belongs to (determines day number).
     */
    fun generate(
        puzzle: Puzzle,
        elapsedMillis: Long,
        date: LocalDate
    ): String {
        val dayNumber = date.toEpochDay() - EPOCH_START.toEpochDay() + 1
        val difficultyLabel = puzzle.difficulty.name
            .lowercase()
            .replaceFirstChar { it.uppercaseChar() }
        val time = formatTime(elapsedMillis)
        val grid = buildGridString(puzzle)
        return "SumGrid #$dayNumber \u2014 $difficultyLabel \u23F1 $time\n\n$grid\n\nPlay free \u2192 sumgrid.dgeek.org"
    }

    /**
     * Builds the emoji grid string. Each cell is one emoji; rows are separated by newlines.
     * Given cells → ⬜ (white square, U+2B1C)
     * User-solved cells (not given, has a value) → 🟩 (green square, U+1F7E9)
     */
    private fun buildGridString(puzzle: Puzzle): String {
        return puzzle.cells.joinToString(separator = "\n") { row ->
            row.joinToString(separator = "") { cell ->
                if (cell.isGiven) GIVEN_EMOJI else USER_SOLVED_EMOJI
            }
        }
    }

    /**
     * Formats elapsed milliseconds as "M:SS" (e.g. 90000 → "1:30", 0 → "0:00").
     * Minutes are not zero-padded; seconds always use two digits.
     */
    fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
}

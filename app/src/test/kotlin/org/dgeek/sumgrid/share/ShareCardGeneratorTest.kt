package org.dgeek.sumgrid.share

import org.dgeek.sumgrid.engine.models.Cell
import org.dgeek.sumgrid.engine.models.Difficulty
import org.dgeek.sumgrid.engine.models.Puzzle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ShareCardGeneratorTest {

    // Helper: build a minimal 3x3 puzzle with explicit given/solved cells
    private fun makePuzzle(cellMatrix: Array<Array<Cell>>): Puzzle {
        val size = cellMatrix.size
        val rowTargets = IntArray(size) { row -> cellMatrix[row].sumOf { it.value } }
        val colTargets = IntArray(size) { col -> cellMatrix.sumOf { row -> row[col].value } }
        return Puzzle(
            size = size,
            cells = cellMatrix,
            rowTargets = rowTargets,
            colTargets = colTargets,
            difficulty = Difficulty.BEGINNER,
            seed = 0L
        )
    }

    // A 3x3 puzzle where all cells are given (no user-solved cells)
    private fun allGivenPuzzle(): Puzzle {
        val cells = Array(3) { Array(3) { Cell(value = 1, isGiven = true) } }
        return makePuzzle(cells)
    }

    // A 3x3 puzzle where first cell is user-solved, rest are given
    private fun oneUserSolvedPuzzle(): Puzzle {
        val cells = Array(3) { row ->
            Array(3) { col ->
                if (row == 0 && col == 0) Cell(value = 1, isGiven = false)
                else Cell(value = 1, isGiven = true)
            }
        }
        return makePuzzle(cells)
    }

    // --- formatTime tests ---

    @Test
    fun `formatTime zero millis returns 0 colon 00`() {
        assertEquals("0:00", ShareCardGenerator.formatTime(0L))
    }

    @Test
    fun `formatTime 90000 millis returns 1 colon 30`() {
        assertEquals("1:30", ShareCardGenerator.formatTime(90_000L))
    }

    @Test
    fun `formatTime 3661000 millis returns 61 colon 01`() {
        assertEquals("61:01", ShareCardGenerator.formatTime(3_661_000L))
    }

    @Test
    fun `formatTime 59999 millis rounds down to 0 colon 59`() {
        assertEquals("0:59", ShareCardGenerator.formatTime(59_999L))
    }

    // --- Day number calculation ---

    @Test
    fun `day number for 2026 03 14 is 73`() {
        val puzzle = allGivenPuzzle()
        val date = LocalDate.of(2026, 3, 14)
        val card = ShareCardGenerator.generate(puzzle, 60_000L, date)
        assertTrue("Expected #73 in card, got: $card", card.contains("#73"))
    }

    @Test
    fun `day number for 2026 01 01 is 1`() {
        val puzzle = allGivenPuzzle()
        val date = LocalDate.of(2026, 1, 1)
        val card = ShareCardGenerator.generate(puzzle, 60_000L, date)
        assertTrue("Expected #1 in card, got: $card", card.contains("#1"))
    }

    // --- Grid emoji tests ---

    @Test
    fun `given cell maps to white square emoji`() {
        val puzzle = allGivenPuzzle()
        val date = LocalDate.of(2026, 3, 14)
        val card = ShareCardGenerator.generate(puzzle, 60_000L, date)
        assertTrue("Expected white square (given cell) in card", card.contains("\u2B1C"))
    }

    @Test
    fun `user solved cell maps to green square emoji`() {
        val puzzle = oneUserSolvedPuzzle()
        val date = LocalDate.of(2026, 3, 14)
        val card = ShareCardGenerator.generate(puzzle, 60_000L, date)
        assertTrue("Expected green square (user-solved cell) in card", card.contains("\uD83D\uDFE9"))
    }

    @Test
    fun `all given puzzle has no green squares`() {
        val puzzle = allGivenPuzzle()
        val date = LocalDate.of(2026, 3, 14)
        val card = ShareCardGenerator.generate(puzzle, 60_000L, date)
        assertFalse("Should not contain green square", card.contains("\uD83D\uDFE9"))
    }

    @Test
    fun `grid rows contain no digit characters`() {
        val puzzle = oneUserSolvedPuzzle()
        val date = LocalDate.of(2026, 3, 14)
        val card = ShareCardGenerator.generate(puzzle, 60_000L, date)
        // Extract lines between the header and footer (the grid lines)
        val lines = card.lines()
        // Grid lines are lines 3..size+2 (0-indexed: line 0=header, line 1=blank, lines 2..N=grid)
        val gridLines = lines.drop(2).dropLast(2) // skip header+blank, drop blank+footer
        gridLines.forEach { line ->
            assertFalse("Grid line should not contain digits: '$line'", line.any { it.isDigit() })
        }
    }

    @Test
    fun `output contains play free link`() {
        val puzzle = allGivenPuzzle()
        val date = LocalDate.of(2026, 3, 14)
        val card = ShareCardGenerator.generate(puzzle, 60_000L, date)
        assertTrue("Expected play link in card", card.contains("Play free \u2192 sumgrid.dgeek.org"))
    }

    @Test
    fun `output contains difficulty name`() {
        val puzzle = allGivenPuzzle()
        val date = LocalDate.of(2026, 3, 14)
        val card = ShareCardGenerator.generate(puzzle, 60_000L, date)
        assertTrue("Expected 'Beginner' in card", card.contains("Beginner"))
    }

    @Test
    fun `output contains formatted time`() {
        val puzzle = allGivenPuzzle()
        val date = LocalDate.of(2026, 3, 14)
        val card = ShareCardGenerator.generate(puzzle, 90_000L, date)
        assertTrue("Expected time '1:30' in card", card.contains("1:30"))
    }

    @Test
    fun `grid has correct number of rows`() {
        val puzzle = allGivenPuzzle() // 3x3
        val date = LocalDate.of(2026, 3, 14)
        val card = ShareCardGenerator.generate(puzzle, 60_000L, date)
        val lines = card.lines()
        // Format: header line, blank line, N grid rows, blank line, footer
        // Total = 1 + 1 + 3 + 1 + 1 = 7
        assertEquals(7, lines.size)
    }
}

package org.dgeek.sumgrid.daily

import kotlinx.coroutines.test.runTest
import org.dgeek.sumgrid.engine.PuzzleGenerator
import org.dgeek.sumgrid.engine.UniqueSolutionValidator
import org.dgeek.sumgrid.engine.models.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [DailyPuzzleRepository].
 *
 * Uses [InMemoryCompletionStore] as a fake DataStore to avoid Android runtime.
 */
class DailyPuzzleRepositoryTest {

    private lateinit var repo: DailyPuzzleRepository
    private lateinit var store: InMemoryCompletionStore

    @Before
    fun setup() {
        val generator = PuzzleGenerator(UniqueSolutionValidator())
        store = InMemoryCompletionStore()
        repo = DailyPuzzleRepository(generator, store)
    }

    // -----------------------------------------------------------------------
    // Determinism — same date+difficulty always returns same puzzle
    // -----------------------------------------------------------------------

    @Test
    fun getPuzzleForDate_sameDateAndDifficulty_returnsSamePuzzle() = runTest {
        val date = LocalDate.of(2026, 3, 14)
        val p1 = repo.getPuzzleForDate(date, Difficulty.BEGINNER)
        val p2 = repo.getPuzzleForDate(date, Difficulty.BEGINNER)
        assertEquals(p1, p2)
    }

    @Test
    fun getPuzzleForDate_sameDateAndDifficulty_seedIsConsistent() = runTest {
        val date = LocalDate.of(2026, 3, 14)
        val p1 = repo.getPuzzleForDate(date, Difficulty.EASY)
        val p2 = repo.getPuzzleForDate(date, Difficulty.EASY)
        assertEquals(p1.seed, p2.seed)
    }

    @Test
    fun getPuzzleForDate_differentDates_returnsDifferentPuzzles() = runTest {
        val date1 = LocalDate.of(2026, 3, 14)
        val date2 = LocalDate.of(2026, 3, 15)
        val p1 = repo.getPuzzleForDate(date1, Difficulty.BEGINNER)
        val p2 = repo.getPuzzleForDate(date2, Difficulty.BEGINNER)
        assertNotEquals(p1.seed, p2.seed)
    }

    @Test
    fun getPuzzleForDate_sameDateDifferentDifficulty_returnsDifferentPuzzles() = runTest {
        val date = LocalDate.of(2026, 3, 14)
        val p1 = repo.getPuzzleForDate(date, Difficulty.BEGINNER)
        val p2 = repo.getPuzzleForDate(date, Difficulty.EASY)
        val p3 = repo.getPuzzleForDate(date, Difficulty.MEDIUM)
        // Seeds must differ because difficulty offset shifts them
        assertNotEquals(p1.seed, p2.seed)
        assertNotEquals(p2.seed, p3.seed)
        assertNotEquals(p1.seed, p3.seed)
    }

    @Test
    fun getPuzzleForDate_seedFormula_epochDayTimesThreePlusDifficultyOffset() = runTest {
        val date = LocalDate.of(2026, 3, 14)
        val epochDay = date.toEpochDay()
        val p = repo.getPuzzleForDate(date, Difficulty.BEGINNER)
        val expectedBaseSeed = epochDay * 3L + Difficulty.BEGINNER.seedOffset
        // The puzzle may have been generated with an offset if generation needed retries,
        // but the seed stored should be the base seed passed to the generator.
        assertEquals(expectedBaseSeed, p.seed)
    }

    @Test
    fun getPuzzleForDate_easyDifficulty_seedOffsetApplied() = runTest {
        val date = LocalDate.of(2026, 3, 14)
        val epochDay = date.toEpochDay()
        val p = repo.getPuzzleForDate(date, Difficulty.EASY)
        val expectedBaseSeed = epochDay * 3L + Difficulty.EASY.seedOffset
        assertEquals(expectedBaseSeed, p.seed)
    }

    @Test
    fun getPuzzleForDate_mediumDifficulty_hasCorrectSize() = runTest {
        val date = LocalDate.of(2026, 3, 14)
        val p = repo.getPuzzleForDate(date, Difficulty.MEDIUM)
        assertEquals(Difficulty.MEDIUM.size, p.size)
    }

    // -----------------------------------------------------------------------
    // Completion state round-trip
    // -----------------------------------------------------------------------

    @Test
    fun getCompletionState_noSave_returnsNull() = runTest {
        val date = LocalDate.of(2026, 3, 14)
        val state = repo.getCompletionState(date, Difficulty.BEGINNER)
        assertNull(state)
    }

    @Test
    fun saveAndGet_completionState_roundTrips() = runTest {
        val date = LocalDate.of(2026, 3, 14)
        val elapsed = 120_000L
        repo.saveCompletionState(date, Difficulty.BEGINNER, elapsed)
        val state = repo.getCompletionState(date, Difficulty.BEGINNER)
        assertNotNull(state)
        assertTrue(state!!.completed)
        assertEquals(elapsed, state.elapsedMillis)
    }

    @Test
    fun saveCompletionState_differentDays_storesSeparately() = runTest {
        val date1 = LocalDate.of(2026, 3, 14)
        val date2 = LocalDate.of(2026, 3, 15)
        repo.saveCompletionState(date1, Difficulty.BEGINNER, 60_000L)
        val s1 = repo.getCompletionState(date1, Difficulty.BEGINNER)
        val s2 = repo.getCompletionState(date2, Difficulty.BEGINNER)
        assertNotNull(s1)
        assertNull(s2)
    }

    @Test
    fun saveCompletionState_differentDifficulties_storesSeparately() = runTest {
        val date = LocalDate.of(2026, 3, 14)
        repo.saveCompletionState(date, Difficulty.BEGINNER, 90_000L)
        val beginner = repo.getCompletionState(date, Difficulty.BEGINNER)
        val easy = repo.getCompletionState(date, Difficulty.EASY)
        assertNotNull(beginner)
        assertNull(easy)
    }

    @Test
    fun saveCompletionState_completedAtIsRecorded() = runTest {
        val before = System.currentTimeMillis()
        val date = LocalDate.of(2026, 3, 14)
        repo.saveCompletionState(date, Difficulty.BEGINNER, 30_000L)
        val after = System.currentTimeMillis()
        val state = repo.getCompletionState(date, Difficulty.BEGINNER)!!
        assertTrue(state.completedAt in before..after)
    }
}

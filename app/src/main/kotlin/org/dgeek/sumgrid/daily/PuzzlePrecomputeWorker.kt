package org.dgeek.sumgrid.daily

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.dgeek.sumgrid.SumGridApplication
import org.dgeek.sumgrid.engine.models.Difficulty
import java.time.LocalDate
import java.time.ZoneId

/**
 * WorkManager worker that precomputes today's puzzles for all three difficulties.
 *
 * Puzzle generation is fast (<20ms for 5x5), so this is an opportunistic warm-up.
 * The [DailyPuzzleRepository] generates puzzles lazily on demand anyway, so if this
 * worker hasn't run yet, the first call to [DailyPuzzleRepository.getPuzzleForDate]
 * will generate on the calling coroutine without noticeable delay.
 *
 * Enqueue on app start via [enqueueOnce].
 */
class PuzzlePrecomputeWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? SumGridApplication ?: return Result.failure()
        val repo = app.container.dailyPuzzleRepository
        val today = LocalDate.now(ZoneId.systemDefault())

        // Warm up all three difficulties for today
        for (difficulty in Difficulty.entries) {
            repo.getPuzzleForDate(today, difficulty)
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "puzzle_precompute_today"

        /**
         * Enqueue a one-time precompute job. WorkManager deduplicates by unique name,
         * so calling this multiple times on the same day is safe.
         */
        fun enqueueOnce(context: Context) {
            val request = OneTimeWorkRequestBuilder<PuzzlePrecomputeWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}

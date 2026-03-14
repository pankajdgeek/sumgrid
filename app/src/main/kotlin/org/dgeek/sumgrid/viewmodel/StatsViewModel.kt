package org.dgeek.sumgrid.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dgeek.sumgrid.daily.CompletionStore
import org.dgeek.sumgrid.engine.models.Difficulty
import java.time.LocalDate

/**
 * UI state for the statistics screen.
 */
data class StatsUiState(
    val totalSolved: Int = 0,
    val countByDifficulty: Map<Difficulty, Int> = emptyMap(),
    val avgTimeMillis: Map<Difficulty, Long> = emptyMap(),
    val bestTimeMillis: Map<Difficulty, Long> = emptyMap(),
    val calendarDates: Set<LocalDate> = emptySet()
)

/**
 * ViewModel for the statistics screen.
 *
 * Reads all completion entries from [CompletionStore] and computes aggregates.
 * Pure Kotlin — no Android dependencies. Tests run on JVM.
 */
class StatsViewModel(
    private val completionStore: CompletionStore,
    private val scope: CoroutineScope
) {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    fun loadStats() {
        scope.launch {
            val all = completionStore.getAll()
            if (all.isEmpty()) {
                _uiState.value = StatsUiState()
                return@launch
            }

            val parsed = all.mapNotNull { (key, state) ->
                parseCompletionKey(key)?.let { (date, difficulty) ->
                    Triple(date, difficulty, state)
                }
            }

            val totalSolved = parsed.size

            val countByDifficulty = parsed
                .groupBy { it.second }
                .mapValues { it.value.size }

            val avgTimeMillis = parsed
                .groupBy { it.second }
                .mapValues { (_, entries) ->
                    entries.map { it.third.elapsedMillis }.average().toLong()
                }

            val bestTimeMillis = parsed
                .groupBy { it.second }
                .mapValues { (_, entries) ->
                    entries.minOf { it.third.elapsedMillis }
                }

            val calendarDates = parsed.map { it.first }.toSet()

            _uiState.value = StatsUiState(
                totalSolved = totalSolved,
                countByDifficulty = countByDifficulty,
                avgTimeMillis = avgTimeMillis,
                bestTimeMillis = bestTimeMillis,
                calendarDates = calendarDates
            )
        }
    }

    /**
     * Parse a completion key like "completion_20254_BEGINNER" into (LocalDate, Difficulty).
     */
    internal fun parseCompletionKey(key: String): Pair<LocalDate, Difficulty>? {
        // Format: completion_{epochDay}_{DIFFICULTY}
        val parts = key.split("_")
        if (parts.size != 3 || parts[0] != "completion") return null
        val epochDay = parts[1].toLongOrNull() ?: return null
        val difficulty = runCatching { Difficulty.valueOf(parts[2]) }.getOrNull() ?: return null
        return LocalDate.ofEpochDay(epochDay) to difficulty
    }
}

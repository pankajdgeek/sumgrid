package org.dgeek.sumgrid.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.dgeek.sumgrid.AppContainer
import org.dgeek.sumgrid.engine.models.Difficulty
import org.dgeek.sumgrid.ui.screens.HomeScreen
import org.dgeek.sumgrid.ui.screens.PuzzleScreen
import org.dgeek.sumgrid.viewmodel.HomeViewModel
import org.dgeek.sumgrid.viewmodel.PuzzleViewModel
import org.dgeek.sumgrid.viewmodel.ViewModelFactory
import java.time.LocalDate
import java.time.ZoneId

// ---------------------------------------------------------------------------
// HomeViewModel factory
// ---------------------------------------------------------------------------

/**
 * Factory that creates [HomeViewModel] with its required dependencies from [AppContainer].
 */
private class HomeViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(
            puzzleRepository = container.dailyPuzzleRepository,
            streakRepository = container.inMemoryStreakRepository
        ) as T
    }
}

// ---------------------------------------------------------------------------
// Route constants
// ---------------------------------------------------------------------------

private object Routes {
    const val HOME = "home"
    const val PUZZLE = "puzzle/{difficulty}"

    fun puzzle(difficulty: Difficulty): String = "puzzle/${difficulty.name}"
}

// ---------------------------------------------------------------------------
// Navigation graph
// ---------------------------------------------------------------------------

/**
 * Root navigation graph for SumGrid.
 *
 * Destinations:
 *  - **Home**: shows today's puzzle status, streak, countdown, and difficulty picker.
 *  - **Puzzle**: active puzzle for the given [Difficulty].
 *
 * Back navigation from Puzzle returns to Home automatically via the back stack.
 *
 * @param container  Application dependency container.
 * @param modifier   Optional modifier for the [NavHost].
 */
@Composable
fun SumGridNavHost(
    container: AppContainer,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val vmFactory = ViewModelFactory(container)

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier
    ) {
        // ── Home ──────────────────────────────────────────────────────────
        composable(Routes.HOME) {
            val homeVm: HomeViewModel = viewModel(
                factory = HomeViewModelFactory(container)
            )
            HomeScreen(
                vm = homeVm,
                onStartPuzzle = { difficulty ->
                    navController.navigate(Routes.puzzle(difficulty))
                }
            )
        }

        // ── Puzzle ────────────────────────────────────────────────────────
        composable(
            route = Routes.PUZZLE,
            arguments = listOf(
                navArgument("difficulty") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val difficultyName = backStackEntry.arguments?.getString("difficulty")
                ?: Difficulty.BEGINNER.name
            val difficulty = runCatching {
                Difficulty.valueOf(difficultyName)
            }.getOrDefault(Difficulty.BEGINNER)

            val puzzleVm: PuzzleViewModel = viewModel(factory = vmFactory)

            // Load the today's puzzle for the requested difficulty on first composition
            LaunchedEffect(difficulty) {
                val today = LocalDate.now(ZoneId.systemDefault())
                val puzzle = container.dailyPuzzleRepository.getPuzzleForDate(today, difficulty)
                puzzleVm.loadPuzzle(puzzle, today)
            }

            PuzzleScreen(vm = puzzleVm)
        }
    }
}

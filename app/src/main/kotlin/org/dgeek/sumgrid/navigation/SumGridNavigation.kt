package org.dgeek.sumgrid.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.dgeek.sumgrid.AppContainer
import org.dgeek.sumgrid.engine.models.Difficulty
import org.dgeek.sumgrid.ui.screens.HomeScreen
import org.dgeek.sumgrid.ui.screens.OnboardingScreen
import org.dgeek.sumgrid.ui.screens.PuzzleScreen
import org.dgeek.sumgrid.viewmodel.HomeViewModel
import org.dgeek.sumgrid.viewmodel.OnboardingViewModel
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
            streakRepository = container.streakRepository
        ) as T
    }
}

// ---------------------------------------------------------------------------
// Route constants
// ---------------------------------------------------------------------------

private object Routes {
    const val HOME = "home"
    const val PUZZLE = "puzzle/{difficulty}"
    const val ONBOARDING = "onboarding"
    /** Stats overview screen. */
    const val STATS = "stats"
    /** Practice mode with a chosen difficulty. */
    const val PRACTICE = "practice/{difficulty}"

    fun puzzle(difficulty: Difficulty): String = "puzzle/${difficulty.name}"
    fun practice(difficulty: Difficulty): String = "practice/${difficulty.name}"
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

    // Determine the start destination by checking whether onboarding is complete.
    // produceState suspends until the repository responds, then emits the route.
    val startDestination by produceState(initialValue = null as String?) {
        val onboardingComplete = container.onboardingRepository.isComplete()
        value = if (onboardingComplete) Routes.HOME else Routes.ONBOARDING
    }

    // Show nothing while we are still determining the start destination.
    val resolvedStart = startDestination ?: return

    NavHost(
        navController = navController,
        startDestination = resolvedStart,
        modifier = modifier,
        enterTransition = {
            fadeIn(tween(300)) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            fadeOut(tween(300)) + slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            fadeIn(tween(300)) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            fadeOut(tween(300)) + slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            )
        }
    ) {
        // ── Onboarding ────────────────────────────────────────────────────
        composable(Routes.ONBOARDING) {
            val onboardingVm: OnboardingViewModel = viewModel(factory = vmFactory)
            val puzzleVm: PuzzleViewModel = viewModel(factory = vmFactory)

            // Increment launch count and load the appropriate puzzle once.
            LaunchedEffect(Unit) {
                val puzzle = onboardingVm.incrementLaunchAndGetPuzzle()
                if (puzzle != null) {
                    puzzleVm.loadPuzzle(puzzle)
                } else {
                    // No puzzle available — onboarding already exhausted; go home.
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            }

            val currentLaunchCount by onboardingVm.launchCount.collectAsState()

            OnboardingScreen(
                onboardingViewModel = onboardingVm,
                puzzleViewModel = puzzleVm,
                onOnboardingComplete = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
                isFirstLaunch = (currentLaunchCount == 1)
            )
        }

        // ── Home ──────────────────────────────────────────────────────────
        composable(Routes.HOME) {
            val homeVm: HomeViewModel = viewModel(
                factory = HomeViewModelFactory(container)
            )
            HomeScreen(
                vm = homeVm,
                onStartPuzzle = { difficulty ->
                    navController.navigate(Routes.puzzle(difficulty))
                },
                onOpenStats = {
                    navController.navigate(Routes.STATS)
                },
                onStartPractice = { difficulty ->
                    navController.navigate(Routes.practice(difficulty))
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

            PuzzleScreen(
                vm = puzzleVm,
                onBack = { navController.popBackStack() },
                puzzleDate = LocalDate.now(ZoneId.systemDefault())
            )
        }

        // ── Stats ───────────────────────────────────────────────────────
        composable(Routes.STATS) {
            // TODO: Wire StatsScreen once implemented (T016)
            Text("Stats — coming soon")
        }

        // ── Practice ────────────────────────────────────────────────────
        composable(
            route = Routes.PRACTICE,
            arguments = listOf(
                navArgument("difficulty") { type = NavType.StringType }
            )
        ) {
            // TODO: Wire PracticeScreen once implemented (T020)
            Text("Practice — coming soon")
        }
    }
}

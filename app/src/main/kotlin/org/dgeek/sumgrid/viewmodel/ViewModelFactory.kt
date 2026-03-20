package org.dgeek.sumgrid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.dgeek.sumgrid.AppContainer

/**
 * [ViewModelProvider.Factory] that wires application dependencies into ViewModels.
 *
 * Usage:
 *   val factory = ViewModelFactory(app.container)
 *   val vm: PuzzleViewModel = ViewModelProvider(this, factory)[PuzzleViewModel::class.java]
 */
class ViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(PuzzleViewModel::class.java) -> {
                PuzzleViewModel(
                    completionStore = container.completionStore,
                    streakDataSource = container.streakRepository,
                    coinRepository = container.coinRepository
                ) as T
            }
            modelClass.isAssignableFrom(OnboardingViewModel::class.java) -> {
                OnboardingViewModel(
                    onboardingRepository = container.onboardingRepository
                ) as T
            }
            else -> throw IllegalArgumentException(
                "ViewModelFactory: unknown ViewModel class ${modelClass.name}"
            )
        }
    }
}

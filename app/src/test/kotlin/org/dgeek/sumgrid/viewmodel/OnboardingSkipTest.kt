package org.dgeek.sumgrid.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.dgeek.sumgrid.onboarding.InMemoryOnboardingRepository
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for skip onboarding functionality.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingSkipTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repo: InMemoryOnboardingRepository
    private lateinit var vm: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repo = InMemoryOnboardingRepository()
        vm = OnboardingViewModel(onboardingRepository = repo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun skipOnboarding_marksComplete() = runTest {
        vm.skipOnboarding()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue("Onboarding should be complete after skip", vm.isOnboardingComplete.value)
    }

    @Test
    fun skipOnboarding_persistsToRepository() = runTest {
        vm.skipOnboarding()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue("Repository should be marked complete", repo.isComplete())
    }
}

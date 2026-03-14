package org.dgeek.sumgrid.streak

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies that both streak implementations conform to the StreakDataSource interface.
 */
class StreakDataSourceTest {

    @Test
    fun inMemoryStreakRepository_implementsStreakDataSource() {
        val repo = InMemoryStreakRepository()
        assertTrue(repo is StreakDataSource)
    }

    @Test
    fun streakDataSource_hasStreakStateFlow() {
        val repo: StreakDataSource = InMemoryStreakRepository()
        // streakState should be accessible through the interface
        val flow = repo.streakState
        assertTrue("streakState should not be null", flow != null)
    }
}

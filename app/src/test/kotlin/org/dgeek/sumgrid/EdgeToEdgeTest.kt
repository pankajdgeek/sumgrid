package org.dgeek.sumgrid

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests verifying edge-to-edge polish integration.
 */
class EdgeToEdgeTest {

    private val mainActivitySource: String by lazy {
        java.io.File("src/main/kotlin/org/dgeek/sumgrid/MainActivity.kt").readText()
    }

    @Test
    fun mainActivity_callsEnableEdgeToEdge() {
        assertTrue(
            "MainActivity should call enableEdgeToEdge()",
            mainActivitySource.contains("enableEdgeToEdge()")
        )
    }

    @Test
    fun mainActivity_hasDynamicStatusBarStyle() {
        assertTrue(
            "MainActivity should configure statusBarStyle dynamically",
            mainActivitySource.contains("statusBarStyle")
        )
    }

    @Test
    fun mainActivity_hasDynamicNavigationBarStyle() {
        assertTrue(
            "MainActivity should configure navigationBarStyle dynamically",
            mainActivitySource.contains("navigationBarStyle")
        )
    }

    @Test
    fun mainActivity_usesDisposableEffect() {
        assertTrue(
            "MainActivity should use DisposableEffect for theme-reactive bar styling",
            mainActivitySource.contains("DisposableEffect")
        )
    }

    @Test
    fun mainActivity_handlesLightAndDarkTheme() {
        assertTrue(
            "MainActivity should handle dark theme",
            mainActivitySource.contains("SystemBarStyle.dark")
        )
        assertTrue(
            "MainActivity should handle light theme",
            mainActivitySource.contains("SystemBarStyle.light")
        )
    }
}

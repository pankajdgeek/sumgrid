package org.dgeek.sumgrid

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests verifying splash screen integration.
 */
class SplashScreenTest {

    private val mainActivitySource: String by lazy {
        java.io.File("src/main/kotlin/org/dgeek/sumgrid/MainActivity.kt").readText()
    }

    private val themesXml: String by lazy {
        java.io.File("src/main/res/values/themes.xml").readText()
    }

    @Test
    fun mainActivity_callsInstallSplashScreen() {
        assertTrue(
            "MainActivity should call installSplashScreen() before super.onCreate()",
            mainActivitySource.contains("installSplashScreen()")
        )
    }

    @Test
    fun mainActivity_importsSplashScreen() {
        assertTrue(
            "MainActivity should import SplashScreen",
            mainActivitySource.contains("import androidx.core.splashscreen.SplashScreen")
        )
    }

    @Test
    fun themes_hasSplashScreenTheme() {
        assertTrue(
            "themes.xml should define Theme.SplashScreen parent",
            themesXml.contains("Theme.SplashScreen")
        )
    }

    @Test
    fun themes_hasWindowSplashScreenBackground() {
        assertTrue(
            "themes.xml should set windowSplashScreenBackground",
            themesXml.contains("windowSplashScreenBackground")
        )
    }

    @Test
    fun themes_hasPostSplashScreenTheme() {
        assertTrue(
            "themes.xml should set postSplashScreenTheme",
            themesXml.contains("postSplashScreenTheme")
        )
    }
}

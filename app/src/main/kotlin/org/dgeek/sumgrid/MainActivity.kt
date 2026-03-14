package org.dgeek.sumgrid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import org.dgeek.sumgrid.navigation.SumGridNavHost
import org.dgeek.sumgrid.ui.theme.SumGridTheme

/**
 * Single-activity entry point for SumGrid.
 *
 * Hosts the [SumGridNavHost] which provides a two-destination navigation graph:
 *  - Home: today's puzzle overview, streak, countdown, difficulty picker
 *  - Puzzle: active puzzle for the selected difficulty
 *
 * Dependencies are resolved from [SumGridApplication.container].
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as SumGridApplication).container

        setContent {
            val darkTheme = isSystemInDarkTheme()

            // Dynamic status/navigation bar coloring to match theme
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    },
                    navigationBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    }
                )
                onDispose {}
            }

            SumGridTheme {
                SumGridNavHost(container = container)
            }
        }
    }
}

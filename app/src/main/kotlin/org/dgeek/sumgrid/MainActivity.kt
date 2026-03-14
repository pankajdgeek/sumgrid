package org.dgeek.sumgrid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as SumGridApplication).container

        setContent {
            SumGridTheme {
                SumGridNavHost(container = container)
            }
        }
    }
}

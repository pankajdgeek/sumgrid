package org.dgeek.sumgrid.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dgeek.sumgrid.BuildConfig
import org.dgeek.sumgrid.R
import org.dgeek.sumgrid.review.InAppReviewTrigger
import org.dgeek.sumgrid.ui.util.findActivity

/**
 * Minimal Settings screen.
 *
 * Currently exposes:
 *  - A manual "Rate SumGrid" entry that launches the Play in-app review flow
 *    (falling back to the Play Store listing if the in-app flow is unavailable).
 *  - The app version string.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    reviewTrigger: InAppReviewTrigger,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_cd),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        scope.launch {
                            context.findActivity()?.let { activity ->
                                reviewTrigger.launchManualReview(activity)
                            }
                        }
                    },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                headlineContent = {
                    Text(text = stringResource(R.string.settings_rate_app_title))
                },
                supportingContent = {
                    Text(text = stringResource(R.string.settings_rate_app_subtitle))
                },
            )

            HorizontalDivider()

            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(
                            R.string.settings_version_format,
                            BuildConfig.VERSION_NAME,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }
    }
}

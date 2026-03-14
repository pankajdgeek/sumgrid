package org.dgeek.sumgrid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.dgeek.sumgrid.ui.theme.SumGridTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SumGridTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SumGridPlaceholder(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
private fun SumGridPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "SumGrid")
    }
}

@Preview(showBackground = true)
@Composable
private fun SumGridPlaceholderPreview() {
    SumGridTheme {
        SumGridPlaceholder()
    }
}

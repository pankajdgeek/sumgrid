package org.dgeek.sumgrid.share

import android.content.Context
import android.content.Intent

/**
 * Launches the Android system share sheet with the given text.
 *
 * Designed for use from a Compose button's onClick lambda via the local [Context]:
 *
 * ```kotlin
 * val context = LocalContext.current
 * Button(onClick = { shareResult(context, shareText) }) { ... }
 * ```
 *
 * @param context   Android Context (Activity or Application).
 * @param shareText The plain-text share card string (from [ShareCardGenerator.generate]).
 */
fun shareResult(context: Context, shareText: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    val chooser = Intent.createChooser(intent, null)
    // FLAG_ACTIVITY_NEW_TASK required when launching from a non-Activity context
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}

package org.dgeek.sumgrid.navigation

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit test verifying the predictive back gesture manifest flag.
 *
 * Parses AndroidManifest.xml source to confirm that
 * android:enableOnBackInvokedCallback="true" is present.
 */
class PredictiveBackTest {

    private val manifestSource: String by lazy {
        java.io.File("src/main/AndroidManifest.xml").readText()
    }

    @Test
    fun manifest_hasEnableOnBackInvokedCallback() {
        assertTrue(
            "AndroidManifest.xml should contain enableOnBackInvokedCallback=\"true\"",
            manifestSource.contains("android:enableOnBackInvokedCallback=\"true\"")
        )
    }

    @Test
    fun manifest_callbackIsOnApplicationTag() {
        // Verify the flag is within the <application> block
        val appTagStart = manifestSource.indexOf("<application")
        val appTagEnd = manifestSource.indexOf(">", appTagStart)
        val appTag = manifestSource.substring(appTagStart, appTagEnd + 1)
        assertTrue(
            "enableOnBackInvokedCallback should be on the <application> tag",
            appTag.contains("enableOnBackInvokedCallback")
        )
    }
}

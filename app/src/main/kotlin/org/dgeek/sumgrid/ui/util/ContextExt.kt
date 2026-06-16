package org.dgeek.sumgrid.ui.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Walks the [ContextWrapper] chain to find the host [Activity], or null when
 * called from a non-Activity context (e.g. application context, ComposePreview).
 *
 * Composables receive a [Context] via `LocalContext.current`, but APIs such as
 * the Play in-app review flow require an Activity reference. This helper bridges
 * that gap without leaking an Activity into the ViewModel.
 */
fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

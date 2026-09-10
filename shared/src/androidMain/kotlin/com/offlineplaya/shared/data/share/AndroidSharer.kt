package com.offlineplaya.shared.data.share

import android.content.Context
import android.content.Intent
import com.offlineplaya.shared.domain.share.Sharer
import com.offlineplaya.shared.util.AppLogger

/**
 * Android [Sharer]: fires an `ACTION_SEND` chooser with plain text.
 *
 * Prefers the resumed Activity as the launching context (a chooser started
 * from one animates in place); falls back to the application context with
 * `FLAG_ACTIVITY_NEW_TASK` when the app is backgrounded. Reuses the same
 * [activityProvider] seam as `PlayReviewPrompter`.
 */
class AndroidSharer(
    private val appContext: Context,
    private val activityProvider: () -> android.app.Activity?,
    private val logger: AppLogger,
) : Sharer {

    override fun share(text: String): Boolean {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(send, null)
        return try {
            val activity = activityProvider()
            if (activity != null) {
                activity.startActivity(chooser)
            } else {
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                appContext.startActivity(chooser)
            }
            true
        } catch (t: Throwable) {
            logger.w(TAG, "Share chooser failed: ${t.message}")
            false
        }
    }

    private companion object {
        const val TAG = "AndroidSharer"
    }
}

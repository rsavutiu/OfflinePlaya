package com.offlineplaya.shared.data.review

import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import com.offlineplaya.shared.domain.review.ReviewPrompter
import com.offlineplaya.shared.util.AppLogger

/**
 * Google Play in-app review implementation of [ReviewPrompter].
 *
 * Notes that matter for anyone debugging "the dialog never shows":
 *  - `launchReview` is a silent no-op in debug / sideloaded builds and is
 *    quota-limited even in production — the store may show nothing despite a
 *    correct call. This is expected; verify the gating logic with the
 *    `ReviewPromptCoordinator` unit test, not by watching for the dialog.
 *  - Requires a resumed Activity ([activityProvider] returns `null` when
 *    backgrounded → we report "not launched" so the milestone is retried).
 */
class PlayReviewPrompter(
    private val activityProvider: () -> android.app.Activity?,
    private val logger: AppLogger,
) : ReviewPrompter {

    override suspend fun requestReview(): Boolean {
        val activity = activityProvider() ?: return false
        return try {
            val manager = ReviewManagerFactory.create(activity)
            val reviewInfo = manager.requestReview()
            manager.launchReview(activity, reviewInfo)
            true
        } catch (t: Throwable) {
            logger.w(TAG, "In-app review flow failed: ${t.message}")
            false
        }
    }

    private companion object {
        const val TAG = "PlayReviewPrompter"
    }
}

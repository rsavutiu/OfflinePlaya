package com.offlineplaya.android.review

import com.offlineplaya.shared.domain.review.ReviewPrompter
import com.offlineplaya.shared.util.AppLogger

/**
 * FOSS-flavour [ReviewPrompter]: there is no store review flow to launch, so
 * this always reports "not launched".
 *
 * That is the same answer the Play implementation already gives on a sideloaded
 * build, so `ReviewPromptCoordinator` needs no flavour-specific handling — it
 * simply never sees a launched prompt and leaves the milestone pending, which
 * is the documented contract of [ReviewPrompter.requestReview].
 */
class NoopReviewPrompter(
    private val logger: AppLogger,
) : ReviewPrompter {

    override suspend fun requestReview(): Boolean {
        logger.d(TAG, "FOSS build: no in-app review flow available")
        return false
    }

    private companion object {
        const val TAG = "NoopReviewPrompter"
    }
}

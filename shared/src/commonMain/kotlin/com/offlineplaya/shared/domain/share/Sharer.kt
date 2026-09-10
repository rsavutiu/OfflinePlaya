package com.offlineplaya.shared.domain.share

/**
 * Platform seam for handing text to the OS "share" sheet (Android's
 * `ACTION_SEND` chooser). Lives in `domain` so the share-nudge logic in
 * [com.offlineplaya.shared.presentation.review.ShareNudgeCoordinator] stays
 * platform-agnostic; future iOS/Desktop targets bind their own impl.
 *
 * Mirrors the [com.offlineplaya.shared.domain.review.ReviewPrompter] idiom.
 */
interface Sharer {
    /**
     * Open the system share chooser for [text]. Returns `true` only if the
     * chooser was actually launched (a resumed Activity / valid context was
     * available); `false` when it couldn't run. Whether the user completes the
     * share is not observable — do not treat `true` as "the user shared".
     */
    fun share(text: String): Boolean
}

package com.offlineplaya.shared.domain.review

/**
 * Platform seam for asking the app store to surface its native "rate this app"
 * flow (Google Play's in-app review on Android). Lives in `domain` so the
 * milestone logic in [com.offlineplaya.shared.presentation.review.ReviewPromptCoordinator]
 * stays platform-agnostic; future iOS/Desktop targets bind their own impl.
 *
 * The platform store deliberately tells us nothing about whether the user
 * actually rated — only whether the flow was *launched* — so the only honest
 * signal we can act on is [requestReview]'s boolean.
 */
interface ReviewPrompter {
    /**
     * Try to show the native in-app review dialog. Returns `true` only if the
     * flow was actually launched (an Activity was available and the store
     * accepted the request); `false` when it couldn't run (no resumed
     * Activity, sideloaded build, store error). Callers must NOT treat a
     * `true` result as "the user reviewed" — that is unknowable by design.
     */
    suspend fun requestReview(): Boolean
}

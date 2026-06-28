package com.offlineplaya.shared.domain.model

/**
 * Persisted bookkeeping for the in-app review nudges. Drives
 * [com.offlineplaya.shared.presentation.review.ReviewPromptCoordinator].
 *
 * @param playsCounted total tracks played since install (incremented in the
 *   background, even when no Activity is around to show a prompt).
 * @param highestMilestoneFired the largest play-count milestone (10/100/1000)
 *   for which a prompt has actually been launched. Monotonic — a milestone is
 *   only recorded once the store accepts the flow, so a prompt that couldn't
 *   show (app backgrounded at the time) is retried at the next foreground.
 * @param lastPromptAtMillis epoch millis of the last launched prompt; gates the
 *   post-resync nudge behind a cooldown so it can't fire on every app launch.
 */
data class ReviewPromptState(
    val playsCounted: Int = 0,
    val highestMilestoneFired: Int = 0,
    val lastPromptAtMillis: Long = 0L,
) {
    companion object {
        val Default = ReviewPromptState()
    }
}

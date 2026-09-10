package com.offlineplaya.shared.domain.model

/**
 * Persisted bookkeeping for BOTH nudge systems:
 *  - the native Google Play review flow (milestone / post-resync), driven by
 *    [com.offlineplaya.shared.presentation.review.ReviewPromptCoordinator], and
 *  - the custom "share the app" dialog, driven by
 *    [com.offlineplaya.shared.presentation.review.ShareNudgeCoordinator].
 *
 * The two are independent (different triggers and copy) but share one
 * [lastPromptAtMillis] clock so they can be deconflicted — we never bug the
 * user with both in quick succession.
 *
 * @param playsCounted total tracks played since install (incremented in the
 *   background, even when no Activity is around to show a prompt). Also the
 *   "has actually listened to something" gate for the share dialog.
 * @param highestMilestoneFired the largest play-count milestone (10/100/1000)
 *   for which the native review flow has actually launched. Monotonic — a
 *   milestone is only recorded once the store accepts the flow, so a prompt
 *   that couldn't show (app backgrounded) is retried at the next foreground.
 * @param lastPromptAtMillis epoch millis of the last time ANY nudge was shown
 *   (native review OR share dialog). Gates the native post-resync nudge behind
 *   a cooldown and lets the share dialog stay clear of a just-shown review.
 * @param firstLaunchAtMillis epoch millis first observed by the share
 *   coordinator; the "used the app for a week" clock starts here. `0L` means
 *   "not seeded yet" — seeded lazily on first read so existing users start the
 *   week countdown at the update, not retroactively.
 * @param shareNudgeLastShownAtMillis epoch millis the share dialog was last
 *   shown; a dismissed dialog re-appears one cooldown later. `0L` = never shown.
 * @param shareNudgeActioned the user tapped Share at least once. The share
 *   dialog then never shows again. NOTE: this is the *tap*, not proof they
 *   completed a share — that signal isn't observable, same as ratings.
 */
data class ReviewPromptState(
    val playsCounted: Int = 0,
    val highestMilestoneFired: Int = 0,
    val lastPromptAtMillis: Long = 0L,
    val firstLaunchAtMillis: Long = 0L,
    val shareNudgeLastShownAtMillis: Long = 0L,
    val shareNudgeActioned: Boolean = false,
) {
    companion object {
        val Default = ReviewPromptState()
    }
}

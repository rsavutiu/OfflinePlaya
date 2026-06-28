package com.offlineplaya.shared.presentation.review

import com.offlineplaya.shared.domain.repository.SettingsRepository
import com.offlineplaya.shared.domain.review.ReviewPrompter
import com.offlineplaya.shared.util.AppLogger
import com.offlineplaya.shared.util.currentTimeMillis

/**
 * Decides when to surface the native in-app review flow ([ReviewPrompter]).
 *
 * Two triggers, deliberately split between "counting" and "prompting" because
 * for a music player the milestone'th track almost always plays while the app
 * is backgrounded — and the store can only show its dialog from a resumed
 * Activity:
 *
 *  - [onTrackPlayed] runs in the background and *only increments the counter*.
 *  - [maybePromptForReview] runs at a foreground checkpoint (the Activity's
 *    ON_RESUME) and decides whether to actually launch the flow.
 *
 * Milestones [10, 100, 1000] each fire once, escalating. A milestone is marked
 * consumed *only when the flow actually launches*, so a milestone reached while
 * backgrounded is retried at the next foreground rather than silently burned.
 *
 * Important limitation surfaced to the caller: the Play API never reports
 * whether the user actually rated, so these are three escalating *nudges*, not
 * conditional "ask again only if they didn't confirm" retries — that signal
 * does not exist.
 *
 * The post-resync nudge ("after a resync, why not") is evaluated at the same
 * foreground checkpoint (resync also fires on resume). It only nudges users who
 * have already crossed the first milestone, and never more than once per
 * [resyncCooldownMillis], so it can't fire on every app launch or on a brand
 * new user who just added their first folder.
 */
class ReviewPromptCoordinator(
    private val settings: SettingsRepository,
    private val prompter: ReviewPrompter,
    private val logger: AppLogger,
    private val now: () -> Long = ::currentTimeMillis,
    private val milestones: List<Int> = DEFAULT_MILESTONES,
    private val resyncCooldownMillis: Long = DEFAULT_RESYNC_COOLDOWN_MILLIS,
) {
    /** Background-safe: bump the lifetime play counter, nothing else. */
    suspend fun onTrackPlayed() {
        try {
            val state = settings.getReviewPromptState()
            settings.setReviewPromptState(state.copy(playsCounted = state.playsCounted + 1))
        } catch (t: Throwable) {
            // Best-effort bookkeeping — never let it disrupt playback/history.
            logger.w(TAG, "Failed to count play for review prompt: ${t.message}")
        }
    }

    /**
     * Foreground checkpoint. Call when an Activity is resumed. Launches the
     * review flow if a play-count milestone is due, otherwise nudges an
     * established user once per cooldown. No-op when nothing is due or the
     * store declines to show the dialog.
     */
    suspend fun maybePromptForReview() {
        try {
            val state = settings.getReviewPromptState()
            val dueMilestone = milestones.firstOrNull {
                it > state.highestMilestoneFired && state.playsCounted >= it
            }
            when {
                dueMilestone != null -> {
                    if (prompter.requestReview()) {
                        settings.setReviewPromptState(
                            state.copy(
                                highestMilestoneFired = dueMilestone,
                                lastPromptAtMillis = now(),
                            ),
                        )
                    }
                }
                // Post-resync nudge: established users only, cooldown-gated.
                state.highestMilestoneFired > 0 &&
                    now() - state.lastPromptAtMillis >= resyncCooldownMillis -> {
                    if (prompter.requestReview()) {
                        settings.setReviewPromptState(state.copy(lastPromptAtMillis = now()))
                    }
                }
            }
        } catch (t: Throwable) {
            logger.w(TAG, "Review prompt checkpoint failed: ${t.message}")
        }
    }

    companion object {
        const val TAG = "ReviewPromptCoordinator"

        /** Escalating play-count milestones: prompt at 10, then 100, then 1000. */
        val DEFAULT_MILESTONES = listOf(10, 100, 1000)

        /** 30 days between post-resync nudges. */
        const val DEFAULT_RESYNC_COOLDOWN_MILLIS = 30L * 24 * 60 * 60 * 1000
    }
}

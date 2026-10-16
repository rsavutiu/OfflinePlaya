package com.offlineplaya.shared.presentation.review

import com.offlineplaya.shared.domain.repository.SettingsRepository
import com.offlineplaya.shared.domain.stream.PlayToPcController
import com.offlineplaya.shared.util.AppLogger
import com.offlineplaya.shared.util.currentTimeMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Surfaces the "did you know you can stream to your PC?" feature-discovery
 * dialog — Stream-to-PC otherwise hides in Settings, and a feature nobody finds
 * might as well not exist. Sibling to [ShareNudgeCoordinator] and
 * [ReviewPromptCoordinator]: same persisted-state bookkeeping and the same
 * shared [com.offlineplaya.shared.domain.model.ReviewPromptState.lastPromptAtMillis]
 * deconfliction clock, so the three nudges never stack.
 *
 * **When it shows** (all must hold):
 *  - the user has played at least [minPlays] tracks (they're actually using the
 *    app for music — "after playing music", as asked);
 *  - they haven't already engaged with Play-to-PC ([markActioned] — opened the
 *    screen, connected, or tapped through the dialog), and aren't streaming
 *    right now ([PlayToPcController.active]);
 *  - it's been shown fewer than [maxShows] times (so a user who'll never use it
 *    isn't nagged forever);
 *  - it's been at least [reshowCooldownMillis] (a month) since it last showed;
 *  - it's been at least [crossNudgeGapMillis] since ANY nudge fired.
 *
 * The host observes [visible], renders the dialog, and calls [markActioned] /
 * [onDismiss]. [maybePrompt] is the foreground checkpoint (Activity ON_RESUME),
 * called AFTER the review and share coordinators so at most one nudge fires per
 * resume.
 */
class PlayToPcNudgeCoordinator(
    private val settings: SettingsRepository,
    private val playToPc: PlayToPcController,
    private val logger: AppLogger,
    private val now: () -> Long = ::currentTimeMillis,
    private val minPlays: Int = DEFAULT_MIN_PLAYS,
    private val maxShows: Int = DEFAULT_MAX_SHOWS,
    private val reshowCooldownMillis: Long = DEFAULT_RESHOW_COOLDOWN_MILLIS,
    private val crossNudgeGapMillis: Long = DEFAULT_CROSS_NUDGE_GAP_MILLIS,
) {
    private val _visible = MutableStateFlow(false)

    /** Whether the discovery dialog should currently be on screen. */
    val visible: StateFlow<Boolean> = _visible.asStateFlow()

    /** Foreground checkpoint. Returns `true` if it put the dialog on screen. */
    suspend fun maybePrompt(): Boolean {
        if (_visible.value) return true
        // Already using the feature — never nudge about something they're doing.
        if (playToPc.active.value) return false
        return try {
            val state = settings.getReviewPromptState()
            val nowMs = now()

            val reshowReady = state.playToPcNudgeLastShownAtMillis == 0L ||
                nowMs - state.playToPcNudgeLastShownAtMillis >= reshowCooldownMillis
            // Deconflict only against a nudge that actually fired; on a fresh
            // install (clock == 0) there's nothing to stay clear of.
            val deconflictReady = state.lastPromptAtMillis == 0L ||
                nowMs - state.lastPromptAtMillis >= crossNudgeGapMillis
            val due = !state.playToPcNudgeActioned &&
                state.playToPcNudgeShowCount < maxShows &&
                state.playsCounted >= minPlays &&
                reshowReady &&
                deconflictReady

            if (due) {
                // Record the show up front: the re-show timer and the shared
                // deconfliction clock both start now; the show count caps it.
                settings.setReviewPromptState(
                    state.copy(
                        playToPcNudgeLastShownAtMillis = nowMs,
                        playToPcNudgeShowCount = state.playToPcNudgeShowCount + 1,
                        lastPromptAtMillis = nowMs,
                    ),
                )
                _visible.value = true
            }
            due
        } catch (t: Throwable) {
            logger.w(TAG, "Play-to-PC nudge checkpoint failed: ${t.message}")
            false
        }
    }

    /**
     * The user engaged with Play-to-PC — opened the screen, connected, or tapped
     * through the dialog. Hide it and never show it again. Safe to call often
     * (e.g. every time the Play-to-PC screen opens).
     */
    suspend fun markActioned() {
        _visible.value = false
        try {
            val state = settings.getReviewPromptState()
            if (!state.playToPcNudgeActioned) {
                settings.setReviewPromptState(state.copy(playToPcNudgeActioned = true))
            }
        } catch (t: Throwable) {
            logger.w(TAG, "Failed to persist Play-to-PC nudge action: ${t.message}")
        }
    }

    /** User dismissed. Timestamps were set at show time → it returns in a month (up to the cap). */
    fun onDismiss() {
        _visible.value = false
    }

    companion object {
        const val TAG = "PlayToPcNudgeCoordinator"

        /** Played a couple of tracks — "after playing music" — before we ask. */
        const val DEFAULT_MIN_PLAYS = 2

        /** Never nag more than this many times total if they don't engage. */
        const val DEFAULT_MAX_SHOWS = 2

        /** A dismissed dialog re-appears one month later (until the cap). */
        const val DEFAULT_RESHOW_COOLDOWN_MILLIS = 30L * 24 * 60 * 60 * 1000

        /** Minimum gap between ANY two nudges (review / share / this). */
        const val DEFAULT_CROSS_NUDGE_GAP_MILLIS = 7L * 24 * 60 * 60 * 1000

        /**
         * Where "Get the PC app" sends the user: the latest GitHub release of the
         * desktop receiver. Public, no login, permanent — unlike CI artifacts.
         * Created by the `desktop-release.yml` workflow on a `desktop-v*` tag.
         */
        const val GITHUB_RELEASES_URL = "https://github.com/rsavutiu/OfflinePlaya/releases/latest"
    }
}

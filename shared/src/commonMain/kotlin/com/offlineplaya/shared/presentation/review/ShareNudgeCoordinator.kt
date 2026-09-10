package com.offlineplaya.shared.presentation.review

import com.offlineplaya.shared.domain.repository.SettingsRepository
import com.offlineplaya.shared.domain.share.Sharer
import com.offlineplaya.shared.util.AppLogger
import com.offlineplaya.shared.util.currentTimeMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Decides when to surface the custom "help us spread the word" dialog — the
 * app is free, has no marketing, and only reaches new people if users share
 * it. Distinct from (and independent of) the native Google Play review flow in
 * [ReviewPromptCoordinator]: different trigger, our own copy, a real Share
 * action rather than a rating sheet.
 *
 * **When it shows** (all must hold):
 *  - the user has used the app for at least [minAgeMillis] (a week), measured
 *    from the first time this coordinator observed them ([firstLaunch] seeded
 *    lazily so existing users get a fresh week, not a retroactive one);
 *  - they've actually played music ([ReviewPromptState.playsCounted] > 0);
 *  - they haven't already tapped Share ([ReviewPromptState.shareNudgeActioned]);
 *  - it's been at least [reshowCooldownMillis] (a month) since it last showed —
 *    a dismissed dialog re-appears one month later, on its own;
 *  - it's been at least [crossNudgeGapMillis] since ANY nudge (this or the
 *    native review) — the deconfliction that stops the two stacking.
 *
 * The visual dialog is driven off [visible]; the host renders it and calls
 * [onShareClicked] / [onDismiss]. [maybePromptForShare] is the foreground
 * checkpoint (Activity ON_RESUME), called AFTER the native review coordinator
 * so at most one nudge is decided per resume.
 */
class ShareNudgeCoordinator(
    private val settings: SettingsRepository,
    private val sharer: Sharer,
    private val logger: AppLogger,
    private val now: () -> Long = ::currentTimeMillis,
    private val minAgeMillis: Long = DEFAULT_MIN_AGE_MILLIS,
    private val reshowCooldownMillis: Long = DEFAULT_RESHOW_COOLDOWN_MILLIS,
    private val crossNudgeGapMillis: Long = DEFAULT_CROSS_NUDGE_GAP_MILLIS,
) {
    private val _visible = MutableStateFlow(false)

    /** Whether the share dialog should currently be on screen. */
    val visible: StateFlow<Boolean> = _visible.asStateFlow()

    /**
     * Foreground checkpoint. Seeds the first-launch clock on first ever call,
     * then shows the dialog if everything above holds. Returns `true` if it
     * put the dialog on screen (so the host can know a nudge fired).
     */
    suspend fun maybePromptForShare(): Boolean {
        if (_visible.value) return true
        return try {
            var state = settings.getReviewPromptState()
            val nowMs = now()

            // Lazy first-launch seed: existing users (upgrading into this
            // feature) start their week here, not from epoch 0 — otherwise the
            // "week old" gate would be instantly true for everyone.
            if (state.firstLaunchAtMillis == 0L) {
                state = state.copy(firstLaunchAtMillis = nowMs)
                settings.setReviewPromptState(state)
            }

            // A never-shown dialog (lastShown == 0) is due on the cooldown
            // check; only a *previous* show starts the month-long re-show timer.
            val reshowReady = state.shareNudgeLastShownAtMillis == 0L ||
                nowMs - state.shareNudgeLastShownAtMillis >= reshowCooldownMillis
            val due = !state.shareNudgeActioned &&
                state.playsCounted > 0 &&
                nowMs - state.firstLaunchAtMillis >= minAgeMillis &&
                reshowReady &&
                nowMs - state.lastPromptAtMillis >= crossNudgeGapMillis

            if (due) {
                // Record the show up front: the re-show timer starts now (a
                // dismiss just lets it lapse), and lastPromptAt is the shared
                // deconfliction clock the native review coordinator also reads.
                settings.setReviewPromptState(
                    state.copy(
                        shareNudgeLastShownAtMillis = nowMs,
                        lastPromptAtMillis = nowMs,
                    ),
                )
                _visible.value = true
            }
            due
        } catch (t: Throwable) {
            logger.w(TAG, "Share nudge checkpoint failed: ${t.message}")
            false
        }
    }

    /**
     * User tapped Share. Launch the chooser and mark the nudge actioned so it
     * never shows again (the tap is all we can observe — see
     * [ReviewPromptState.shareNudgeActioned]).
     */
    suspend fun onShareClicked(shareText: String) {
        _visible.value = false
        sharer.share(shareText)
        try {
            val state = settings.getReviewPromptState()
            settings.setReviewPromptState(state.copy(shareNudgeActioned = true))
        } catch (t: Throwable) {
            logger.w(TAG, "Failed to persist share action: ${t.message}")
        }
    }

    /** User dismissed. Timestamps were set at show time → it returns in a month. */
    fun onDismiss() {
        _visible.value = false
    }

    companion object {
        const val TAG = "ShareNudgeCoordinator"

        /** "Used the app for a week" before the first share ask. */
        const val DEFAULT_MIN_AGE_MILLIS = 7L * 24 * 60 * 60 * 1000

        /** A dismissed share dialog re-appears one month later. */
        const val DEFAULT_RESHOW_COOLDOWN_MILLIS = 30L * 24 * 60 * 60 * 1000

        /**
         * Minimum gap between ANY two nudges (share or native review). Keeps
         * the share dialog from landing right after a review sheet even when
         * both happen to come due — the two stay monthly but never stack.
         */
        const val DEFAULT_CROSS_NUDGE_GAP_MILLIS = 7L * 24 * 60 * 60 * 1000

        /** Play Store listing for the release build (not the `.debug` suffix). */
        const val PLAY_STORE_URL =
            "https://play.google.com/store/apps/details?id=com.offlineplaya.android"
    }
}

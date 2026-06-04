package com.offlineplaya.shared.domain.model

/**
 * User-customizable playback-engine preferences. Persisted by
 * [com.offlineplaya.shared.domain.repository.SettingsRepository] and observed by
 * the Android crossfade controller (`CrossfadeController`).
 *
 * @property crossfadeEnabled when `true`, consecutive tracks are crossfaded:
 *   the outgoing track's tail keeps playing on a secondary engine while the
 *   incoming track fades up on the main player, so both are briefly audible at
 *   once. When `false` the app falls back to ExoPlayer's native **gapless**
 *   playback (back-to-back with no silence) — there is no separate "gapless"
 *   toggle because gapless is always-on in ExoPlayer and is exactly what you
 *   get with crossfade off.
 * @property crossfadeDurationSeconds length of the overlap, in seconds. Only
 *   meaningful when [crossfadeEnabled]. Clamped to
 *   [MIN_DURATION_SECONDS]..[MAX_DURATION_SECONDS].
 */
data class PlaybackPreferences(
    val crossfadeEnabled: Boolean,
    val crossfadeDurationSeconds: Int,
) {
    /** Overlap length in milliseconds, or `0` when crossfade is disabled. */
    val crossfadeDurationMs: Long
        get() = if (crossfadeEnabled) crossfadeDurationSeconds * 1000L else 0L

    companion object {
        const val MIN_DURATION_SECONDS = 1
        const val MAX_DURATION_SECONDS = 12

        /**
         * Crossfade on by default at 6s — a tasteful middle of the 1–12s range
         * that smooths shuffle/playlist transitions without swallowing whole
         * intros. Album purists can drop it to native gapless with one toggle.
         */
        val Default = PlaybackPreferences(
            crossfadeEnabled = true,
            crossfadeDurationSeconds = 6,
        )
    }
}

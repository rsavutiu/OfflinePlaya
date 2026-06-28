package com.offlineplaya.shared.presentation.history

import com.offlineplaya.shared.domain.player.MusicPlayer
import com.offlineplaya.shared.domain.repository.PlayHistoryRepository
import com.offlineplaya.shared.presentation.review.ReviewPromptCoordinator
import com.offlineplaya.shared.util.AppLogger
import com.offlineplaya.shared.util.currentTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Watches the player and appends a [PlayHistoryRepository] row every time a
 * track becomes the playing item — explicit taps and queue auto-advance both
 * count, which is why this observes [MusicPlayer.playbackState] rather than
 * hooking the UI's play action.
 *
 * Consecutive emissions for the same track (position ticks, pause/resume)
 * record nothing; only a *change* of playing track does. Replaying the same
 * track back-to-back via repeat-one therefore counts once — acceptable for
 * a "what do I actually listen to" signal.
 */
class PlayHistoryRecorder(
    private val musicPlayer: MusicPlayer,
    private val playHistory: PlayHistoryRepository,
    private val scope: CoroutineScope,
    private val logger: AppLogger,
    // Optional so the test/E2E fixtures that construct the recorder directly
    // don't need to supply it; production wires it via Koin.
    private val reviewPromptCoordinator: ReviewPromptCoordinator? = null,
) {
    private var job: Job? = null

    fun start() {
        if (job != null) return
        job = scope.launch {
            var lastRecordedTrackId: Long? = null
            musicPlayer.playbackState.collect { state ->
                val trackId = state.currentTrack?.id
                if (trackId != null && state.isPlaying && trackId != lastRecordedTrackId) {
                    lastRecordedTrackId = trackId
                    try {
                        playHistory.recordPlay(trackId, currentTimeMillis())
                    } catch (t: Throwable) {
                        // History is best-effort — never let a DB hiccup
                        // bubble into the player pipeline.
                        logger.w(TAG, "Failed to record play of $trackId: ${t.message}")
                    }
                    // Counts toward the in-app review nudge milestones. The
                    // coordinator only *counts* here; the prompt itself fires at
                    // a foreground checkpoint (ON_RESUME) where an Activity exists.
                    reviewPromptCoordinator?.onTrackPlayed()
                }
            }
        }
    }

    private companion object {
        const val TAG = "PlayHistoryRecorder"
    }
}

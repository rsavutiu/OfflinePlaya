package com.offlineplaya.shared.domain.usecase

import com.offlineplaya.shared.domain.model.RepeatMode
import com.offlineplaya.shared.domain.player.MusicPlayer
import com.offlineplaya.shared.domain.repository.QueueRepository
import com.offlineplaya.shared.util.AppLogger

/**
 * Cold-start counterpart of `QueueStateRecorder`: loads the persisted queue
 * and hands it to the player **paused** at the saved index/position, so the
 * mini player reappears with last session's queue without making a sound.
 *
 * Skipped when anything is already queued (e.g. the system's playback
 * resumption beat us to it); [MusicPlayer.restoreQueue] guards the same
 * race internally for taps that land mid-restore.
 */
class RestorePersistedQueueUseCase(
    private val queue: QueueRepository,
    private val player: MusicPlayer,
    private val logger: AppLogger,
) {
    suspend operator fun invoke() {
        try {
            if (player.playbackState.value.queue.isNotEmpty()) return
            val tracks = queue.loadQueue()
            if (tracks.isEmpty()) return
            val snapshot = queue.loadPlaybackSnapshot()
            player.setShuffleEnabled(snapshot?.shuffleEnabled ?: false)
            player.setRepeatMode(snapshot?.repeatMode ?: RepeatMode.OFF)
            player.restoreQueue(
                tracks = tracks,
                startIndex = (snapshot?.queueIndex ?: 0).coerceIn(0, tracks.size - 1),
                positionMs = snapshot?.positionMs ?: 0L,
            )
            logger.i(TAG, "Restored queue of ${tracks.size} at index ${snapshot?.queueIndex}")
        } catch (t: Throwable) {
            // Restore is a convenience — never let it disturb startup.
            logger.w(TAG, "Queue restore failed: ${t.message}")
        }
    }

    private companion object {
        const val TAG = "RestorePersistedQueue"
    }
}

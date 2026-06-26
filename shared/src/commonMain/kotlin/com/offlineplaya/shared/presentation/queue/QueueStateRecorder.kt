package com.offlineplaya.shared.presentation.queue

import com.offlineplaya.shared.domain.model.PlaybackSnapshot
import com.offlineplaya.shared.domain.player.MusicPlayer
import com.offlineplaya.shared.domain.repository.QueueRepository
import com.offlineplaya.shared.util.AppLogger
import com.offlineplaya.shared.util.currentTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Watches the player and persists the queue + transport position so the next
 * process can pick up where this one stopped (see `RestorePersistedQueueUseCase`
 * and the session's playback-resumption callback).
 *
 * Write policy — the queue itself is saved only when its track ids actually
 * change; the cheap transport snapshot is saved immediately on the
 * "moments that matter" (track change, pause, mode change) and otherwise at
 * most every [SNAPSHOT_INTERVAL_MS] while playing, so position survives a
 * process kill to within a few seconds without hammering the DB on every
 * 500 ms position tick.
 *
 * The first emission a fresh process sees is [com.offlineplaya.shared.domain.model.PlaybackState.Empty];
 * persisting that would wipe the saved queue before the restore path ever
 * reads it, so an empty queue is only persisted after a non-empty one was
 * seen this session (i.e. the user really cleared it).
 */
class QueueStateRecorder(
    private val musicPlayer: MusicPlayer,
    private val queue: QueueRepository,
    private val scope: CoroutineScope,
    private val logger: AppLogger,
    private val now: () -> Long = { currentTimeMillis() },
) {
    private var job: Job? = null

    fun start() {
        if (job != null) return
        job = scope.launch {
            var lastIds: List<Long>? = null
            var lastSnapshot: PlaybackSnapshot? = null
            var lastSnapshotAt = 0L
            var wasPlaying = false

            musicPlayer.playbackState.collect { state ->
                val ids = state.queue.map { it.id }

                var queueChanged = false
                if (ids != lastIds) {
                    val startupEmpty = ids.isEmpty() && lastIds == null
                    lastIds = ids
                    if (!startupEmpty) {
                        queueChanged = true
                        runSaving("queue") { queue.replaceAll(ids) }
                    }
                }

                if (ids.isEmpty()) {
                    wasPlaying = false
                    return@collect
                }

                val snapshot = PlaybackSnapshot(
                    queueIndex = state.queueIndex,
                    positionMs = state.positionMs,
                    shuffleEnabled = state.shuffleEnabled,
                    repeatMode = state.repeatMode,
                )
                val pauseEdge = wasPlaying && !state.isPlaying
                val structuralChange = lastSnapshot?.let {
                    it.queueIndex != snapshot.queueIndex ||
                        it.shuffleEnabled != snapshot.shuffleEnabled ||
                        it.repeatMode != snapshot.repeatMode
                } ?: true
                val periodic = state.isPlaying && now() - lastSnapshotAt >= SNAPSHOT_INTERVAL_MS

                // queueChanged is in the immediate set so a queue swap can't
                // leave the previous queue's position pointing into the new one.
                if (queueChanged || pauseEdge || structuralChange || periodic) {
                    lastSnapshot = snapshot
                    lastSnapshotAt = now()
                    runSaving("snapshot") { queue.savePlaybackSnapshot(snapshot) }
                }
                wasPlaying = state.isPlaying
            }
        }
    }

    /** Persistence is best-effort — a DB hiccup must never reach the player. */
    private suspend fun runSaving(what: String, save: suspend () -> Unit) {
        try {
            save()
        } catch (t: Throwable) {
            logger.w(TAG, "Failed to persist $what: ${t.message}")
        }
    }

    private companion object {
        const val TAG = "QueueStateRecorder"
        const val SNAPSHOT_INTERVAL_MS = 5_000L
    }
}

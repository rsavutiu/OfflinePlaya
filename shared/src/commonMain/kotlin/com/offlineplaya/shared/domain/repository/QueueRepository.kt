package com.offlineplaya.shared.domain.repository

import com.offlineplaya.shared.domain.model.PlaybackSnapshot
import com.offlineplaya.shared.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface QueueRepository {
    fun observeQueue(): Flow<List<Track>>
    suspend fun count(): Long
    suspend fun enqueue(trackId: Long, source: String = "queue")
    suspend fun clear()

    /** Atomically replace the persisted queue with [trackIds] in play order. */
    suspend fun replaceAll(trackIds: List<Long>)

    /**
     * One-shot read of the persisted queue in play order. Tracks that have
     * since left the library are silently dropped (the row joins Track), so
     * the result can be shorter than what was saved.
     */
    suspend fun loadQueue(): List<Track>

    /** Persist the transport state that goes with the saved queue. */
    suspend fun savePlaybackSnapshot(snapshot: PlaybackSnapshot)

    /** The last saved transport state, or `null` if none was ever saved. */
    suspend fun loadPlaybackSnapshot(): PlaybackSnapshot?
}

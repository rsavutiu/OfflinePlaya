package com.offlineplaya.shared.domain.repository

import com.offlineplaya.shared.domain.model.Track
import kotlinx.coroutines.flow.Flow

/**
 * Listening history: one event per track play. Backs the play counter and
 * the history-derived smart playlists. Timestamps are epoch millis.
 */
interface PlayHistoryRepository {
    suspend fun recordPlay(trackId: Long, playedAtMs: Long)
    suspend fun countForTrack(trackId: Long): Long

    fun observeMostPlayed(limit: Int): Flow<List<Track>>
    fun observeRecentlyPlayed(limit: Int): Flow<List<Track>>
    fun observeNeverPlayed(limit: Int): Flow<List<Track>>

    /**
     * Tracks played at least [minPlays] times overall but not once since
     * [cutoffMs] — old favorites that fell out of rotation.
     */
    fun observeForgottenFavorites(minPlays: Int, cutoffMs: Long, limit: Int): Flow<List<Track>>
}

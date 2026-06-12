package com.offlineplaya.shared.domain.repository

import com.offlineplaya.shared.domain.model.ListeningStats
import com.offlineplaya.shared.domain.model.PlayedTrack
import com.offlineplaya.shared.domain.model.TopAlbumStat
import com.offlineplaya.shared.domain.model.TopArtistStat
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

    /**
     * Most-played tracks with play counts. [artistId] scopes to one artist
     * (`null` = whole library); [sinceMs] keeps plays at/after that time
     * (`0` = all time).
     */
    fun observeTopPlayed(artistId: Long?, sinceMs: Long, limit: Int): Flow<List<PlayedTrack>>

    /** Headline totals (plays / distinct tracks / listening time) since [sinceMs]. */
    fun observeStats(sinceMs: Long): Flow<ListeningStats>

    fun observeTopArtists(sinceMs: Long, limit: Int): Flow<List<TopArtistStat>>
    fun observeTopAlbums(sinceMs: Long, limit: Int): Flow<List<TopAlbumStat>>
}

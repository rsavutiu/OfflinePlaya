package com.offlineplaya.shared.presentation.history

import com.offlineplaya.shared.domain.model.ListeningStats
import com.offlineplaya.shared.domain.model.PlayedTrack
import com.offlineplaya.shared.domain.model.SmartPlaylistKind
import com.offlineplaya.shared.domain.model.StatsPeriod
import com.offlineplaya.shared.domain.model.TopAlbumStat
import com.offlineplaya.shared.domain.model.TopArtistStat
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.repository.PlayHistoryRepository
import com.offlineplaya.shared.domain.repository.TrackRepository
import com.offlineplaya.shared.util.currentTimeMillis
import kotlinx.coroutines.flow.Flow

/**
 * The history-derived read models: resolves each [SmartPlaylistKind] to its
 * live track list, and serves the artist top-tracks + listening-stats
 * aggregates. Flows are cold — pages collect only while visible, so the
 * underlying GROUP-BY queries don't run in the background.
 */
class SmartPlaylistsStateHolder(
    private val playHistory: PlayHistoryRepository,
    private val tracks: TrackRepository,
) {
    fun observe(kind: SmartPlaylistKind): Flow<List<Track>> = when (kind) {
        SmartPlaylistKind.RECENTLY_PLAYED -> playHistory.observeRecentlyPlayed(LIMIT)
        SmartPlaylistKind.MOST_PLAYED -> playHistory.observeMostPlayed(LIMIT)
        SmartPlaylistKind.RECENTLY_ADDED -> tracks.observeRecentlyAdded(LIMIT)
        SmartPlaylistKind.FORGOTTEN_FAVORITES -> playHistory.observeForgottenFavorites(
            minPlays = FORGOTTEN_MIN_PLAYS,
            cutoffMs = currentTimeMillis() - FORGOTTEN_DAYS * DAY_MS,
            limit = LIMIT,
        )
        SmartPlaylistKind.NEVER_PLAYED -> playHistory.observeNeverPlayed(LIMIT)
    }

    /** The artist-detail "Top tracks" section: all-time, ranked by plays. */
    fun topTracksForArtist(artistId: Long): Flow<List<PlayedTrack>> =
        playHistory.observeTopPlayed(artistId = artistId, sinceMs = 0L, limit = TOP_TRACKS_LIMIT)

    // --- listening-stats page ---

    fun stats(period: StatsPeriod): Flow<ListeningStats> =
        playHistory.observeStats(period.sinceMs())

    fun topArtists(period: StatsPeriod): Flow<List<TopArtistStat>> =
        playHistory.observeTopArtists(period.sinceMs(), STATS_LIMIT)

    fun topAlbums(period: StatsPeriod): Flow<List<TopAlbumStat>> =
        playHistory.observeTopAlbums(period.sinceMs(), STATS_LIMIT)

    fun topTracks(period: StatsPeriod): Flow<List<PlayedTrack>> =
        playHistory.observeTopPlayed(artistId = null, sinceMs = period.sinceMs(), limit = STATS_LIMIT)

    private fun StatsPeriod.sinceMs(): Long = when (this) {
        StatsPeriod.WEEK -> currentTimeMillis() - 7L * DAY_MS
        StatsPeriod.MONTH -> currentTimeMillis() - 30L * DAY_MS
        StatsPeriod.ALL -> 0L
    }

    private companion object {
        const val LIMIT = 200
        const val TOP_TRACKS_LIMIT = 5
        const val STATS_LIMIT = 5
        /** "Favorite" = played at least this many times overall. */
        const val FORGOTTEN_MIN_PLAYS = 3
        /** "Forgotten" = not played within this window. */
        const val FORGOTTEN_DAYS = 30L
        const val DAY_MS = 24L * 60L * 60L * 1000L
    }
}

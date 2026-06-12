package com.offlineplaya.shared.presentation.history

import com.offlineplaya.shared.domain.model.PlayedTrack
import com.offlineplaya.shared.domain.model.SmartPlaylistKind
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.repository.PlayHistoryRepository
import com.offlineplaya.shared.domain.repository.TrackRepository
import kotlinx.coroutines.flow.Flow

/**
 * Resolves each [SmartPlaylistKind] to its live track list. Flows are cold —
 * the smart-playlist page collects only while visible, so the underlying
 * GROUP-BY queries don't run in the background.
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
            cutoffMs = System.currentTimeMillis() - FORGOTTEN_DAYS * DAY_MS,
            limit = LIMIT,
        )
        SmartPlaylistKind.NEVER_PLAYED -> playHistory.observeNeverPlayed(LIMIT)
    }

    /** The artist-detail "Top tracks" section: all-time, ranked by plays. */
    fun topTracksForArtist(artistId: Long): Flow<List<PlayedTrack>> =
        playHistory.observeTopPlayed(artistId = artistId, sinceMs = 0L, limit = TOP_TRACKS_LIMIT)

    private companion object {
        const val LIMIT = 200
        const val TOP_TRACKS_LIMIT = 5
        /** "Favorite" = played at least this many times overall. */
        const val FORGOTTEN_MIN_PLAYS = 3
        /** "Forgotten" = not played within this window. */
        const val FORGOTTEN_DAYS = 30L
        const val DAY_MS = 24L * 60L * 60L * 1000L
    }
}

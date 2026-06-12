package com.offlineplaya.shared.domain.model

/** A track paired with how many times it was played (in some period). */
data class PlayedTrack(
    val track: Track,
    val playCount: Long,
)

/** Headline listening totals for a period. */
data class ListeningStats(
    val plays: Long,
    val distinctTracks: Long,
    val listenedMs: Long,
) {
    companion object {
        val Empty = ListeningStats(plays = 0, distinctTracks = 0, listenedMs = 0)
    }
}

/** An artist ranked by play count (for the stats page). */
data class TopArtistStat(
    val artistId: Long,
    val name: String,
    val plays: Long,
)

/** An album ranked by play count (for the stats page). */
data class TopAlbumStat(
    val albumId: Long,
    val name: String,
    val artistName: String?,
    val plays: Long,
)

/** Time window the stats page aggregates over. */
enum class StatsPeriod {
    WEEK,
    MONTH,
    ALL,
}

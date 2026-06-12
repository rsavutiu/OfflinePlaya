package com.offlineplaya.shared.domain.model

/**
 * The user-editable tag fields for a single track, as entered in the tag
 * editor. Every field is nullable: `null` (or blank, for the text fields)
 * means "clear this tag." Technical fields (duration, bitrate, codec, …) are
 * intentionally absent — they're read from the stream, not user-editable.
 */
data class TrackTagEdits(
    val title: String?,
    val artist: String?,
    val albumArtist: String?,
    val album: String?,
    val genre: String?,
    val year: Int?,
    val trackNumber: Int?,
    val discNumber: Int?,
) {
    companion object {
        /** Pre-fill the editor from an existing track. */
        fun from(track: Track): TrackTagEdits = TrackTagEdits(
            title = track.title,
            artist = track.artistName,
            albumArtist = track.albumArtistName,
            album = track.albumName,
            genre = track.genre,
            year = track.year,
            trackNumber = track.trackNumber,
            discNumber = track.discNumber,
        )
    }
}

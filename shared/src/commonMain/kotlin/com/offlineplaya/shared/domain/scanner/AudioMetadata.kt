package com.offlineplaya.shared.domain.scanner

/**
 * Tag metadata for a single audio file, as read by [MetadataReader]. Every
 * field is nullable — readers must not invent values. Downstream code applies
 * fallbacks (e.g. file name when [title] is null).
 */
data class AudioMetadata(
    val title: String?,
    val artist: String?,
    val albumArtist: String?,
    val album: String?,
    val genre: String?,
    val year: Int?,
    val trackNumber: Int?,
    val discNumber: Int?,
    val durationMs: Long?,
    val bitrate: Int?,
    val sampleRate: Int?,
    val channels: Int?,
    val codec: String?,
) {
    companion object {
        val Empty = AudioMetadata(
            title = null, artist = null, albumArtist = null, album = null,
            genre = null, year = null, trackNumber = null, discNumber = null,
            durationMs = null, bitrate = null, sampleRate = null, channels = null,
            codec = null,
        )
    }
}

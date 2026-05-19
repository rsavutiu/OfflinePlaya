package com.offlineplaya.shared.data.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.offlineplaya.shared.domain.model.Track

/**
 * Bridges domain [Track] objects into Media3 [MediaItem]s. The Track's
 * `documentUri` is used as both the playable URI and the stable mediaId so
 * we can match notifications back to the source track.
 */
internal object TrackMediaItemMapper {

    fun toMediaItem(track: Track): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artistName)
            .setAlbumArtist(track.albumArtistName ?: track.artistName)
            .setAlbumTitle(track.albumName)
            .setGenre(track.genre)
            .setReleaseYear(track.year)
            .setTrackNumber(track.trackNumber)
            .setDiscNumber(track.discNumber)
            .setDurationMs(track.durationMs)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .build()

        return MediaItem.Builder()
            .setMediaId(track.documentUri)
            .setUri(Uri.parse(track.documentUri))
            .setMediaMetadata(metadata)
            .build()
    }

    /** Recover the track's stable mediaId (its document URI) from a Media3 item. */
    fun mediaIdOf(item: MediaItem?): String? = item?.mediaId?.takeIf { it.isNotBlank() }
}

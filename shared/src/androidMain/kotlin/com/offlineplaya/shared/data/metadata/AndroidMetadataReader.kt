package com.offlineplaya.shared.data.metadata

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.offlineplaya.shared.data.scanner.MetadataParsing
import com.offlineplaya.shared.domain.scanner.AudioMetadata
import com.offlineplaya.shared.domain.scanner.MetadataReader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [MetadataReader] backed by Android's [MediaMetadataRetriever]. Covers the
 * native Media3 format set (MP3, FLAC, OGG/Vorbis, AAC, OPUS, WAV, ALAC).
 *
 * Returns `null` on any failure — the use case will mark the track as errored.
 * A jaudiotagger fallback for APE / extended tags is deferred to a later
 * follow-up (see plan, Phase 2 risks).
 */
internal class AndroidMetadataReader(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MetadataReader {

    override suspend fun read(documentUri: String): AudioMetadata? = withContext(ioDispatcher) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(documentUri))
            extract(retriever)
        } catch (_: Throwable) {
            null
        } finally {
            // MediaMetadataRetriever only implements AutoCloseable on API 29+,
            // so we always call release() manually for minSdk 24 compatibility.
            try {
                retriever.release()
            } catch (_: Throwable) {
                // ignore — release errors don't change the read outcome
            }
        }
    }

    private fun extract(retriever: MediaMetadataRetriever): AudioMetadata {
        val mime = retriever.string(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
        return AudioMetadata(
            title = retriever.string(MediaMetadataRetriever.METADATA_KEY_TITLE),
            artist = retriever.string(MediaMetadataRetriever.METADATA_KEY_ARTIST),
            albumArtist = retriever.string(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST),
            album = retriever.string(MediaMetadataRetriever.METADATA_KEY_ALBUM),
            genre = retriever.string(MediaMetadataRetriever.METADATA_KEY_GENRE),
            year = MetadataParsing.parseYear(
                retriever.string(MediaMetadataRetriever.METADATA_KEY_YEAR)
                    ?: retriever.string(MediaMetadataRetriever.METADATA_KEY_DATE)
            ),
            trackNumber = MetadataParsing.parseTrackNumber(
                retriever.string(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
            ),
            discNumber = MetadataParsing.parseTrackNumber(
                retriever.string(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)
            ),
            durationMs = retriever.string(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull(),
            bitrate = retriever.string(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull(),
            sampleRate = null,  // not exposed by MediaMetadataRetriever
            channels = null,    // not exposed by MediaMetadataRetriever
            codec = codecFromMime(mime),
        )
    }

    private fun MediaMetadataRetriever.string(key: Int): String? =
        extractMetadata(key)?.takeIf { it.isNotBlank() }

    private fun codecFromMime(mime: String?): String? {
        if (mime.isNullOrBlank()) return null
        return mime.substringAfter("audio/", missingDelimiterValue = "")
            .takeIf { it.isNotBlank() }
    }
}

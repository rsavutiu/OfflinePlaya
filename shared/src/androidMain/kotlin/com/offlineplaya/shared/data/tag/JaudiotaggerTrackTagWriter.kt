package com.offlineplaya.shared.data.tag

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.offlineplaya.shared.data.util.resolveAudioExtension
import com.offlineplaya.shared.domain.model.TrackTagEdits
import com.offlineplaya.shared.domain.tag.TagWriteStats
import com.offlineplaya.shared.domain.tag.TrackTagWriter
import com.offlineplaya.shared.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.TagOptionSingleton
import java.io.File
import java.io.FileOutputStream

/**
 * [TrackTagWriter] backed by Jaudiotagger, using the same SAF temp-file dance
 * as [com.offlineplaya.shared.data.genre.JaudiotaggerGenreWriter]:
 *
 *  1. Copy the SAF document into a temp file in app cache.
 *  2. Load it, set/clear each edited field, commit.
 *  3. Stream the modified bytes back over the original SAF URI (`"w"` truncates).
 *
 * A blank/null edit clears that field; a non-blank value sets it.
 */
internal class JaudiotaggerTrackTagWriter(
    private val context: Context,
    private val logger: AppLogger,
) : TrackTagWriter {

    init {
        TagOptionSingleton.getInstance().isAndroid = true
    }

    override suspend fun write(
        documentUri: String,
        edits: TrackTagEdits,
    ): Result<TagWriteStats> = withContext(Dispatchers.IO) {
        runCatching {
            logger.i(TAG, "Writing tags to $documentUri")
            val uri = Uri.parse(documentUri)
            val resolver = context.contentResolver
            val extension = resolveAudioExtension(context, uri)
            val tempFile = File.createTempFile("tag-edit-", ".$extension", context.cacheDir)

            try {
                resolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                } ?: error("Could not open input stream for $uri")

                val audioFile = AudioFileIO.read(tempFile)
                val tag = audioFile.tagOrCreateAndSetDefault
                tag.apply(FieldKey.TITLE, edits.title)
                tag.apply(FieldKey.ARTIST, edits.artist)
                tag.apply(FieldKey.ALBUM_ARTIST, edits.albumArtist)
                tag.apply(FieldKey.ALBUM, edits.album)
                tag.apply(FieldKey.GENRE, edits.genre)
                tag.apply(FieldKey.YEAR, edits.year?.toString())
                tag.apply(FieldKey.TRACK, edits.trackNumber?.toString())
                tag.apply(FieldKey.DISC_NO, edits.discNumber?.toString())
                audioFile.commit()
                logger.d(TAG, "Committed tag edits to temp file")

                resolver.openFileDescriptor(uri, "w")?.use { pfd ->
                    FileOutputStream(pfd.fileDescriptor).use { output ->
                        tempFile.inputStream().use { input -> input.copyTo(output) }
                    }
                } ?: error("Could not open output FD for $uri")

                logger.i(TAG, "Successfully wrote tags back to $documentUri")
                queryStats(uri)
            } finally {
                if (tempFile.exists()) tempFile.delete()
            }
        }.onFailure { logger.e(TAG, "write($documentUri) failed", it) }
    }

    /**
     * Post-write (size, mtime) of the document, so the caller can refresh the
     * track's stored content fingerprint. SAF documents expose `last_modified`
     * in millis; MediaStore rows expose `date_modified` in seconds — probe the
     * document shape first and fall back. Both failing yields empty stats,
     * which leaves the stored fingerprint untouched.
     */
    private fun queryStats(uri: Uri): TagWriteStats {
        val resolver = context.contentResolver
        runCatching {
            resolver.query(
                uri,
                arrayOf(OpenableColumns.SIZE, DocumentsContract.Document.COLUMN_LAST_MODIFIED),
                null, null, null,
            )?.use { c ->
                if (c.moveToFirst()) {
                    return TagWriteStats(
                        fileSize = if (c.isNull(0)) null else c.getLong(0),
                        lastModified = if (c.isNull(1)) null else c.getLong(1),
                    )
                }
            }
        }
        runCatching {
            resolver.query(
                uri,
                arrayOf(OpenableColumns.SIZE, MediaStore.MediaColumns.DATE_MODIFIED),
                null, null, null,
            )?.use { c ->
                if (c.moveToFirst()) {
                    return TagWriteStats(
                        fileSize = if (c.isNull(0)) null else c.getLong(0),
                        lastModified = if (c.isNull(1)) null else c.getLong(1) * 1000L,
                    )
                }
            }
        }
        logger.w(TAG, "Could not query post-write stats for $uri")
        return TagWriteStats()
    }

    /** Set the field when [value] is non-blank, otherwise clear it. */
    private fun Tag.apply(key: FieldKey, value: String?) {
        if (value.isNullOrBlank()) {
            runCatching { deleteField(key) }
        } else {
            setField(key, value)
        }
    }

    private companion object {
        const val TAG = "JaudiotaggerTagWriter"
    }
}

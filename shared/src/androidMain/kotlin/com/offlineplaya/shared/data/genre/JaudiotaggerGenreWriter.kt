package com.offlineplaya.shared.data.genre

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.offlineplaya.shared.domain.genre.GenreTagWriter
import com.offlineplaya.shared.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.TagOptionSingleton
import java.io.File
import java.io.FileOutputStream

/**
 * [GenreTagWriter] backed by Jaudiotagger, using the same SAF temp-file dance
 * as [com.offlineplaya.shared.data.image.JaudiotaggerArtWriter]:
 *
 *  1. Copy the SAF document into a temp file inside app cache.
 *  2. Load that temp file with Jaudiotagger, set the genre tag, commit.
 *  3. Stream the modified bytes back over the original SAF URI
 *     (truncate-on-open via `openFileDescriptor("w")`).
 *
 * Per-track failures bubble up as `Result.failure` so the caller can count
 * them without aborting the whole batch.
 */
internal class JaudiotaggerGenreWriter(
    private val context: Context,
    private val logger: AppLogger,
) : GenreTagWriter {

    init {
        // Shared Jaudiotagger singleton setting — also flipped by the art
        // writer. Setting twice is idempotent; we do it here too so the genre
        // writer is safe to use without the art writer ever being constructed.
        TagOptionSingleton.getInstance().isAndroid = true
    }

    private companion object {
        const val TAG = "JaudiotaggerGenreWriter"
    }

    override suspend fun hasGenreTag(documentUri: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val tempFile = pullToTemp(documentUri) ?: return@withContext false
            try {
                val audioFile = AudioFileIO.read(tempFile)
                val genre = audioFile.tag?.getFirst(FieldKey.GENRE)
                !genre.isNullOrBlank()
            } finally {
                tempFile.delete()
            }
        }.getOrElse {
            logger.w(TAG, "hasGenreTag($documentUri) failed: ${it.message}")
            false
        }
    }

    override suspend fun writeGenre(
        documentUri: String,
        genre: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            logger.i(TAG, "Writing genre '$genre' to $documentUri")
            val uri = Uri.parse(documentUri)
            val resolver = context.contentResolver
            val tempFile = pullToTemp(documentUri)
                ?: error("Could not pull $documentUri into a temp file")

            try {
                // Step 2 — set the genre field on the temp file.
                val audioFile = AudioFileIO.read(tempFile)
                val tag = audioFile.tagOrCreateAndSetDefault
                tag.setField(FieldKey.GENRE, genre)
                audioFile.commit()
                logger.d(TAG, "Jaudiotagger committed genre change to temp file")

                // Step 3 — copy modified bytes back over the SAF doc.
                resolver.openFileDescriptor(uri, "w")?.use { pfd ->
                    FileOutputStream(pfd.fileDescriptor).use { output ->
                        tempFile.inputStream().use { input -> input.copyTo(output) }
                    }
                } ?: error("Could not open output FD for $uri")

                logger.i(TAG, "Successfully wrote genre back to $documentUri")
                Unit
            } finally {
                if (tempFile.exists()) tempFile.delete()
            }
        }.onFailure { logger.e(TAG, "writeGenre($documentUri) failed", it) }
    }

    /**
     * Pull the SAF document at [documentUri] into a temp file with the right
     * extension. Returns null when the SAF stream can't be opened — caller
     * decides whether that's fatal (writeGenre) or just "no, can't tell"
     * (hasGenreTag).
     */
    private fun pullToTemp(documentUri: String): File? {
        val uri = Uri.parse(documentUri)
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri)
        val extensionFromMime =
            mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
        val displayName = uri.lastPathSegment ?: "audio"
        val extensionFromUri = displayName
            .substringAfterLast('.', "")
            .substringBefore('?')
            .substringBefore('#')
        val extension = extensionFromMime ?: extensionFromUri.ifBlank { "mp3" }

        val tempFile = File.createTempFile("genre-tag-", ".$extension", context.cacheDir)
        return runCatching {
            resolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            } ?: error("Could not open input stream for $uri")
            tempFile
        }.getOrElse {
            tempFile.delete()
            null
        }
    }
}

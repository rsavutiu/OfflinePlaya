package com.offlineplaya.shared.data.image

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.offlineplaya.shared.data.util.resolveAudioExtension
import com.offlineplaya.shared.domain.image.AlbumArtWriter
import com.offlineplaya.shared.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.TagOptionSingleton
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File
import java.io.FileOutputStream

/**
 * [AlbumArtWriter] backed by jaudiotagger. Jaudiotagger needs a real
 * [File], not a content URI, so we do a three-step dance:
 *
 *  1. Copy the SAF document into a temp file inside app cache.
 *  2. Load that temp file with jaudiotagger, replace the artwork, commit.
 *  3. Stream the modified temp file back over the original SAF URI
 *     (truncate-on-open via `openFileDescriptor("w")`).
 *
 * Per-track failures are returned as `Result.failure` so the caller can
 * count them without aborting the whole batch.
 */
internal class JaudiotaggerArtWriter(
    private val context: Context,
    private val logger: AppLogger,
) : AlbumArtWriter {

    init {
        // Tell jaudiotagger to use AndroidArtwork (Bitmap-backed) rather
        // than StandardArtwork (java.awt-backed) which would crash on Android.
        TagOptionSingleton.getInstance().isAndroid = true
    }

    private companion object {
        const val TAG = "JaudiotaggerArtWriter"
    }

    override suspend fun hasEmbeddedArt(documentUri: String): Boolean =
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, Uri.parse(documentUri))
                val hasArt = retriever.embeddedPicture != null
                logger.d(TAG, "hasEmbeddedArt($documentUri) -> $hasArt")
                hasArt
            } catch (e: Throwable) {
                logger.w(TAG, "Failed to check embedded art for $documentUri: ${e.message}")
                false
            } finally {
                try {
                    retriever.release()
                } catch (_: Throwable) {
                    // ignore
                }
            }
        }

    override suspend fun write(
        documentUri: String,
        jpegBytes: ByteArray,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            logger.i(TAG, "Writing art to $documentUri (${jpegBytes.size} bytes)")
            val uri = Uri.parse(documentUri)
            val resolver = context.contentResolver
            val extension = resolveAudioExtension(context, uri)
            logger.d(TAG, "Determined extension: $extension for $documentUri")
            val tempFile = File.createTempFile("embed-art-", ".$extension", context.cacheDir)

            try {
                // Step 1 — pull SAF bytes into a temp file.
                resolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                } ?: error("Could not open input stream for $uri")

                // Step 2 — swap artwork in the temp file via jaudiotagger.
                val audioFile = AudioFileIO.read(tempFile)
                val tag = audioFile.tagOrCreateAndSetDefault
                val artwork = ArtworkFactory.getNew().apply {
                    binaryData = jpegBytes
                    setMimeType("image/jpeg")
                    pictureType = 0 // "Other" — universally supported value
                    description = ""
                }
                runCatching { tag.deleteArtworkField() }
                tag.setField(artwork)
                audioFile.commit()
                logger.d(TAG, "Jaudiotagger committed changes to temp file")

                // Step 3 — write the modified bytes back over the SAF doc.
                // `"w"` truncates on open per ParcelFileDescriptor docs, so we
                // don't leak trailing bytes when the new file is smaller.
                resolver.openFileDescriptor(uri, "w")?.use { pfd ->
                    FileOutputStream(pfd.fileDescriptor).use { output ->
                        tempFile.inputStream().use { input -> input.copyTo(output) }
                    }
                } ?: error("Could not open output FD for $uri")

                logger.i(TAG, "Successfully wrote art back to $documentUri")
                // Explicit Unit so runCatching infers Result<Unit>, not Result<Long>.
                Unit
            } catch (t: Throwable) {
                logger.e(TAG, "Embed failed for $documentUri", t)
                throw t
            } finally {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            }
        }
    }
}

package com.offlineplaya.shared.data.image

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import com.offlineplaya.shared.data.util.resolveAudioExtension
import com.offlineplaya.shared.domain.image.AlbumArtWriter
import com.offlineplaya.shared.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.flac.metadatablock.MetadataBlockDataPicture
import org.jaudiotagger.tag.TagOptionSingleton
import org.jaudiotagger.tag.flac.FlacTag
import org.jaudiotagger.tag.images.ArtworkFactory
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentFieldKey
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag
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
/**
 * Public factory exposing [JaudiotaggerArtWriter] to instrumentation tests
 * in the `androidApp` module without leaking the class itself onto the
 * production API surface. Production callers stay on the
 * [AlbumArtWriter] interface via DI; tests get a real implementation
 * pointed at a fixture file.
 */
fun createJaudiotaggerArtWriter(context: Context, logger: AppLogger): AlbumArtWriter =
    JaudiotaggerArtWriter(context, logger)

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
        const val PICTURE_TYPE_FRONT_COVER = 3

        // JPEG is 8 bits per channel × 3 channels (YCbCr). The FLAC PICTURE
        // block stores this as a single colour-depth integer; 24 is the
        // value every well-formed FLAC writer emits for JPEG cover art.
        const val JPEG_COLOUR_DEPTH = 24

        // 0 = non-palettized (JPEG never uses an indexed palette).
        const val INDEXED_COLOUR_COUNT_NON_INDEXED = 0
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
                // Tag-type dispatch is unavoidable: ID3 and MP4 atom-based
                // formats use the polymorphic `Tag.setField(Artwork)` path,
                // but FlacTag and VorbisCommentTag both call into
                // `AndroidArtwork.setImageFromData()` from there — and that
                // method is a UnsupportedOperationException stub in the
                // Android fork of jaudiotagger. For FLAC and Vorbis we
                // construct the PICTURE block ourselves and bypass the
                // throwing setter.
                val audioFile = AudioFileIO.read(tempFile)
                val tag = audioFile.tagOrCreateAndSetDefault
                val (artW, artH) = decodeJpegDimensions(jpegBytes)
                when (tag) {
                    is FlacTag -> writeFlacArtwork(tag, jpegBytes, artW, artH)
                    is VorbisCommentTag -> writeVorbisArtwork(tag, jpegBytes, artW, artH)
                    else -> writeViaArtworkField(tag, jpegBytes, artW, artH)
                }
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

    /**
     * Returns `(width, height)` for the JPEG bytes by decoding just the
     * header (`inJustDecodeBounds = true` — no pixel allocation, fast and
     * memory-safe even for multi-MB images). Falls back to `(0, 0)` if the
     * bytes don't parse, which still produces a writable PICTURE block —
     * the metadata block stays valid, the dimension fields are just
     * unhelpful.
     */
    private fun decodeJpegDimensions(jpegBytes: ByteArray): Pair<Int, Int> {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size, opts)
        val w = if (opts.outWidth > 0) opts.outWidth else 0
        val h = if (opts.outHeight > 0) opts.outHeight else 0
        return w to h
    }

    /**
     * ID3 / MP4 / etc. — the formats where `setField(Artwork)` works because
     * the underlying tag layer never calls back into `AndroidArtwork.getImage`
     * or `setImageFromData`. APIC frames embed raw JPEG bytes + MIME and don't
     * need width/height in the format itself, so this is the simple path.
     */
    private fun writeViaArtworkField(
        tag: org.jaudiotagger.tag.Tag,
        jpegBytes: ByteArray,
        artW: Int,
        artH: Int,
    ) {
        val artwork = ArtworkFactory.getNew().apply {
            binaryData = jpegBytes
            setMimeType("image/jpeg")
            pictureType = PICTURE_TYPE_FRONT_COVER
            description = ""
            width = artW
            height = artH
        }
        runCatching { tag.deleteArtworkField() }
        tag.setField(artwork)
    }

    /**
     * FLAC PICTURE block. Built directly via [MetadataBlockDataPicture] so
     * we never touch [org.jaudiotagger.tag.images.AndroidArtwork.setImageFromData],
     * which is a `throw UnsupportedOperationException` stub in the Android
     * fork of jaudiotagger. The `FlacTag.setField(MetadataBlockDataPicture)`
     * overload accepts a raw picture-block payload and writes it as-is.
     */
    private fun writeFlacArtwork(
        tag: FlacTag,
        jpegBytes: ByteArray,
        artW: Int,
        artH: Int,
    ) {
        runCatching { tag.deleteArtworkField() }
        val picture = MetadataBlockDataPicture(
            jpegBytes,
            PICTURE_TYPE_FRONT_COVER,
            "image/jpeg",
            "",
            artW,
            artH,
            JPEG_COLOUR_DEPTH,
            INDEXED_COLOUR_COUNT_NON_INDEXED,
        )
        tag.setField(picture)
    }

    /**
     * Vorbis Comment PICTURE storage. Vorbis stores the FLAC-style
     * PICTURE block as a base64-encoded value under the
     * `METADATA_BLOCK_PICTURE` comment, per the OGG/Vorbis spec
     * (xiph.org "Metadata Block Picture"). We construct the same
     * payload as FLAC, then base64-encode its raw bytes into a
     * Vorbis field.
     */
    private fun writeVorbisArtwork(
        tag: VorbisCommentTag,
        jpegBytes: ByteArray,
        artW: Int,
        artH: Int,
    ) {
        runCatching { tag.deleteArtworkField() }
        val picture = MetadataBlockDataPicture(
            jpegBytes,
            PICTURE_TYPE_FRONT_COVER,
            "image/jpeg",
            "",
            artW,
            artH,
            JPEG_COLOUR_DEPTH,
            INDEXED_COLOUR_COUNT_NON_INDEXED,
        )
        val base64 = Base64.encodeToString(picture.rawContent, Base64.NO_WRAP)
        tag.setField(tag.createField(VorbisCommentFieldKey.METADATA_BLOCK_PICTURE, base64))
    }

}

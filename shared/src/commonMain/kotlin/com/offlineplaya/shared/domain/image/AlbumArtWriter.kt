package com.offlineplaya.shared.domain.image

/**
 * Reads / writes embedded album art inside an audio file referenced by its
 * SAF document URI. Lives in `commonMain` because the use case orchestrates
 * across platforms; the actual implementation
 * ([com.offlineplaya.shared.data.image.JaudiotaggerArtWriter] on Android)
 * uses jaudiotagger via a temp-file dance.
 */
interface AlbumArtWriter {

    /**
     * Returns `true` when the audio file at [documentUri] already has
     * embedded album art, `false` when it doesn't (or when the file can't
     * be opened).
     */
    suspend fun hasEmbeddedArt(documentUri: String): Boolean

    /**
     * Replace the embedded album art at [documentUri] with [jpegBytes].
     * Returns `Result.success(Unit)` on success, `Result.failure` with the
     * underlying exception on any read/write error so the use case can
     * count failures without aborting the whole pass.
     */
    suspend fun write(documentUri: String, jpegBytes: ByteArray): Result<Unit>
}

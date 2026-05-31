package com.offlineplaya.shared.domain.genre

/**
 * Reads / writes the embedded genre tag inside an audio file referenced by
 * its SAF document URI. Mirrors
 * [com.offlineplaya.shared.domain.image.AlbumArtWriter] — same temp-file
 * dance on Android, same `Result<Unit>` contract so the use case can count
 * per-track failures without aborting the whole pass.
 *
 * "Genre tag" here means whatever the file's container natively supports:
 * ID3v2 `TCON` for MP3, Vorbis `GENRE` comment for FLAC / OGG, freeform
 * atom for M4A. Jaudiotagger normalises the surface.
 */
interface GenreTagWriter {

    /**
     * Returns `true` when the audio file at [documentUri] already carries a
     * non-blank genre tag. The auto-tag pass skips those — the user already
     * told us (or some prior pass already did).
     */
    suspend fun hasGenreTag(documentUri: String): Boolean

    /**
     * Write [genre] as the file's genre tag, replacing whatever was there.
     * Returns `Result.success(Unit)` on success, `Result.failure` with the
     * underlying exception otherwise. SAF write-permission is assumed —
     * callers grant it before calling.
     */
    suspend fun writeGenre(documentUri: String, genre: String): Result<Unit>
}

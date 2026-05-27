package com.offlineplaya.shared.data.image

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.offlineplaya.shared.domain.model.Track
import java.io.File

/**
 * Single source of truth for the on-disk album-art cache. Every consumer
 * (the Coil [TrackArtFetcher], the Android Auto FileProvider, the
 * Media3 queue mapper) routes through here so the cache-key convention
 * stays consistent.
 *
 * Layout:
 *  - Root dir: `cacheDir/track_art/`
 *  - Per-album entries: `album-art_<artistId>_<albumId>` — shared by every
 *    track on the same album so only one round-trip to MusicBrainz / Cover
 *    Art Archive is needed per cover.
 *  - Per-track fallback: `track-art_<hexDocUriHash>` — used when artist or
 *    album id is missing (pending-scan or pathological data).
 *
 * The cross-process URI form (`content://<applicationId>.artprovider/...`)
 * is exposed via [uriForTrack]. The matching `<provider>` declaration in
 * the manifest is the contract that makes the authority resolvable; only
 * the suffix lives here.
 */
object TrackArtCache {

    /** Subdirectory of [Context.getCacheDir] where art bytes are persisted. */
    const val SUBDIR: String = "track_art"

    /**
     * Suffix on the FileProvider authority. The full authority is built as
     * `<applicationId>$AUTHORITY_SUFFIX`. Must match the manifest entry.
     */
    const val AUTHORITY_SUFFIX: String = ".artprovider"

    /**
     * Stable cache slot for [track]'s album art. Returns the album-scoped
     * key when both artistId and albumId are known; falls back to a
     * per-track key derived from the document URI hash.
     *
     * Result is always a valid filename (no separators or shell-meta chars)
     * because callers use it as a file path component.
     */
    fun cacheKey(track: Track): String {
        val artistId = track.artistId
        val albumId = track.albumId
        return if (artistId != null && albumId != null) {
            cacheKeyForAlbum(artistId, albumId)
        } else {
            "track-art_${track.documentUri.hashCode().toUInt().toString(16)}"
        }
    }

    /**
     * Album-scoped key form. Use when you have only an [Album] (no concrete
     * Track) — for instance when rendering a browse row in Android Auto.
     * Returns null when `artistId` is missing; the cache file for that
     * album was either written under the per-track fallback or never
     * existed.
     */
    fun cacheKeyForAlbum(artistId: Long?, albumId: Long): String? {
        if (artistId == null) return null
        return cacheKeyForAlbum(artistId, albumId)
    }

    private fun cacheKeyForAlbum(artistId: Long, albumId: Long): String =
        "album-art_${artistId}_${albumId}"

    /** Root directory for cached art files. Creates the directory on first call. */
    fun cacheDir(context: Context): File =
        File(context.cacheDir, SUBDIR).also { it.mkdirs() }

    /** Resolve [track] to its cache file. Always non-null — the file may not exist yet. */
    fun cacheFile(context: Context, track: Track): File =
        File(cacheDir(context), cacheKey(track))

    /** Resolve a raw [key] to its cache file. For album-only callers. */
    fun cacheFile(context: Context, key: String): File =
        File(cacheDir(context), key)

    /** FileProvider authority computed from the runtime application id. */
    fun authority(context: Context): String =
        context.packageName + AUTHORITY_SUFFIX

    /**
     * `content://` URI for [track]'s cached cover, or null when the file
     * isn't on disk yet. Auto/lock-screen surfaces handle 404s gracefully
     * but probing here saves the consumer a wasted request.
     */
    fun uriForTrack(context: Context, track: Track): Uri? =
        uriForFile(context, cacheFile(context, track))

    /**
     * `content://` URI for the album-scoped slot. Returns null when the
     * album has no artistId (key can't be formed) or when the file isn't
     * yet on disk.
     */
    fun uriForAlbum(context: Context, artistId: Long?, albumId: Long): Uri? {
        val key = cacheKeyForAlbum(artistId, albumId) ?: return null
        return uriForFile(context, cacheFile(context, key))
    }

    private fun uriForFile(context: Context, file: File): Uri? {
        if (!file.exists()) return null
        return runCatching {
            FileProvider.getUriForFile(context, authority(context), file)
        }.getOrNull()
    }
}

package com.offlineplaya.android.auto

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.offlineplaya.shared.domain.model.Album
import com.offlineplaya.shared.domain.model.Track
import java.io.File

/**
 * Trivial subclass of [androidx.core.content.FileProvider] declared in the
 * manifest with its own [AUTHORITY]. The subclass exists purely so the
 * authority namespace can't collide with other libraries' default
 * `FileProvider` declarations — every consumer using
 * [androidx.core.content.FileProvider] under one applicationId would clash.
 *
 * The shared cover-art cache at `cacheDir/track_art/<key>` is exposed via
 * this provider so:
 *   - Android Auto can fetch row + now-playing artwork via `content://`
 *     URIs (it can't read app-private cache files directly).
 *   - Future cross-process surfaces (e.g. a homescreen widget) get the
 *     same path for free.
 *
 * Helpers ([uriForTrack], [uriForAlbum]) compute the same cache key as
 * `TrackArtFetcher.trackArtCacheKey` — duplicated here because that helper
 * is `internal` to the shared module and exposing it cross-module would
 * leak an implementation detail. Five lines of duplication is the lesser
 * evil.
 */
class AppArtFileProvider : FileProvider() {

    companion object {
        /**
         * Must match the `android:authorities` attribute in the manifest.
         * Suffixed with the package name at runtime via [authority].
         */
        const val AUTHORITY_SUFFIX = ".artprovider"

        /** Subdirectory of [Context.getCacheDir] where art bytes are persisted. */
        const val CACHE_SUBDIR = "track_art"

        fun authority(context: Context): String = context.packageName + AUTHORITY_SUFFIX

        /**
         * `content://` URI for [track]'s cover art, or null when no cached
         * file exists. Auto handles 404s gracefully (renders a generic
         * placeholder) so returning a non-existent path is "safe" too —
         * but probing the filesystem here saves the head unit a round
         * trip when art is truly missing.
         */
        fun uriForTrack(context: Context, track: Track): Uri? {
            val file = cacheFileForTrack(context, track) ?: return null
            if (!file.exists()) return null
            return getUriForFile(context, authority(context), file)
        }

        /**
         * `content://` URI for the album-level cache key. Mirrors the
         * "album-art_<artistId>_<albumId>" form `TrackArtFetcher` writes
         * when both ids are known on the track. Returns null if [album]
         * has no artist (then the cache key would degrade to the track-
         * level form, which we can't reconstruct without a concrete track).
         */
        fun uriForAlbum(context: Context, album: Album): Uri? {
            val artistId = album.artistId ?: return null
            val key = "album-art_${artistId}_${album.id}"
            val file = File(cacheDir(context), key)
            if (!file.exists()) return null
            return getUriForFile(context, authority(context), file)
        }

        private fun cacheFileForTrack(context: Context, track: Track): File? {
            val key = trackArtCacheKey(track) ?: return null
            return File(cacheDir(context), key)
        }

        private fun cacheDir(context: Context): File =
            File(context.cacheDir, CACHE_SUBDIR)

        /**
         * Mirrors `TrackArtFetcher.trackArtCacheKey` exactly. Duplicated
         * because the original is module-internal. If the cache-key
         * convention changes there, update here too.
         */
        private fun trackArtCacheKey(track: Track): String? {
            val artistId = track.artistId
            val albumId = track.albumId
            return if (artistId != null && albumId != null) {
                "album-art_${artistId}_${albumId}"
            } else {
                "track-art_${track.documentUri.hashCode().toUInt().toString(16)}"
            }
        }
    }
}

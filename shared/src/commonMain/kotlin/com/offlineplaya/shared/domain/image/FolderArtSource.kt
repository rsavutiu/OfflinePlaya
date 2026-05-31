package com.offlineplaya.shared.domain.image

import com.offlineplaya.shared.domain.model.Track

/**
 * Looks for a sidecar cover image sitting alongside a track in the same
 * folder — the conventional `cover.jpg` / `folder.jpg` / `album.jpg` /
 * `front.jpg` files that ripping tools and torrent releases ship with.
 *
 * Returns the raw image bytes, or `null` when no sidecar is present.
 * Implementations should treat absence as the common case (most folders
 * won't have one) and fail silently — callers fall through to the next
 * source in the chain (remote lookup).
 */
interface FolderArtSource {
    suspend fun findInFolder(track: Track): ByteArray?
}

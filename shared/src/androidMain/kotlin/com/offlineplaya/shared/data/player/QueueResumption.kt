package com.offlineplaya.shared.data.player

import android.content.Context
import androidx.media3.session.MediaSession
import com.offlineplaya.shared.data.image.TrackArtCache
import com.offlineplaya.shared.domain.repository.QueueRepository

/**
 * Builds the Media3 playback-resumption payload from the persisted queue.
 * The session callback uses this when the system asks a *dead* service what
 * it was playing — Bluetooth/headset play button, or the lock-screen media
 * controls Android 13+ keeps across reboots. Returns `null` when there is
 * nothing to resume (the caller reports that as an unsupported request).
 *
 * Lives in the shared module because [TrackMediaItemMapper] is internal
 * here — the app module only sees the finished payload.
 */
suspend fun loadPersistedQueueForResumption(
    context: Context,
    queue: QueueRepository,
): MediaSession.MediaItemsWithStartPosition? {
    val tracks = queue.loadQueue()
    if (tracks.isEmpty()) return null
    val snapshot = queue.loadPlaybackSnapshot()
    val items = tracks.map {
        TrackMediaItemMapper.toMediaItem(it, TrackArtCache.uriForTrack(context, it))
    }
    return MediaSession.MediaItemsWithStartPosition(
        items,
        (snapshot?.queueIndex ?: 0).coerceIn(0, items.size - 1),
        (snapshot?.positionMs ?: 0L).coerceAtLeast(0L),
    )
}

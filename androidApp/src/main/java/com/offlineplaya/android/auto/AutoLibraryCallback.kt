package com.offlineplaya.android.auto

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.offlineplaya.shared.data.image.TrackArtCache
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.auto.BrowseEntry
import com.offlineplaya.shared.presentation.auto.BrowseTreeBuilder
import com.offlineplaya.shared.presentation.auto.MediaIdRouter
import com.offlineplaya.shared.presentation.auto.MediaIdRouter.ParentContext
import com.offlineplaya.shared.presentation.library.LibraryStateHolder
import com.offlineplaya.shared.presentation.playlist.PlaylistStateHolder
import com.offlineplaya.shared.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.google.common.collect.ImmutableList

/**
 * Wires Android Auto's browse-tree IPC into the existing library + player
 * state holders. The pure routing lives in
 * [com.offlineplaya.shared.presentation.auto.MediaIdRouter] /
 * [com.offlineplaya.shared.presentation.auto.BrowseTreeBuilder]; this class
 * is the platform glue that turns [BrowseEntry] lists into
 * [androidx.media3.common.MediaItem] and bridges suspend functions into
 * [com.google.common.util.concurrent.ListenableFuture].
 *
 * The callback methods get called from the head unit on its own thread —
 * we use [scope] to dispatch suspend work onto the service-scoped
 * coroutine context, then fulfill a [SettableFuture] back to media3.
 */
class AutoLibraryCallback(
    private val context: Context,
    private val library: LibraryStateHolder,
    private val playlists: PlaylistStateHolder,
    private val scope: CoroutineScope,
    private val logger: AppLogger,
) : MediaLibrarySession.Callback {

    /**
     * Lazy because [BrowseTreeBuilder] takes a lookup for the per-album
     * artist name — and resolving that means hitting [LibraryStateHolder],
     * which we receive in the constructor. Snapshotting the artists map
     * once per call keeps subtitle lookups O(1) without holding stale data.
     */
    private val treeBuilder: BrowseTreeBuilder by lazy {
        BrowseTreeBuilder(
            artistNameByAlbum = { artistId ->
                if (artistId == null) null
                else library.allArtists.value.firstOrNull { it.id == artistId }?.name
            },
            albumArtUri = { album ->
                TrackArtCache.uriForAlbum(context, album.artistId, album.id)?.toString()
            },
            trackArtUri = { track ->
                TrackArtCache.uriForTrack(context, track)?.toString()
            },
        )
    }

    // ─── Root ────────────────────────────────────────────────────────────

    override fun onGetLibraryRoot(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: LibraryParams?,
    ): ListenableFuture<LibraryResult<MediaItem>> {
        val root = MediaItem.Builder()
            .setMediaId(MediaIdRouter.ROOT)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("OfflinePlaya")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .build(),
            )
            .build()
        return Futures.immediateFuture(LibraryResult.ofItem(root, params))
    }

    // ─── Children ────────────────────────────────────────────────────────

    override fun onGetChildren(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        // page / pageSize are honoured by Auto but we don't paginate in v1.
        // We always return the full (capped) list and accept the truncate.
        val parsed = MediaIdRouter.parse(parentId)
        logger.d(TAG, "onGetChildren parentId=$parentId parsed=$parsed")

        // Top-level synchronous nodes — answered without coroutine bridging.
        when {
            parentId == MediaIdRouter.ROOT -> {
                return immediate(treeBuilder.rootChildren().toMediaItems())
            }
            parentId == MediaIdRouter.NODE_RECENTS -> {
                val recents = library.allAlbums.value.take(BrowseTreeBuilder.RECENT_ALBUMS_LIMIT)
                return immediate(treeBuilder.albumsAsBrowsable(recents).toMediaItems())
            }
            parentId == MediaIdRouter.NODE_ALBUMS -> {
                return immediate(treeBuilder.albumsAsBrowsable(library.allAlbums.value).toMediaItems())
            }
            parentId == MediaIdRouter.NODE_ARTISTS -> {
                return immediate(treeBuilder.artistsAsBrowsable(library.allArtists.value).toMediaItems())
            }
            parentId == MediaIdRouter.NODE_PLAYLISTS -> {
                return immediate(
                    treeBuilder.playlistsAsBrowsable(playlists.allPlaylists.value).toMediaItems(),
                )
            }
        }

        // Entity ids — `album/123`, `artist/45`, `playlist/7`. These need a
        // suspend flow read; bridge with a SettableFuture.
        if (parsed !is MediaIdRouter.ParsedId.Entity) {
            return immediate(emptyList())
        }
        val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
        scope.launch {
            try {
                val children = when (parsed.kind) {
                    MediaIdRouter.Kind.ALBUM -> {
                        val tracks = library.tracksByAlbum(parsed.id).first()
                        treeBuilder.tracksAsPlayable(tracks, ParentContext.Album(parsed.id))
                    }
                    MediaIdRouter.Kind.ARTIST -> {
                        val albums = library.albumsByArtist(parsed.id).first()
                        treeBuilder.albumsAsBrowsable(albums)
                    }
                    MediaIdRouter.Kind.PLAYLIST -> {
                        val tracks = playlists.tracksIn(parsed.id).first()
                        treeBuilder.tracksAsPlayable(tracks, ParentContext.Playlist(parsed.id))
                    }
                    MediaIdRouter.Kind.TRACK -> emptyList()
                }
                future.set(
                    LibraryResult.ofItemList(
                        ImmutableList.copyOf(children.toMediaItems()),
                        params,
                    ),
                )
            } catch (t: Throwable) {
                logger.e(TAG, "onGetChildren failed for $parentId", t)
                future.set(
                    LibraryResult.ofError(LibraryResult.RESULT_ERROR_UNKNOWN),
                )
            }
        }
        return future
    }

    // ─── Single item ────────────────────────────────────────────────────

    override fun onGetItem(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String,
    ): ListenableFuture<LibraryResult<MediaItem>> {
        val parsed = MediaIdRouter.parse(mediaId)
            ?: return Futures.immediateFuture(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
        if (parsed !is MediaIdRouter.ParsedId.Entity) {
            return Futures.immediateFuture(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
        }
        // Synthesize the MediaItem from the parsed id — head units typically
        // call this to refresh a row, so the shape just needs to match what
        // we returned from onGetChildren.
        val item = MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsBrowsable(parsed.kind != MediaIdRouter.Kind.TRACK)
                    .setIsPlayable(parsed.kind == MediaIdRouter.Kind.TRACK)
                    .build(),
            )
            .build()
        return Futures.immediateFuture(LibraryResult.ofItem(item, /* params = */ null))
    }

    // ─── Play action ────────────────────────────────────────────────────

    /**
     * Items that already carry a URI (the in-app flow built by
     * [com.offlineplaya.shared.data.player.TrackMediaItemMapper]) are
     * passed through unchanged — they're complete and ready for the
     * session's player to load. Items that arrive as stubs from Android
     * Auto (mediaId only, no URI) are resolved through the router into
     * one or more concrete playable items.
     *
     * Critically: this method does NOT call [musicPlayer.setQueue] — the
     * media3 session player picks up the returned items directly. Going
     * back through MusicPlayer would re-enter this very callback via the
     * in-app MediaController, looping.
     */
    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
    ): ListenableFuture<MutableList<MediaItem>> {
        if (mediaItems.all { it.localConfiguration?.uri != null }) {
            // In-app fast path: nothing to resolve.
            return Futures.immediateFuture(mediaItems)
        }
        // Auto path: stubs need URI resolution.
        val future = SettableFuture.create<MutableList<MediaItem>>()
        scope.launch {
            try {
                future.set(resolveAutoStubs(mediaItems).toMutableList())
            } catch (t: Throwable) {
                logger.e(TAG, "onAddMediaItems resolve failed", t)
                future.set(mutableListOf())
            }
        }
        return future
    }

    /**
     * Auto entry point for "user tapped a playable row." Returns both the
     * full queue and the index the player should start at, so a track
     * tapped inside an album plays from that track with the rest of the
     * album queued behind it.
     *
     * For in-app callers (Media3MusicPlayer.setQueue), [mediaItems] already
     * have URIs and a meaningful [startIndex]; we pass through unchanged.
     */
    override fun onSetMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        if (mediaItems.all { it.localConfiguration?.uri != null }) {
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(
                    ImmutableList.copyOf(mediaItems),
                    startIndex,
                    startPositionMs,
                ),
            )
        }
        val first = mediaItems.firstOrNull()
            ?: return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(ImmutableList.of(), 0, 0L),
            )
        val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
        scope.launch {
            try {
                val parsed = MediaIdRouter.parse(first.mediaId)
                val (tracks, idx) = buildPlaybackQueue(parsed, first.mediaId)
                val items = ImmutableList.copyOf(tracks.map { it.toAutoMediaItem() })
                future.set(MediaSession.MediaItemsWithStartPosition(items, idx, 0L))
            } catch (t: Throwable) {
                logger.e(TAG, "onSetMediaItems resolve failed", t)
                future.set(
                    MediaSession.MediaItemsWithStartPosition(ImmutableList.of(), 0, 0L),
                )
            }
        }
        return future
    }

    /**
     * Expand a list of Auto stub MediaItems (each containing only a
     * router-encoded mediaId) into concrete playable MediaItems. Each stub
     * is treated independently; a single tap that resolves to N tracks
     * contributes those N to the returned list.
     */
    private suspend fun resolveAutoStubs(stubs: List<MediaItem>): List<MediaItem> =
        stubs.flatMap { stub ->
            val parsed = MediaIdRouter.parse(stub.mediaId)
            if (parsed !is MediaIdRouter.ParsedId.Entity) {
                emptyList()
            } else {
                val (tracks, _) = buildPlaybackQueue(parsed, stub.mediaId)
                tracks.map { it.toAutoMediaItem() }
            }
        }

    /**
     * Given a parsed tap, expand into (queue, startIndex). Tap on a track
     * with a parent context loads the parent's full track list. Tap on a
     * track with no parent → queue is just that track.
     */
    private suspend fun buildPlaybackQueue(
        parsed: MediaIdRouter.ParsedId?,
        rawMediaId: String,
    ): Pair<List<Track>, Int> {
        if (parsed !is MediaIdRouter.ParsedId.Entity) return emptyList<Track>() to 0
        return when (parsed.kind) {
            MediaIdRouter.Kind.TRACK -> {
                val tracks = when (val p = parsed.parent) {
                    is ParentContext.Album -> library.tracksByAlbum(p.id).first()
                    is ParentContext.Playlist -> playlists.tracksIn(p.id).first()
                    is ParentContext.Artist -> library.tracksByArtist(p.id).first()
                    null -> {
                        // Single-track queue. We'd need a `findTrackById`
                        // entry on LibraryStateHolder to load it cleanly;
                        // fall back to scanning allTracks for now.
                        val all = library.allTracks.value
                        listOfNotNull(all.firstOrNull { it.id == parsed.id })
                    }
                }
                val idx = tracks.indexOfFirst { it.id == parsed.id }.coerceAtLeast(0)
                tracks to idx
            }
            MediaIdRouter.Kind.ALBUM -> library.tracksByAlbum(parsed.id).first() to 0
            MediaIdRouter.Kind.PLAYLIST -> playlists.tracksIn(parsed.id).first() to 0
            MediaIdRouter.Kind.ARTIST -> library.tracksByArtist(parsed.id).first() to 0
        }
    }

    // ─── Helpers ────────────────────────────────────────────────────────

    private fun immediate(items: List<MediaItem>): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> =
        Futures.immediateFuture(
            LibraryResult.ofItemList(ImmutableList.copyOf(items), /* params = */ null),
        )

    private fun List<BrowseEntry>.toMediaItems(): List<MediaItem> = map { it.toMediaItem() }

    private fun BrowseEntry.toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .apply { subtitle?.let { setSubtitle(it) } }
            .apply { artworkUri?.let { setArtworkUri(Uri.parse(it)) } }
            .setIsBrowsable(isBrowsable)
            .setIsPlayable(isPlayable)
            .setMediaType(
                if (isPlayable) MediaMetadata.MEDIA_TYPE_MUSIC
                else MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
            )
            .build()
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(metadata)
            .build()
    }

    /**
     * Track → MediaItem with `setUri` so media3's session can resolve the
     * source. The `TrackMediaItemMapper` in androidMain does the same thing
     * for in-app playback; we duplicate the minimum here to keep this file
     * self-contained.
     */
    private fun Track.toAutoMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(MediaIdRouter.trackId(id))
        .setUri(documentUri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artistName)
                .setAlbumTitle(albumName)
                .apply {
                    TrackArtCache.uriForTrack(context, this@toAutoMediaItem)
                        ?.let { setArtworkUri(it) }
                }
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .build(),
        )
        .build()

    private companion object {
        const val TAG = "AutoLibraryCallback"
    }
}

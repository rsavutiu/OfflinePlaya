package com.offlineplaya.shared.presentation.auto

import com.offlineplaya.shared.domain.model.Album
import com.offlineplaya.shared.domain.model.Artist
import com.offlineplaya.shared.domain.model.Playlist
import com.offlineplaya.shared.domain.model.Track

/**
 * Pure projection from library snapshot → [BrowseEntry] lists keyed by the
 * media id of the parent node. The Android Auto callback queries this with
 * the parentId it received in `onGetChildren` and converts the result into
 * `MediaItem`s; no business logic on the platform side.
 *
 * The MVP browse tree is deliberately shallow — two levels deep at most —
 * so head-unit UIs don't bury content behind taps:
 *
 *  root
 *  ├── recents      → up to 20 albums (alphabetical for v1; real "recent"
 *  │                  tracking is a follow-up)
 *  ├── albums       → all albums (capped — see [MAX_TOP_LEVEL_RESULTS])
 *  ├── artists      → all artists → list of that artist's albums
 *  └── playlists    → all playlists
 *
 *  album/{id}       → tracks (playable)
 *  artist/{id}      → albums (browsable)
 *  playlist/{id}    → tracks (playable)
 *
 * Folder browsing is intentionally skipped for v1 — SAF tree-uri folder
 * names are noisy in a car UI and the value-per-tap is low.
 */
class BrowseTreeBuilder(
    private val artistNameByAlbum: (artistId: Long?) -> String? = { null },
    private val albumArtUri: (album: Album) -> String? = { null },
    private val trackArtUri: (track: Track) -> String? = { null },
) {

    /**
     * Top-level browsable children of the root node. Always exactly four
     * rows, in stable order — Auto remembers a head unit's last-position
     * by index, so re-ordering would jump the user.
     */
    fun rootChildren(): List<BrowseEntry> = listOf(
        BrowseEntry(
            mediaId = MediaIdRouter.NODE_RECENTS,
            title = "Recents",
            isBrowsable = true,
            // Children are albums → show their covers as a grid of tiles.
            childrenStyle = BrowseStyle.GRID,
        ),
        BrowseEntry(
            mediaId = MediaIdRouter.NODE_ALBUMS,
            title = "Albums",
            isBrowsable = true,
            childrenStyle = BrowseStyle.GRID,
        ),
        BrowseEntry(
            mediaId = MediaIdRouter.NODE_ARTISTS,
            title = "Artists",
            isBrowsable = true,
            // Artist rows carry no artwork — a list reads better than empty tiles.
            childrenStyle = BrowseStyle.LIST,
        ),
        BrowseEntry(
            mediaId = MediaIdRouter.NODE_PLAYLISTS,
            title = "Playlists",
            isBrowsable = true,
            childrenStyle = BrowseStyle.LIST,
        ),
    )

    /**
     * Project a list of albums onto browse rows. Used by the `albums` and
     * `recents` nodes; for `recents` the caller pre-slices the list to the
     * top N.
     */
    fun albumsAsBrowsable(albums: List<Album>): List<BrowseEntry> =
        albums.take(MAX_TOP_LEVEL_RESULTS).map { album ->
            BrowseEntry(
                mediaId = MediaIdRouter.albumId(album.id),
                title = album.name,
                subtitle = artistNameByAlbum(album.artistId),
                isBrowsable = true,
                artworkUri = albumArtUri(album),
                // Children are tracks → a plain list, not tiles.
                childrenStyle = BrowseStyle.LIST,
            )
        }

    fun artistsAsBrowsable(artists: List<Artist>): List<BrowseEntry> =
        artists.take(MAX_TOP_LEVEL_RESULTS).map { artist ->
            BrowseEntry(
                mediaId = MediaIdRouter.artistId(artist.id),
                title = artist.name,
                subtitle = pluralCount(artist.albumCount, "album", "albums"),
                isBrowsable = true,
                // Drilling into an artist shows their albums → grid of covers.
                childrenStyle = BrowseStyle.GRID,
            )
        }

    fun playlistsAsBrowsable(playlists: List<Playlist>): List<BrowseEntry> =
        playlists.take(MAX_TOP_LEVEL_RESULTS).map { playlist ->
            BrowseEntry(
                mediaId = MediaIdRouter.playlistId(playlist.id),
                title = playlist.name,
                isBrowsable = true,
                // Children are tracks → list.
                childrenStyle = BrowseStyle.LIST,
            )
        }

    /**
     * Tracks inside an album/playlist surface as playable rows. The parent
     * context is baked into each track's media id so playback can rebuild
     * the surrounding queue when the user taps one.
     */
    fun tracksAsPlayable(
        tracks: List<Track>,
        parent: MediaIdRouter.ParentContext,
    ): List<BrowseEntry> =
        tracks.take(MAX_TRACKS_PER_NODE).map { track ->
            BrowseEntry(
                mediaId = MediaIdRouter.trackId(track.id, parent),
                title = track.title,
                subtitle = track.artistName,
                isPlayable = true,
                trackId = track.id,
                artworkUri = trackArtUri(track),
            )
        }

    /**
     * Render the singular/plural count line for an artist row.
     * Returns null when the count is zero so an "0 albums" line doesn't
     * waste the row's secondary text.
     */
    private fun pluralCount(n: Int, singular: String, plural: String): String? = when {
        n <= 0 -> null
        n == 1 -> "1 $singular"
        else -> "$n $plural"
    }

    companion object {
        /**
         * Auto recommends ≤100 items per browse node. We cap rather than
         * paginate v1 — explicit todo: switch to MediaConstants
         * EXTRAS_KEY_FETCH_BATCH_* once we ship a real Auto-targeting
         * release.
         */
        const val MAX_TOP_LEVEL_RESULTS = 100

        /**
         * Per-album / per-playlist track cap. Auto handles up to ~500 but
         * head-unit UIs lag past ~200 — staying conservative.
         */
        const val MAX_TRACKS_PER_NODE = 200

        /** Number of recent albums surfaced under the "Recents" node. */
        const val RECENT_ALBUMS_LIMIT = 20
    }
}

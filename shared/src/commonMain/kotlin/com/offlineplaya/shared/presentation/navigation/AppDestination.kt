package com.offlineplaya.shared.presentation.navigation

/**
 * Every reachable destination in the app. Adding a new screen means
 * extending this sealed hierarchy + handling it in the `App` composable.
 *
 * Library destinations are already declared so [AppNavigator] is ready for
 * Substage C without further changes; the matching pages will fill in.
 */
sealed interface AppDestination {
    data object Home : AppDestination
    data object Settings : AppDestination
    data object NowPlaying : AppDestination
    data object Queue : AppDestination

    data object Playlists : AppDestination
    data class PlaylistDetail(val playlistId: Long) : AppDestination

    /** Built-in history-derived playlist (Most played, Recently added, …). */
    data class SmartPlaylist(
        val kind: com.offlineplaya.shared.domain.model.SmartPlaylistKind,
    ) : AppDestination

    data object Search : AppDestination

    data object LibraryArtists : AppDestination
    data class LibraryArtistDetail(val artistId: Long) : AppDestination
    data class LibraryAlbumDetail(val albumId: Long) : AppDestination

    data object LibraryFolderRoots : AppDestination
    data class LibraryFolderDetail(val folderId: Long) : AppDestination

    data object LibraryFlat : AppDestination

    /** Internal design-system catalog — every token rendered on one page. */
    data object DesignSystemGallery : AppDestination

    /** Equalizer page — accessible from NowPlaying and from Settings. */
    data object Equalizer : AppDestination

    /** Lyrics page — accessible from the NowPlaying aux row. */
    data object Lyrics : AppDestination

    /** Manual tag editor for a single track — reached from the track actions sheet. */
    data class TagEditor(val trackId: Long) : AppDestination
}

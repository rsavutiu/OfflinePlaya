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

    data object LibraryArtists : AppDestination
    data class LibraryArtistDetail(val artistId: Long) : AppDestination
    data class LibraryAlbumDetail(val albumId: Long) : AppDestination

    data object LibraryFolderRoots : AppDestination
    data class LibraryFolderDetail(val folderId: Long) : AppDestination

    data object LibraryFlat : AppDestination
}

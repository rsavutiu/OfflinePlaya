package com.offlineplaya.shared.presentation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.offlineplaya.shared.domain.model.Album
import com.offlineplaya.shared.domain.model.Artist
import com.offlineplaya.shared.domain.model.ColorMode
import com.offlineplaya.shared.domain.model.Folder
import com.offlineplaya.shared.domain.model.PlaybackState
import com.offlineplaya.shared.domain.model.ThemePreferences
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.player.MusicPlayer
import com.offlineplaya.shared.presentation.library.LibraryStateHolder
import com.offlineplaya.shared.presentation.navigation.AppDestination
import com.offlineplaya.shared.presentation.navigation.AppNavigator
import com.offlineplaya.shared.presentation.playlist.PlaylistStateHolder
import com.offlineplaya.shared.presentation.sync.SyncStatus
import com.offlineplaya.shared.presentation.ui.molecules.LibraryTab
import com.offlineplaya.shared.presentation.ui.organisms.MiniPlayer
import com.offlineplaya.shared.presentation.ui.pages.HomePage
import com.offlineplaya.shared.presentation.ui.pages.LibraryAlbumDetailPage
import com.offlineplaya.shared.presentation.ui.pages.LibraryArtistDetailPage
import com.offlineplaya.shared.presentation.ui.pages.LibraryArtistsPage
import com.offlineplaya.shared.presentation.ui.pages.LibraryFlatPage
import com.offlineplaya.shared.presentation.ui.pages.LibraryFolderDetailPage
import com.offlineplaya.shared.presentation.ui.pages.LibraryFolderRootsPage
import com.offlineplaya.shared.presentation.ui.pages.NowPlayingPage
import com.offlineplaya.shared.presentation.ui.pages.PlaylistDetailPage
import com.offlineplaya.shared.presentation.ui.pages.PlaylistsPage
import com.offlineplaya.shared.presentation.ui.pages.QueuePage
import com.offlineplaya.shared.presentation.ui.pages.SearchPage
import com.offlineplaya.shared.presentation.ui.pages.SettingsPage
import com.offlineplaya.shared.presentation.ui.theme.OfflinePlayaTheme

/**
 * Shared app entry. Renders whichever destination is on top of the navigator's
 * stack, applies the user's chosen theme, threads library data into the
 * Library destinations, and floats a [MiniPlayer] above non-NowPlaying pages
 * whenever something is queued.
 */
@Composable
fun App(
    navigator: AppNavigator,
    library: LibraryStateHolder,
    playlists: PlaylistStateHolder,
    syncCoordinator: com.offlineplaya.shared.presentation.sync.LibrarySyncCoordinator,
    embedArtCoordinator: com.offlineplaya.shared.presentation.artwork.EmbedArtCoordinator,
    musicPlayer: MusicPlayer,
    themePreferences: ThemePreferences,
    artworkPreferences: com.offlineplaya.shared.domain.model.ArtworkPreferences,
    hasWritePermission: Boolean,
    syncStatus: SyncStatus,
    trackCount: Long,
    onPickFolder: () -> Unit,
    onColorModeChange: (ColorMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onDownloadRemoteArtChange: (Boolean) -> Unit,
    onEmbedDownloadedArtChange: (Boolean) -> Unit,
    onRequestWritePermission: () -> Unit,
    dynamicColorSupported: Boolean,
) {
    OfflinePlayaTheme(preferences = themePreferences) {
        val stack by navigator.stack.collectAsState()
        val current = stack.last()
        val playback by musicPlayer.playbackState.collectAsState()

        val availablePlaylists by playlists.allPlaylists.collectAsState()
        val embedReport by embedArtCoordinator.report.collectAsState()

        val onTabSelected: (LibraryTab) -> Unit = { tab ->
            navigator.swapTop(tab.toDestination())
        }
        val onPlayTracks: (List<Track>, Int) -> Unit = { tracks, index ->
            musicPlayer.setQueue(tracks, index)
            navigator.push(AppDestination.NowPlaying)
        }
        val onPlayNext: (Track) -> Unit = { musicPlayer.addNext(it) }
        val onAddToQueue: (Track) -> Unit = { musicPlayer.addToQueue(it) }
        val onAddToPlaylist: (Track, Long) -> Unit = { track, playlistId ->
            playlists.addTrack(playlistId, track.id)
        }

        // Single outer Scaffold owns all system insets. Inner page Scaffolds
        // opt out via `contentWindowInsets = WindowInsets(0)` to avoid double
        // padding. The MiniPlayer slots in as `bottomBar` so its background
        // extends behind the navigation bar while its content sits above it.
        val showMini = playback.currentTrack != null && current != AppDestination.NowPlaying
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                AnimatedVisibility(visible = showMini) {
                    MiniPlayer(
                        state = playback,
                        onExpand = { navigator.push(AppDestination.NowPlaying) },
                        onPlayPause = {
                            if (playback.isPlaying) musicPlayer.pause() else musicPlayer.play()
                        },
                        onPrevious = { musicPlayer.skipToPrevious() },
                        onNext = { musicPlayer.skipToNext() },
                    )
                }
            },
        ) { outerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(outerPadding)
                    .consumeWindowInsets(outerPadding),
            ) {
                DestinationContent(
                    current = current,
                    navigator = navigator,
                    library = library,
                    playlists = playlists,
                    availablePlaylists = availablePlaylists,
                    syncCoordinator = syncCoordinator,
                    embedArtCoordinator = embedArtCoordinator,
                    embedReport = embedReport,
                    musicPlayer = musicPlayer,
                    playback = playback,
                    themePreferences = themePreferences,
                    artworkPreferences = artworkPreferences,
                    hasWritePermission = hasWritePermission,
                    syncStatus = syncStatus,
                    trackCount = trackCount,
                    onPickFolder = onPickFolder,
                    onColorModeChange = onColorModeChange,
                    onDynamicColorChange = onDynamicColorChange,
                    onDownloadRemoteArtChange = onDownloadRemoteArtChange,
                    onEmbedDownloadedArtChange = onEmbedDownloadedArtChange,
                    onRequestWritePermission = onRequestWritePermission,
                    dynamicColorSupported = dynamicColorSupported,
                    onTabSelected = onTabSelected,
                    onPlayTracks = onPlayTracks,
                    onPlayNext = onPlayNext,
                    onAddToQueue = onAddToQueue,
                    onAddToPlaylist = onAddToPlaylist,
                )
            }
        }
    }
}

@Composable
private fun DestinationContent(
    current: AppDestination,
    navigator: AppNavigator,
    library: LibraryStateHolder,
    playlists: PlaylistStateHolder,
    availablePlaylists: List<com.offlineplaya.shared.domain.model.Playlist>,
    syncCoordinator: com.offlineplaya.shared.presentation.sync.LibrarySyncCoordinator,
    embedArtCoordinator: com.offlineplaya.shared.presentation.artwork.EmbedArtCoordinator,
    embedReport: com.offlineplaya.shared.domain.usecase.EmbedReport,
    musicPlayer: MusicPlayer,
    playback: PlaybackState,
    themePreferences: ThemePreferences,
    artworkPreferences: com.offlineplaya.shared.domain.model.ArtworkPreferences,
    hasWritePermission: Boolean,
    syncStatus: SyncStatus,
    trackCount: Long,
    onPickFolder: () -> Unit,
    onColorModeChange: (ColorMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onDownloadRemoteArtChange: (Boolean) -> Unit,
    onEmbedDownloadedArtChange: (Boolean) -> Unit,
    onRequestWritePermission: () -> Unit,
    dynamicColorSupported: Boolean,
    onTabSelected: (LibraryTab) -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onAddToPlaylist: (Track, Long) -> Unit,
) {
    AnimatedContent(
        targetState = current,
        transitionSpec = {
            fadeIn(animationSpec = tween(durationMillis = 180)) togetherWith
                fadeOut(animationSpec = tween(durationMillis = 180))
        },
        label = "destination",
    ) { dest ->
        when (dest) {
            AppDestination.Home -> HomePage(
                status = syncStatus,
                trackCount = trackCount,
                onPickFolder = onPickFolder,
                onOpenLibrary = { navigator.push(AppDestination.LibraryArtists) },
                onOpenPlaylists = { navigator.push(AppDestination.Playlists) },
                onOpenSearch = { navigator.push(AppDestination.Search) },
                onOpenSettings = { navigator.push(AppDestination.Settings) },
            )

            AppDestination.Settings -> {
                val managedRoots by syncCoordinator.managedRootsFlow
                    .collectAsState(initial = emptyList<com.offlineplaya.shared.domain.model.ManagedTreeRoot>())
                SettingsPage(
                    preferences = themePreferences,
                    artworkPreferences = artworkPreferences,
                    managedRoots = managedRoots,
                    isScanning = syncStatus is SyncStatus.Scanning,
                    hasWritePermission = hasWritePermission,
                    embedReport = embedReport,
                    onColorModeChange = onColorModeChange,
                    onDynamicColorChange = onDynamicColorChange,
                    onDownloadRemoteArtChange = onDownloadRemoteArtChange,
                    onEmbedDownloadedArtChange = onEmbedDownloadedArtChange,
                    onRequestWritePermission = onRequestWritePermission,
                    onEmbedMissingArt = { embedArtCoordinator.start() },
                    onAcknowledgeEmbedReport = { embedArtCoordinator.acknowledge() },
                    onRescanAll = { syncCoordinator.resyncAll() },
                    onRemoveManagedRoot = { uri -> syncCoordinator.removeManagedRoot(uri) },
                    onBack = { navigator.pop() },
                    dynamicColorSupported = dynamicColorSupported,
                )
            }

            AppDestination.NowPlaying -> NowPlayingPage(
                state = playback,
                onPlayPause = { if (playback.isPlaying) musicPlayer.pause() else musicPlayer.play() },
                onPrevious = { musicPlayer.skipToPrevious() },
                onNext = { musicPlayer.skipToNext() },
                onSeek = { musicPlayer.seekTo(it) },
                onShuffleToggle = { musicPlayer.setShuffleEnabled(!playback.shuffleEnabled) },
                onRepeatChange = { musicPlayer.setRepeatMode(it) },
                onOpenQueue = { navigator.push(AppDestination.Queue) },
                onBack = { navigator.pop() },
            )

            AppDestination.Search -> {
                val query by library.searchQuery.collectAsState()
                val results by library.searchResults.collectAsState()
                SearchPage(
                    query = query,
                    results = results,
                    availablePlaylists = availablePlaylists,
                    onQueryChange = { library.setSearchQuery(it) },
                    onPlayTracks = onPlayTracks,
                    onPlayNext = onPlayNext,
                    onAddToQueue = onAddToQueue,
                    onAddToPlaylist = onAddToPlaylist,
                    onBack = { navigator.pop() },
                )
            }

            AppDestination.Queue -> QueuePage(
                state = playback,
                onJumpTo = { idx -> musicPlayer.seekToIndex(idx) },
                onRemove = { idx -> musicPlayer.removeFromQueue(idx) },
                onClearQueue = { musicPlayer.clearQueue() },
                onBack = { navigator.pop() },
            )

            AppDestination.Playlists -> {
                val list by playlists.allPlaylists.collectAsState()
                PlaylistsPage(
                    playlists = list,
                    onPlaylistClick = { id ->
                        navigator.push(AppDestination.PlaylistDetail(id))
                    },
                    onCreate = { name -> playlists.create(name) },
                    onBack = { navigator.pop() },
                )
            }

            is AppDestination.PlaylistDetail -> {
                var playlist by remember(dest.playlistId) {
                    mutableStateOf<com.offlineplaya.shared.domain.model.Playlist?>(null)
                }
                LaunchedEffect(dest.playlistId) {
                    playlist = playlists.findPlaylist(dest.playlistId)
                }
                val tracks by remember(dest.playlistId) {
                    playlists.tracksIn(dest.playlistId)
                }.collectAsState(initial = emptyList<Track>())

                PlaylistDetailPage(
                    playlistName = playlist?.name ?: "Loading…",
                    tracks = tracks,
                    availablePlaylists = availablePlaylists,
                    onPlayTracks = onPlayTracks,
                    onPlayNext = onPlayNext,
                    onAddToQueue = onAddToQueue,
                    onAddToPlaylist = onAddToPlaylist,
                    onRename = { newName ->
                        playlists.rename(dest.playlistId, newName)
                        // Re-pull so the title bar refreshes without a round-trip pop.
                        playlist = playlist?.copy(name = newName)
                    },
                    onDelete = {
                        playlists.delete(dest.playlistId)
                        navigator.pop()
                    },
                    onBack = { navigator.pop() },
                )
            }

            AppDestination.LibraryArtists -> {
                val artists by library.allArtists.collectAsState()
                LibraryArtistsPage(
                    artists = artists,
                    onArtistClick = { id ->
                        navigator.push(AppDestination.LibraryArtistDetail(id))
                    },
                    onTabSelected = onTabSelected,
                    onBack = { navigator.pop() },
                )
            }

            is AppDestination.LibraryArtistDetail -> {
                var artist by remember(dest.artistId) { mutableStateOf<Artist?>(null) }
                LaunchedEffect(dest.artistId) {
                    artist = library.findArtist(dest.artistId)
                }
                val albums by remember(dest.artistId) {
                    library.albumsByArtist(dest.artistId)
                }.collectAsState(initial = emptyList<Album>())

                LibraryArtistDetailPage(
                    artistName = artist?.name ?: "Loading…",
                    albums = albums,
                    onAlbumClick = { id ->
                        navigator.push(AppDestination.LibraryAlbumDetail(id))
                    },
                    onBack = { navigator.pop() },
                )
            }

            is AppDestination.LibraryAlbumDetail -> {
                var album by remember(dest.albumId) { mutableStateOf<Album?>(null) }
                LaunchedEffect(dest.albumId) {
                    album = library.findAlbum(dest.albumId)
                }
                val tracks by remember(dest.albumId) {
                    library.tracksByAlbum(dest.albumId)
                }.collectAsState(initial = emptyList<Track>())

                LibraryAlbumDetailPage(
                    albumTitle = album?.name ?: "Loading…",
                    tracks = tracks,
                    availablePlaylists = availablePlaylists,
                    onPlayTracks = onPlayTracks,
                    onPlayNext = onPlayNext,
                    onAddToQueue = onAddToQueue,
                    onAddToPlaylist = onAddToPlaylist,
                    onBack = { navigator.pop() },
                )
            }

            AppDestination.LibraryFolderRoots -> {
                val roots by library.rootFolders.collectAsState()
                LibraryFolderRootsPage(
                    roots = roots,
                    onFolderClick = { id ->
                        navigator.push(AppDestination.LibraryFolderDetail(id))
                    },
                    onTabSelected = onTabSelected,
                    onBack = { navigator.pop() },
                )
            }

            is AppDestination.LibraryFolderDetail -> {
                var folder by remember(dest.folderId) { mutableStateOf<Folder?>(null) }
                LaunchedEffect(dest.folderId) {
                    folder = library.findFolder(dest.folderId)
                }
                val subfolders by remember(dest.folderId) {
                    library.childFolders(dest.folderId)
                }.collectAsState(initial = emptyList<Folder>())
                val tracks by remember(dest.folderId) {
                    library.tracksInFolder(dest.folderId)
                }.collectAsState(initial = emptyList<Track>())

                LibraryFolderDetailPage(
                    folderName = folder?.displayName ?: "Loading…",
                    subfolders = subfolders,
                    tracks = tracks,
                    availablePlaylists = availablePlaylists,
                    onFolderClick = { id ->
                        navigator.push(AppDestination.LibraryFolderDetail(id))
                    },
                    onPlayTracks = onPlayTracks,
                    onPlayNext = onPlayNext,
                    onAddToQueue = onAddToQueue,
                    onAddToPlaylist = onAddToPlaylist,
                    onBack = { navigator.pop() },
                )
            }

            AppDestination.LibraryFlat -> {
                val tracks by library.allTracks.collectAsState()
                LibraryFlatPage(
                    tracks = tracks,
                    availablePlaylists = availablePlaylists,
                    onPlayTracks = onPlayTracks,
                    onPlayNext = onPlayNext,
                    onAddToQueue = onAddToQueue,
                    onAddToPlaylist = onAddToPlaylist,
                    onTabSelected = onTabSelected,
                    onBack = { navigator.pop() },
                )
            }
        }
    }
}

private fun LibraryTab.toDestination(): AppDestination = when (this) {
    LibraryTab.ARTISTS -> AppDestination.LibraryArtists
    LibraryTab.FOLDERS -> AppDestination.LibraryFolderRoots
    LibraryTab.FLAT -> AppDestination.LibraryFlat
}

package com.offlineplaya.shared.presentation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.Album
import com.offlineplaya.shared.domain.model.Artist
import com.offlineplaya.shared.domain.model.ColorMode
import com.offlineplaya.shared.domain.model.Folder
import com.offlineplaya.shared.domain.model.PlaybackState
import com.offlineplaya.shared.domain.model.ThemePreferences
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.player.MusicPlayer
import com.offlineplaya.shared.domain.usecase.EmbedReport
import com.offlineplaya.shared.presentation.eq.EqualizerStateHolder
import com.offlineplaya.shared.presentation.library.LibraryStateHolder
import com.offlineplaya.shared.presentation.metadata.BurnMetadataCoordinator
import com.offlineplaya.shared.presentation.navigation.AppDestination
import com.offlineplaya.shared.presentation.navigation.AppNavigator
import com.offlineplaya.shared.presentation.playlist.PlaylistStateHolder
import com.offlineplaya.shared.presentation.sync.SyncStatus
import com.offlineplaya.shared.presentation.ui.atoms.LocalOpenSettings
import com.offlineplaya.shared.presentation.ui.molecules.LibraryTab
import com.offlineplaya.shared.presentation.ui.organisms.MiniPlayer
import com.offlineplaya.shared.presentation.ui.organisms.MiniPlayerReservedSpace
import com.offlineplaya.shared.presentation.ui.organisms.userLabel
import com.offlineplaya.shared.presentation.ui.pages.DesignSystemGalleryPage
import com.offlineplaya.shared.presentation.ui.pages.EqualizerPage
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
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun App(
    navigator: AppNavigator,
    library: LibraryStateHolder,
    playlists: PlaylistStateHolder,
    syncCoordinator: com.offlineplaya.shared.presentation.sync.LibrarySyncCoordinator,
    burnMetadataCoordinator: BurnMetadataCoordinator,
    musicPlayer: MusicPlayer,
    equalizerStateHolder: EqualizerStateHolder,
    themePreferences: ThemePreferences,
    artworkPreferences: com.offlineplaya.shared.domain.model.ArtworkPreferences,
    syncStatus: SyncStatus,
    trackCount: Long,
    onPickFolder: () -> Unit,
    onColorModeChange: (ColorMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onDownloadRemoteArtChange: (Boolean) -> Unit,
    onAddDirectFolder: (String) -> Unit,
    onManageExternalStorageClick: () -> Unit,
    dynamicColorSupported: Boolean,
) {
    OfflinePlayaTheme(preferences = themePreferences) {
        val stack by navigator.stack.collectAsState()
        val current = stack.last()
        val playback by musicPlayer.playbackState.collectAsState()
        val availablePlaylists by playlists.allPlaylists.collectAsState()
        val burnReport by burnMetadataCoordinator.report.collectAsState()

        val snackbarHostState = remember { SnackbarHostState() }

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

        val onNowPlaying = current == AppDestination.NowPlaying
        val showMini = playback.currentTrack != null && !onNowPlaying

        ProvideOrientation {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    when {
                        showMini -> MiniPlayer(
                            state = playback,
                            onExpand = { navigator.push(AppDestination.NowPlaying) },
                            onPlayPause = {
                                if (playback.isPlaying) musicPlayer.pause() else musicPlayer.play()
                            },
                            onPrevious = { musicPlayer.skipToPrevious() },
                            onNext = { musicPlayer.skipToNext() },
                        )
                        !onNowPlaying -> MiniPlayerReservedSpace()
                    }
                },
            ) { outerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(outerPadding)
                        .consumeWindowInsets(outerPadding),
                ) {
                    AppWatermark()
                    val openSettingsGlobally: (() -> Unit)? =
                        if (current == AppDestination.Settings) null else {
                            { navigator.push(AppDestination.Settings) }
                        }
                    
                    CompositionLocalProvider(LocalOpenSettings provides openSettingsGlobally) {
                        DestinationContent(
                            current = current,
                            navigator = navigator,
                            library = library,
                            playlists = playlists,
                            availablePlaylists = availablePlaylists,
                            syncCoordinator = syncCoordinator,
                            musicPlayer = musicPlayer,
                            equalizerStateHolder = equalizerStateHolder,
                            playback = playback,
                            themePreferences = themePreferences,
                            artworkPreferences = artworkPreferences,
                            burnReport = burnReport,
                            syncStatus = syncStatus,
                            trackCount = trackCount,
                            onPickFolder = onPickFolder,
                            onColorModeChange = onColorModeChange,
                            onDynamicColorChange = onDynamicColorChange,
                            onDownloadRemoteArtChange = onDownloadRemoteArtChange,
                            onBurnMetadataClick = { burnMetadataCoordinator.start() },
                            onAcknowledgeBurnReport = { burnMetadataCoordinator.acknowledge() },
                            onAddDirectFolder = onAddDirectFolder,
                            onManageExternalStorageClick = onManageExternalStorageClick,
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
    musicPlayer: MusicPlayer,
    equalizerStateHolder: EqualizerStateHolder,
    playback: PlaybackState,
    themePreferences: ThemePreferences,
    artworkPreferences: com.offlineplaya.shared.domain.model.ArtworkPreferences,
    burnReport: EmbedReport,
    syncStatus: SyncStatus,
    trackCount: Long,
    onPickFolder: () -> Unit,
    onColorModeChange: (ColorMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onDownloadRemoteArtChange: (Boolean) -> Unit,
    onBurnMetadataClick: () -> Unit,
    onAcknowledgeBurnReport: () -> Unit,
    onManageExternalStorageClick: () -> Unit,
    onAddDirectFolder: (String) -> Unit,
    dynamicColorSupported: Boolean,
    onTabSelected: (LibraryTab) -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onAddToPlaylist: (Track, Long) -> Unit,
) {
    val scope = rememberCoroutineScope()
    AnimatedContent(
        targetState = current,
        transitionSpec = {
            fadeIn(animationSpec = tween(durationMillis = 180)) togetherWith
                    fadeOut(animationSpec = tween(durationMillis = 180))
        },
        label = "destination",
    ) { dest ->
        when (dest) {
            AppDestination.Home -> {
                val rootFolders by library.rootFolders.collectAsState()
                val albums by library.allAlbums.collectAsState()
                val artists by library.allArtists.collectAsState()
                val playlistList by playlists.allPlaylists.collectAsState()
                HomePage(
                    status = syncStatus,
                    trackCount = trackCount,
                    folderCount = rootFolders.size,
                    albumCount = albums.size,
                    artistCount = artists.size,
                    playlistCount = playlistList.size,
                    recentAlbums = albums.take(10),
                    representativeTrackOfAlbum = { id -> library.representativeTrackOfAlbum(id) },
                    onOpenLibrary = { navigator.push(AppDestination.LibraryArtists) },
                    onOpenAllTracks = { navigator.push(AppDestination.LibraryFlat) },
                    onOpenAlbums = { navigator.push(AppDestination.LibraryArtists) },
                    onOpenArtists = { navigator.push(AppDestination.LibraryArtists) },
                    onOpenPlaylists = { navigator.push(AppDestination.Playlists) },
                    onOpenAlbum = { id -> navigator.push(AppDestination.LibraryAlbumDetail(id)) },
                    onOpenSearch = { navigator.push(AppDestination.Search) },
                    onOpenSettings = { navigator.push(AppDestination.Settings) },
                )
            }

            AppDestination.Settings -> {
                val managedRoots by syncCoordinator.managedRootsFlow
                    .collectAsState(initial = emptyList<com.offlineplaya.shared.domain.model.ManagedTreeRoot>())
                SettingsPage(
                    preferences = themePreferences,
                    artworkPreferences = artworkPreferences,
                    managedRoots = managedRoots.toPersistentList(),
                    isScanning = syncStatus is SyncStatus.Scanning,
                    burnReport = burnReport,
                    onColorModeChange = onColorModeChange,
                    onDynamicColorChange = onDynamicColorChange,
                    onDownloadRemoteArtChange = onDownloadRemoteArtChange,
                    onBurnMetadataClick = onBurnMetadataClick,
                    onAcknowledgeBurnReport = onAcknowledgeBurnReport,
                    onManageExternalStorageClick = onManageExternalStorageClick,
                    onAddDirectFolder = onAddDirectFolder,
                    onRescanAll = { syncCoordinator.resyncAll() },
                    onRemoveManagedRoot = { uri -> syncCoordinator.removeManagedRoot(uri) },
                    onOpenEqualizer = { navigator.push(AppDestination.Equalizer) },
                    onOpenDesignSystem = { navigator.push(AppDestination.DesignSystemGallery) },
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
                onOpenEqualizer = { navigator.push(AppDestination.Equalizer) },
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
                    onPlayArtist = { artist ->
                        scope.launch {
                            val tracks = library.tracksByArtist(artist.id).first()
                            onPlayTracks(tracks, 0)
                        }
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
                    artist = artist,
                    albums = albums,
                    onAlbumClick = { id ->
                        navigator.push(AppDestination.LibraryAlbumDetail(id))
                    },
                    onPlayAlbum = { album ->
                        scope.launch {
                            val tracks = library.tracksByAlbum(album.id).first()
                            onPlayTracks(tracks, 0)
                        }
                    },
                    onPlayArtist = {
                        scope.launch {
                            val tracks = library.tracksByArtist(dest.artistId).first()
                            onPlayTracks(tracks.shuffled(), 0)
                        }
                    },
                    onBack = { navigator.pop() },
                    representativeTrackProvider = { id -> library.representativeTrackOfAlbum(id) },
                )
            }

            is AppDestination.LibraryAlbumDetail -> {
                var album by remember(dest.albumId) { mutableStateOf<Album?>(null) }
                var artistName by remember(dest.albumId) { mutableStateOf<String?>(null) }
                var representativeTrack by remember(dest.albumId) { mutableStateOf<Track?>(null) }
                LaunchedEffect(dest.albumId) {
                    val a = library.findAlbum(dest.albumId)
                    album = a
                    artistName = library.artistNameOrNull(a?.artistId)
                    representativeTrack = library.representativeTrackOfAlbum(dest.albumId)
                }
                val tracks by remember(dest.albumId) {
                    library.tracksByAlbum(dest.albumId)
                }.collectAsState(initial = emptyList<Track>())

                LibraryAlbumDetailPage(
                    album = album,
                    artistName = artistName,
                    representativeTrack = representativeTrack,
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
                    previewTracksProvider = { id -> library.previewTracksInFolder(id) },
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
                    previewTracksProvider = { id -> library.previewTracksInFolder(id) },
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

            AppDestination.DesignSystemGallery -> DesignSystemGalleryPage(
                onBack = { navigator.pop() },
            )

            AppDestination.Equalizer -> {
                val prefs by equalizerStateHolder.preferences.collectAsState()
                val activePreset by equalizerStateHolder.activePreset.collectAsState()
                EqualizerPage(
                    preferences = prefs,
                    activePreset = activePreset,
                    nowPlayingTitle = playback.currentTrack?.title,
                    rawGenreTag = playback.currentTrack?.genre,
                    autoGenreLabel = playback.currentTrack?.canonicalGenre?.userLabel(),
                    onModeChange = { equalizerStateHolder.setMode(it) },
                    onPresetChange = { equalizerStateHolder.setManualPreset(it) },
                    onBandGainChange = { idx, mb, gains ->
                        equalizerStateHolder.setBandGain(idx, mb, gains)
                    },
                    onResetOverrides = { equalizerStateHolder.resetManualOverrides() },
                    onBack = { navigator.pop() },
                )
            }
        }
    }
}

@Composable
private fun AppWatermark() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
            modifier = Modifier.size(280.dp),
        )
    }
}

private fun LibraryTab.toDestination(): AppDestination = when (this) {
    LibraryTab.ARTISTS -> AppDestination.LibraryArtists
    LibraryTab.FOLDERS -> AppDestination.LibraryFolderRoots
    LibraryTab.FLAT -> AppDestination.LibraryFlat
}

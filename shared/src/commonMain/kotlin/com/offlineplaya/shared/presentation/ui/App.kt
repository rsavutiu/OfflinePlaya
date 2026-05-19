package com.offlineplaya.shared.presentation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
    musicPlayer: MusicPlayer,
    themePreferences: ThemePreferences,
    syncStatus: SyncStatus,
    trackCount: Long,
    onPickFolder: () -> Unit,
    onColorModeChange: (ColorMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    dynamicColorSupported: Boolean,
) {
    OfflinePlayaTheme(preferences = themePreferences) {
        val stack by navigator.stack.collectAsState()
        val current = stack.last()
        val playback by musicPlayer.playbackState.collectAsState()

        val onTabSelected: (LibraryTab) -> Unit = { tab ->
            navigator.swapTop(tab.toDestination())
        }
        val onPlayTracks: (List<Track>, Int) -> Unit = { tracks, index ->
            musicPlayer.setQueue(tracks, index)
            navigator.push(AppDestination.NowPlaying)
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                DestinationContent(
                    current = current,
                    navigator = navigator,
                    library = library,
                    musicPlayer = musicPlayer,
                    playback = playback,
                    themePreferences = themePreferences,
                    syncStatus = syncStatus,
                    trackCount = trackCount,
                    onPickFolder = onPickFolder,
                    onColorModeChange = onColorModeChange,
                    onDynamicColorChange = onDynamicColorChange,
                    dynamicColorSupported = dynamicColorSupported,
                    onTabSelected = onTabSelected,
                    onPlayTracks = onPlayTracks,
                )
            }

            // MiniPlayer sits below the content, visible on every page that
            // isn't NowPlaying (which already has full transport controls).
            AnimatedVisibility(
                visible = playback.currentTrack != null && current != AppDestination.NowPlaying,
            ) {
                MiniPlayer(
                    state = playback,
                    onExpand = { navigator.push(AppDestination.NowPlaying) },
                    onPlayPause = { if (playback.isPlaying) musicPlayer.pause() else musicPlayer.play() },
                    onPrevious = { musicPlayer.skipToPrevious() },
                    onNext = { musicPlayer.skipToNext() },
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
    musicPlayer: MusicPlayer,
    playback: PlaybackState,
    themePreferences: ThemePreferences,
    syncStatus: SyncStatus,
    trackCount: Long,
    onPickFolder: () -> Unit,
    onColorModeChange: (ColorMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    dynamicColorSupported: Boolean,
    onTabSelected: (LibraryTab) -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit,
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
                onOpenSettings = { navigator.push(AppDestination.Settings) },
            )

            AppDestination.Settings -> SettingsPage(
                preferences = themePreferences,
                onColorModeChange = onColorModeChange,
                onDynamicColorChange = onDynamicColorChange,
                onBack = { navigator.pop() },
                dynamicColorSupported = dynamicColorSupported,
            )

            AppDestination.NowPlaying -> NowPlayingPage(
                state = playback,
                onPlayPause = { if (playback.isPlaying) musicPlayer.pause() else musicPlayer.play() },
                onPrevious = { musicPlayer.skipToPrevious() },
                onNext = { musicPlayer.skipToNext() },
                onSeek = { musicPlayer.seekTo(it) },
                onBack = { navigator.pop() },
            )

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
                    onPlayTracks = onPlayTracks,
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
                    onFolderClick = { id ->
                        navigator.push(AppDestination.LibraryFolderDetail(id))
                    },
                    onPlayTracks = onPlayTracks,
                    onBack = { navigator.pop() },
                )
            }

            AppDestination.LibraryFlat -> {
                val tracks by library.allTracks.collectAsState()
                LibraryFlatPage(
                    tracks = tracks,
                    onPlayTracks = onPlayTracks,
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

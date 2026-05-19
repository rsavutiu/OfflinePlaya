package com.offlineplaya.shared.presentation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.offlineplaya.shared.domain.model.Album
import com.offlineplaya.shared.domain.model.Artist
import com.offlineplaya.shared.domain.model.ColorMode
import com.offlineplaya.shared.domain.model.Folder
import com.offlineplaya.shared.domain.model.ThemePreferences
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.library.LibraryStateHolder
import com.offlineplaya.shared.presentation.navigation.AppDestination
import com.offlineplaya.shared.presentation.navigation.AppNavigator
import com.offlineplaya.shared.presentation.sync.SyncStatus
import com.offlineplaya.shared.presentation.ui.molecules.LibraryTab
import com.offlineplaya.shared.presentation.ui.pages.HomePage
import com.offlineplaya.shared.presentation.ui.pages.LibraryAlbumDetailPage
import com.offlineplaya.shared.presentation.ui.pages.LibraryArtistDetailPage
import com.offlineplaya.shared.presentation.ui.pages.LibraryArtistsPage
import com.offlineplaya.shared.presentation.ui.pages.LibraryFlatPage
import com.offlineplaya.shared.presentation.ui.pages.LibraryFolderDetailPage
import com.offlineplaya.shared.presentation.ui.pages.LibraryFolderRootsPage
import com.offlineplaya.shared.presentation.ui.pages.SettingsPage
import com.offlineplaya.shared.presentation.ui.theme.OfflinePlayaTheme

/**
 * Shared app entry. Renders whichever destination is on top of the navigator's
 * stack, applies the user's chosen theme, and threads library data into the
 * Library destinations. Platform hosts (Android `MainActivity`) inject Koin
 * dependencies and platform callbacks (SAF picker on Android).
 */
@Composable
fun App(
    navigator: AppNavigator,
    library: LibraryStateHolder,
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

        val onTabSelected: (LibraryTab) -> Unit = { tab ->
            navigator.swapTop(tab.toDestination())
        }

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
                        onBack = { navigator.pop() },
                    )
                }

                AppDestination.LibraryFlat -> {
                    val tracks by library.allTracks.collectAsState()
                    LibraryFlatPage(
                        tracks = tracks,
                        onTabSelected = onTabSelected,
                        onBack = { navigator.pop() },
                    )
                }
            }
        }
    }
}

private fun LibraryTab.toDestination(): AppDestination = when (this) {
    LibraryTab.ARTISTS -> AppDestination.LibraryArtists
    LibraryTab.FOLDERS -> AppDestination.LibraryFolderRoots
    LibraryTab.FLAT -> AppDestination.LibraryFlat
}

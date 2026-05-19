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
import com.offlineplaya.shared.domain.model.ThemePreferences
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.library.LibraryStateHolder
import com.offlineplaya.shared.presentation.navigation.AppDestination
import com.offlineplaya.shared.presentation.navigation.AppNavigator
import com.offlineplaya.shared.presentation.sync.SyncStatus
import com.offlineplaya.shared.presentation.ui.pages.HomePage
import com.offlineplaya.shared.presentation.ui.pages.LibraryAlbumDetailPage
import com.offlineplaya.shared.presentation.ui.pages.LibraryArtistDetailPage
import com.offlineplaya.shared.presentation.ui.pages.LibraryArtistsPage
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

                // Folder + flat destinations land in a follow-up; until then we
                // route back to Home rather than show a blank screen.
                AppDestination.LibraryFolderRoots,
                is AppDestination.LibraryFolderDetail,
                AppDestination.LibraryFlat -> HomePage(
                    status = syncStatus,
                    trackCount = trackCount,
                    onPickFolder = onPickFolder,
                    onOpenLibrary = { navigator.push(AppDestination.LibraryArtists) },
                    onOpenSettings = { navigator.push(AppDestination.Settings) },
                )
            }
        }
    }
}

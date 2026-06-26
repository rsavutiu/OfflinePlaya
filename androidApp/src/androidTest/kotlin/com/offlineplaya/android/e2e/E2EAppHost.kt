package com.offlineplaya.android.e2e

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.offlineplaya.shared.presentation.eq.EqualizerStateHolder
import com.offlineplaya.shared.presentation.history.SmartPlaylistsStateHolder
import com.offlineplaya.shared.presentation.library.LibraryStateHolder
import com.offlineplaya.shared.presentation.lyrics.LyricsStateHolder
import com.offlineplaya.shared.presentation.metadata.BurnMetadataCoordinator
import com.offlineplaya.shared.presentation.navigation.AppNavigator
import com.offlineplaya.shared.presentation.playlist.PlaylistStateHolder
import com.offlineplaya.shared.presentation.settings.ArtworkStateHolder
import com.offlineplaya.shared.presentation.settings.LyricsPreferencesStateHolder
import com.offlineplaya.shared.presentation.settings.PlaybackTuningStateHolder
import com.offlineplaya.shared.presentation.settings.ThemeStateHolder
import com.offlineplaya.shared.presentation.sync.LibrarySyncCoordinator
import com.offlineplaya.shared.presentation.tag.TagEditorCoordinator
import com.offlineplaya.shared.presentation.theme.AlbumColorStateHolder
import com.offlineplaya.shared.presentation.ui.App
import org.koin.core.Koin

/**
 * Minimal in-test host for [App] — resolves the same graph `MainActivity` does
 * from [koin] and collects the preference flows, without the Android-only
 * plumbing (SAF picker, runtime-permission prompt, system-bar styling). The
 * change-callbacks are no-ops; the E2E happy paths don't exercise settings.
 *
 * Shared by the Phase-2 E2E tests so each one is just the user journey.
 */
@Composable
fun E2EAppHost(koin: Koin) {
    val navigator: AppNavigator = koin.get()
    val library: LibraryStateHolder = koin.get()
    val playlists: PlaylistStateHolder = koin.get()
    val syncCoordinator: LibrarySyncCoordinator = koin.get()
    val burnMetadataCoordinator: BurnMetadataCoordinator = koin.get()
    val equalizerStateHolder: EqualizerStateHolder = koin.get()
    val lyricsStateHolder: LyricsStateHolder = koin.get()
    val tagEditorCoordinator: TagEditorCoordinator = koin.get()
    val smartPlaylists: SmartPlaylistsStateHolder = koin.get()
    val themeStateHolder: ThemeStateHolder = koin.get()
    val artworkStateHolder: ArtworkStateHolder = koin.get()
    val lyricsPreferencesStateHolder: LyricsPreferencesStateHolder = koin.get()
    val playbackTuningStateHolder: PlaybackTuningStateHolder = koin.get()
    val albumColorStateHolder: AlbumColorStateHolder = koin.get()

    val themePreferences by themeStateHolder.preferences.collectAsState()
    val artworkPreferences by artworkStateHolder.preferences.collectAsState()
    val lyricsPreferences by lyricsPreferencesStateHolder.preferences.collectAsState()
    val playbackPreferences by playbackTuningStateHolder.preferences.collectAsState()
    val syncStatus by syncCoordinator.status.collectAsState()
    val trackCount by library.totalTrackCount.collectAsState()
    val seedColor by albumColorStateHolder.seedColor.collectAsState()

    App(
        navigator = navigator,
        library = library,
        playlists = playlists,
        syncCoordinator = syncCoordinator,
        burnMetadataCoordinator = burnMetadataCoordinator,
        musicPlayer = koin.get(),
        equalizerStateHolder = equalizerStateHolder,
        lyricsStateHolder = lyricsStateHolder,
        tagEditorCoordinator = tagEditorCoordinator,
        smartPlaylists = smartPlaylists,
        themePreferences = themePreferences,
        artworkPreferences = artworkPreferences,
        lyricsPreferences = lyricsPreferences,
        playbackPreferences = playbackPreferences,
        syncStatus = syncStatus,
        trackCount = trackCount,
        seedColor = seedColor,
        onPickFolder = {},
        onColorModeChange = {},
        onDynamicColorChange = {},
        onAlbumArtColorChange = {},
        onDownloadRemoteArtChange = {},
        onDownloadRemoteLyricsChange = {},
        onSaveLyricsAsSidecarChange = {},
        onCrossfadeEnabledChange = {},
        onCrossfadeDurationChange = {},
        dynamicColorSupported = true,
    )
}

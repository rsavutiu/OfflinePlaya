package com.offlineplaya.android.e2e

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
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
import com.offlineplaya.shared.presentation.ui.TestTags
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

/**
 * Construct every singleton [E2EAppHost] resolves, **before** `setContent`, so
 * the composition's first frame isn't competing with graph construction (DB
 * open, repositories, OkHttp clients, flow subscriptions) on the main thread —
 * that contention starves the `runComposeUiTest` host and surfaces as flaky
 * "No compose hierarchies found". Koin caches the singletons, so the in-compose
 * `koin.get()`s then return instantly.
 */
fun warmUpE2EGraph(koin: Koin) {
    koin.get<AppNavigator>()
    koin.get<LibraryStateHolder>()
    koin.get<PlaylistStateHolder>()
    koin.get<LibrarySyncCoordinator>()
    koin.get<BurnMetadataCoordinator>()
    koin.get<EqualizerStateHolder>()
    koin.get<LyricsStateHolder>()
    koin.get<TagEditorCoordinator>()
    koin.get<SmartPlaylistsStateHolder>()
    koin.get<ThemeStateHolder>()
    koin.get<ArtworkStateHolder>()
    koin.get<LyricsPreferencesStateHolder>()
    koin.get<PlaybackTuningStateHolder>()
    koin.get<AlbumColorStateHolder>()
}

/**
 * Standard E2E entry: warm the graph, render the host, wait for Home to come up
 * **before** kicking the seed scan — so the host registers its composition on a
 * quiet main thread, then the library populates via a real `resyncAll()`.
 */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.launchSeededE2EApp(handle: E2EKoinHandle) {
    warmUpE2EGraph(handle.koin)
    setContent { E2EAppHost(handle.koin) }
    waitUntil(timeoutMillis = 15_000) {
        onAllNodesWithTag(TestTags.Home.ROOT).fetchSemanticsNodes().isNotEmpty()
    }
    handle.koin.get<LibrarySyncCoordinator>().resyncAll()
}

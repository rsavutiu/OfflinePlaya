package com.offlineplaya.android

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.offlineplaya.android.picker.OpenDocumentTreeContract
import com.offlineplaya.shared.domain.player.MusicPlayer
import com.offlineplaya.shared.presentation.library.LibraryStateHolder
import com.offlineplaya.shared.presentation.metadata.BurnMetadataCoordinator
import com.offlineplaya.shared.presentation.navigation.AppNavigator
import com.offlineplaya.shared.presentation.playlist.PlaylistStateHolder
import com.offlineplaya.shared.presentation.settings.ArtworkStateHolder
import com.offlineplaya.shared.presentation.settings.PlaybackTuningStateHolder
import com.offlineplaya.shared.presentation.settings.ThemeStateHolder
import com.offlineplaya.shared.presentation.sync.LibrarySyncCoordinator
import com.offlineplaya.shared.presentation.ui.App
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            KoinContext {
                AndroidApp()
            }
        }
    }
}

/**
 * The runtime permission we need to enumerate music files via MediaStore.
 * Android 13 split storage perms by media type — audio gets its own
 * `READ_MEDIA_AUDIO`. On 12 and older we fall back to the legacy
 * `READ_EXTERNAL_STORAGE` (declared in the manifest with maxSdkVersion=32).
 *
 * We deliberately do NOT use `MANAGE_EXTERNAL_STORAGE` ("All files access").
 * Google Play's policy reserves that for file managers / backup / antivirus
 * apps, and music players are not on the allow-list. Library reads go via
 * MediaStore + this permission; library writes (the burn-metadata feature)
 * go via SAF tree URIs the user explicitly grants in the folder picker.
 */
private val AUDIO_READ_PERMISSION: String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

private fun hasAudioReadPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, AUDIO_READ_PERMISSION) ==
            PackageManager.PERMISSION_GRANTED

/**
 * Android host for the shared [App]. Wires the SAF folder picker, hardware
 * back into the [AppNavigator], and observes every state holder.
 */
@Composable
private fun AndroidApp() {
    val navigator: AppNavigator = koinInject()
    val themeStateHolder: ThemeStateHolder = koinInject()
    val artworkStateHolder: ArtworkStateHolder = koinInject()
    val playbackTuningStateHolder: PlaybackTuningStateHolder = koinInject()
    val coordinator: LibrarySyncCoordinator = koinInject()
    val burnMetadataCoordinator: BurnMetadataCoordinator = koinInject()
    val library: LibraryStateHolder = koinInject()
    val playlists: PlaylistStateHolder = koinInject()
    val musicPlayer: MusicPlayer = koinInject()
    val equalizerStateHolder: com.offlineplaya.shared.presentation.eq.EqualizerStateHolder = koinInject()
    val albumColorStateHolder: com.offlineplaya.shared.presentation.theme.AlbumColorStateHolder = koinInject()
    val themePreferences by themeStateHolder.preferences.collectAsState()
    val seedColor by albumColorStateHolder.seedColor.collectAsState()
    val artworkPreferences by artworkStateHolder.preferences.collectAsState()
    val playbackPreferences by playbackTuningStateHolder.preferences.collectAsState()
    val syncStatus by coordinator.status.collectAsState()
    val trackCount by library.totalTrackCount.collectAsState()
    val stack by navigator.stack.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Re-scan whenever the app comes back to the foreground AND we still
    // hold the audio-read permission. If the user toggled it off in system
    // settings, ON_RESUME notices that and the next sync turns into a no-op.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && hasAudioReadPermission(context)) {
                // resyncIfIdle, not resyncAll: returning from the SAF picker or
                // a quick app-switch fires ON_RESUME, and we don't want those to
                // stack a second full scan on top of one already running. The
                // scan itself is now cheap when nothing changed (already-scanned
                // device rows are short-circuited in syncDeviceAudio).
                coordinator.resyncIfIdle()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Granted now → kick off a scan so the library populates without
        // requiring a manual re-sync. Denied is fine — the app still works
        // with SAF-picked folders; the library just won't auto-discover
        // MediaStore-indexed audio (Downloads, root storage, etc.).
        if (granted) coordinator.resyncAll()
    }

    // Fire the system runtime-permission dialog at most once per process
    // start. No in-app rationale screen — the system dialog itself
    // ("Allow OfflinePlaya to access music and audio?") is self-explanatory
    // for a music player, and a custom pre-prompt screen would just be an
    // extra tap with no information value.
    var permissionPromptFired by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!permissionPromptFired && !hasAudioReadPermission(context)) {
            permissionPromptFired = true
            permissionLauncher.launch(AUDIO_READ_PERMISSION)
        }
    }

    BackHandler(enabled = stack.size > 1) {
        navigator.pop()
    }

    val readPickerLauncher = rememberLauncherForActivityResult(
        OpenDocumentTreeContract(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        // Take read+write so the burn-metadata feature can write tags back
        // into files inside the tree. Fall back to read-only if the provider
        // refuses write — at minimum we can still scan the folder.
        runCatching {
            val takeFlags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        }.onFailure {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        val displayName =
            DocumentFile.fromTreeUri(context, uri)?.name ?: uri.lastPathSegment ?: "Folder"
        coordinator.addAndSync(uri.toString(), displayName)
    }

    App(
        navigator = navigator,
        library = library,
        playlists = playlists,
        syncCoordinator = coordinator,
        burnMetadataCoordinator = burnMetadataCoordinator,
        musicPlayer = musicPlayer,
        equalizerStateHolder = equalizerStateHolder,
        themePreferences = themePreferences,
        artworkPreferences = artworkPreferences,
        playbackPreferences = playbackPreferences,
        syncStatus = syncStatus,
        trackCount = trackCount,
        seedColor = seedColor,
        onPickFolder = { readPickerLauncher.launch(Unit) },
        onColorModeChange = themeStateHolder::setColorMode,
        onDynamicColorChange = themeStateHolder::setUseDynamicColor,
        onAlbumArtColorChange = themeStateHolder::setUseAlbumArtColor,
        onDownloadRemoteArtChange = artworkStateHolder::setDownloadRemoteArt,
        onCrossfadeEnabledChange = playbackTuningStateHolder::setCrossfadeEnabled,
        onCrossfadeDurationChange = playbackTuningStateHolder::setCrossfadeDurationSeconds,
        dynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    )
}

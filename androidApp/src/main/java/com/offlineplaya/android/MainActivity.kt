package com.offlineplaya.android

import android.Manifest
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import org.koin.compose.KoinContext
import com.offlineplaya.android.picker.OpenDocumentTreeContract
import com.offlineplaya.shared.domain.player.MusicPlayer
import com.offlineplaya.shared.presentation.library.LibraryStateHolder
import com.offlineplaya.shared.presentation.navigation.AppNavigator
import com.offlineplaya.shared.presentation.artwork.EmbedArtCoordinator
import com.offlineplaya.shared.presentation.playlist.PlaylistStateHolder
import com.offlineplaya.shared.presentation.settings.ArtworkStateHolder
import com.offlineplaya.shared.presentation.settings.ThemeStateHolder
import com.offlineplaya.shared.presentation.sync.LibrarySyncCoordinator
import com.offlineplaya.shared.presentation.ui.App
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
 * Android host for the shared [App]. Wires SAF folder picking (read-only +
 * read-write variants), hardware back into the [AppNavigator], and observes
 * every state holder.
 */
@Composable
private fun AndroidApp() {
    val navigator: AppNavigator = koinInject()
    val themeStateHolder: ThemeStateHolder = koinInject()
    val artworkStateHolder: ArtworkStateHolder = koinInject()
    val coordinator: LibrarySyncCoordinator = koinInject()
    val embedArtCoordinator: EmbedArtCoordinator = koinInject()
    val library: LibraryStateHolder = koinInject()
    val playlists: PlaylistStateHolder = koinInject()
    val musicPlayer: MusicPlayer = koinInject()
    val equalizerStateHolder: com.offlineplaya.shared.presentation.eq.EqualizerStateHolder = koinInject()

    val themePreferences by themeStateHolder.preferences.collectAsState()
    val artworkPreferences by artworkStateHolder.preferences.collectAsState()
    val syncStatus by coordinator.status.collectAsState()
    val trackCount by library.totalTrackCount.collectAsState()
    val stack by navigator.stack.collectAsState()
    val context = LocalContext.current

    // Audio-library permission. Granting this lets MediaStoreDeviceAudioScanner
    // see music in folders SAF won't let users tree-pick (Download/, internal
    // storage root). We ask once on first launch; if the user declines the
    // app still works against any SAF folders they've added — they just won't
    // see Downloads-style content.
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) coordinator.resyncAll()
    }
    LaunchedEffect(Unit) {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED) {
            audioPermissionLauncher.launch(perm)
        }
    }

    // Plain folder picker (read-only, used for the initial Pick Folder action).
    val readPickerLauncher = rememberLauncherForActivityResult(OpenDocumentTreeContract()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        val displayName = DocumentFile.fromTreeUri(context, uri)?.name
            ?: uri.lastPathSegment
            ?: "Folder"
        coordinator.addAndSync(uri.toString(), displayName)
    }

    // Embed-cover picker. One-shot: the user picks a folder, we take write
    // access for it, and immediately kick off the burn-art pass scoped to
    // that tree URI. Completion bubbles back as a snackbar via
    // EmbedArtCoordinator.events — no persistent state.
    val embedPickerLauncher = rememberLauncherForActivityResult(
        OpenDocumentTreeContract(requestWrite = true),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        embedArtCoordinator.embedFolder(uri.toString())
    }

    BackHandler(enabled = stack.size > 1) {
        navigator.pop()
    }

    App(
        navigator = navigator,
        library = library,
        playlists = playlists,
        syncCoordinator = coordinator,
        embedArtCoordinator = embedArtCoordinator,
        musicPlayer = musicPlayer,
        equalizerStateHolder = equalizerStateHolder,
        themePreferences = themePreferences,
        artworkPreferences = artworkPreferences,
        syncStatus = syncStatus,
        trackCount = trackCount,
        onPickFolder = { readPickerLauncher.launch(Unit) },
        onColorModeChange = themeStateHolder::setColorMode,
        onDynamicColorChange = themeStateHolder::setUseDynamicColor,
        onDownloadRemoteArtChange = artworkStateHolder::setDownloadRemoteArt,
        onEmbedFolderClick = { embedPickerLauncher.launch(Unit) },
        dynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    )
}

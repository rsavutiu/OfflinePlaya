package com.offlineplaya.android

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import com.offlineplaya.android.picker.OpenDocumentTreeContract
import com.offlineplaya.shared.domain.player.MusicPlayer
import com.offlineplaya.shared.presentation.library.LibraryStateHolder
import com.offlineplaya.shared.presentation.navigation.AppNavigator
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
        setContent { AndroidApp() }
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
    val library: LibraryStateHolder = koinInject()
    val playlists: PlaylistStateHolder = koinInject()
    val musicPlayer: MusicPlayer = koinInject()

    val themePreferences by themeStateHolder.preferences.collectAsState()
    val artworkPreferences by artworkStateHolder.preferences.collectAsState()
    val syncStatus by coordinator.status.collectAsState()
    val trackCount by library.totalTrackCount.collectAsState()
    val stack by navigator.stack.collectAsState()
    val context = LocalContext.current

    // Snapshot of whether any persisted SAF permission grants us write access
    // — used to gate the "embed art into files" toggle in Settings. We
    // recompute it on first composition + every time the read-write picker
    // returns successfully.
    var hasWritePermission by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        hasWritePermission = context.contentResolver.persistedUriPermissions
            .any { it.isWritePermission }
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

    // Write-elevation picker. The user re-picks the same root to grant write
    // access; we persist both R+W permissions so the embed flow can use them.
    val writePickerLauncher = rememberLauncherForActivityResult(
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
        // Refresh the cached state so the Settings toggle un-greys.
        hasWritePermission = context.contentResolver.persistedUriPermissions
            .any { it.isWritePermission }
        // If this is a brand-new tree, treat it like a normal Pick Folder too.
        val displayName = DocumentFile.fromTreeUri(context, uri)?.name
            ?: uri.lastPathSegment
            ?: "Folder"
        coordinator.addAndSync(uri.toString(), displayName)
    }

    BackHandler(enabled = stack.size > 1) {
        navigator.pop()
    }

    App(
        navigator = navigator,
        library = library,
        playlists = playlists,
        syncCoordinator = coordinator,
        musicPlayer = musicPlayer,
        themePreferences = themePreferences,
        artworkPreferences = artworkPreferences,
        hasWritePermission = hasWritePermission,
        syncStatus = syncStatus,
        trackCount = trackCount,
        onPickFolder = { readPickerLauncher.launch(Unit) },
        onColorModeChange = themeStateHolder::setColorMode,
        onDynamicColorChange = themeStateHolder::setUseDynamicColor,
        onDownloadRemoteArtChange = artworkStateHolder::setDownloadRemoteArt,
        onEmbedDownloadedArtChange = artworkStateHolder::setEmbedDownloadedArt,
        onRequestWritePermission = { writePickerLauncher.launch(Unit) },
        dynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    )
}

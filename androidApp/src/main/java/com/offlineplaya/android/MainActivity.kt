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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import com.offlineplaya.android.picker.OpenDocumentTreeContract
import com.offlineplaya.shared.domain.player.MusicPlayer
import com.offlineplaya.shared.presentation.library.LibraryStateHolder
import com.offlineplaya.shared.presentation.navigation.AppNavigator
import com.offlineplaya.shared.presentation.playlist.PlaylistStateHolder
import com.offlineplaya.shared.presentation.settings.ThemeStateHolder
import com.offlineplaya.shared.presentation.sync.LibrarySyncCoordinator
import com.offlineplaya.shared.presentation.ui.App
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Edge-to-edge: draw under status + nav bars. ComponentActivity's
        // enableEdgeToEdge() also handles light/dark icon contrast automatically.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { AndroidApp() }
    }
}

/**
 * Android host for the shared [App]. Wires SAF folder picking, hardware back
 * button into the [AppNavigator], and observes every state holder.
 */
@Composable
private fun AndroidApp() {
    val navigator: AppNavigator = koinInject()
    val themeStateHolder: ThemeStateHolder = koinInject()
    val coordinator: LibrarySyncCoordinator = koinInject()
    val library: LibraryStateHolder = koinInject()
    val playlists: PlaylistStateHolder = koinInject()
    val musicPlayer: MusicPlayer = koinInject()

    val themePreferences by themeStateHolder.preferences.collectAsState()
    val syncStatus by coordinator.status.collectAsState()
    val trackCount by library.totalTrackCount.collectAsState()
    val stack by navigator.stack.collectAsState()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(OpenDocumentTreeContract()) { pickedUri ->
        if (pickedUri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                pickedUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        val displayName = DocumentFile.fromTreeUri(context, pickedUri)?.name
            ?: pickedUri.lastPathSegment
            ?: "Folder"
        coordinator.addAndSync(pickedUri.toString(), displayName)
    }

    // Hardware/gesture back routes into the in-memory navigator.
    BackHandler(enabled = stack.size > 1) {
        navigator.pop()
    }

    App(
        navigator = navigator,
        library = library,
        playlists = playlists,
        musicPlayer = musicPlayer,
        themePreferences = themePreferences,
        syncStatus = syncStatus,
        trackCount = trackCount,
        onPickFolder = { launcher.launch(Unit) },
        onColorModeChange = themeStateHolder::setColorMode,
        onDynamicColorChange = themeStateHolder::setUseDynamicColor,
        dynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    )
}

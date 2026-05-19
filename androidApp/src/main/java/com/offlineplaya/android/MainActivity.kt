package com.offlineplaya.android

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import com.offlineplaya.android.picker.OpenDocumentTreeContract
import com.offlineplaya.shared.presentation.navigation.AppNavigator
import com.offlineplaya.shared.presentation.settings.ThemeStateHolder
import com.offlineplaya.shared.presentation.sync.LibrarySyncCoordinator
import com.offlineplaya.shared.presentation.ui.App
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AndroidApp() }
    }
}

/**
 * Android host for the shared [App]. Wires SAF folder picking, hardware back
 * button into the [AppNavigator], and observes the theme/sync state holders.
 */
@Composable
private fun AndroidApp() {
    val navigator: AppNavigator = koinInject()
    val themeStateHolder: ThemeStateHolder = koinInject()
    val coordinator: LibrarySyncCoordinator = koinInject()

    val themePreferences by themeStateHolder.preferences.collectAsState()
    val syncStatus by coordinator.status.collectAsState()
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
        themePreferences = themePreferences,
        syncStatus = syncStatus,
        onPickFolder = { launcher.launch(Unit) },
        onColorModeChange = themeStateHolder::setColorMode,
        onDynamicColorChange = themeStateHolder::setUseDynamicColor,
        dynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    )
}

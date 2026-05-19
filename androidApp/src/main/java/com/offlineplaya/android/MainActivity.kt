package com.offlineplaya.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.documentfile.provider.DocumentFile
import com.offlineplaya.android.picker.OpenDocumentTreeContract
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
 * Android host for the shared [App] composable. Observes the
 * [LibrarySyncCoordinator] state, presents a SAF picker, takes persistable
 * URI permission, and hands the picked URI to the coordinator.
 */
@Composable
private fun AndroidApp() {
    val coordinator: LibrarySyncCoordinator = koinInject()
    val status by coordinator.status.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val launcher = rememberLauncherForActivityResult(OpenDocumentTreeContract()) { pickedUri ->
        if (pickedUri == null) return@rememberLauncherForActivityResult

        // Persist the grant across process death so we can re-scan on launch.
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

    App(
        status = status,
        onPickFolder = { launcher.launch(Unit) },
    )
}

package com.offlineplaya.android

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.offlineplaya.android.picker.OpenDocumentTreeContract
import com.offlineplaya.android.ui.PermissionRequiredScreen
import com.offlineplaya.shared.domain.player.MusicPlayer
import com.offlineplaya.shared.presentation.library.LibraryStateHolder
import com.offlineplaya.shared.presentation.metadata.BurnMetadataCoordinator
import com.offlineplaya.shared.presentation.navigation.AppNavigator
import com.offlineplaya.shared.presentation.playlist.PlaylistStateHolder
import com.offlineplaya.shared.presentation.settings.ArtworkStateHolder
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

private fun checkMandatoryPermissions(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        val read =
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
        val write =
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED
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
    val burnMetadataCoordinator: BurnMetadataCoordinator = koinInject()
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
    val lifecycleOwner = LocalLifecycleOwner.current

    // Check permissions immediately on launch and every time app resumes.
    var hasPermission by remember {
        mutableStateOf(checkMandatoryPermissions(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = checkMandatoryPermissions(context)
                if (hasPermission) coordinator.resyncAll()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermission = results.values.all { it }
        if (hasPermission) coordinator.resyncAll()
    }

    if (!hasPermission) {
        PermissionRequiredScreen {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ Mandatory "All Files Access"
                runCatching {
                    val intent =
                        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = "package:${context.packageName}".toUri()
                        }
                    context.startActivity(intent)
                }.onFailure {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            } else {
                // Older Android: Standard runtime storage permissions
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            }
        }
        return
    }

    // Standard activity-level logic only runs once permission is secured.
    BackHandler(enabled = stack.size > 1) {
        navigator.pop()
    }

    val readPickerLauncher = rememberLauncherForActivityResult(
        OpenDocumentTreeContract(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
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
        syncStatus = syncStatus,
        trackCount = trackCount,
        onPickFolder = { readPickerLauncher.launch(Unit) },
        onColorModeChange = themeStateHolder::setColorMode,
        onDynamicColorChange = themeStateHolder::setUseDynamicColor,
        onDownloadRemoteArtChange = artworkStateHolder::setDownloadRemoteArt,
        onAddDirectFolder = { path ->
            val name = path.substringAfterLast('/')
            coordinator.addAndSync(path, name)
        },
        onManageExternalStorageClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                runCatching {
                    val intent =
                        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    context.startActivity(intent)
                }.onFailure {
                    // Fallback to the general list if package-specific fails
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    context.startActivity(intent)
                }
            } else {
                // On older versions, just trigger a media scan of common folders
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath,
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
                    ),
                    null
                ) { path, _ ->
                    android.util.Log.d("OfflinePlaya", "Scanned $path")
                }
            }
        },
        dynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    )
}

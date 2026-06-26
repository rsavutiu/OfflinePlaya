package com.offlineplaya.android

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.offlineplaya.android.picker.OpenDocumentTreeContract
import com.offlineplaya.shared.domain.model.ColorMode
import com.offlineplaya.shared.domain.model.EqPreferences
import com.offlineplaya.shared.domain.player.MusicPlayer
import com.offlineplaya.shared.presentation.eq.EqualizerStateHolder
import com.offlineplaya.shared.presentation.library.LibraryStateHolder
import com.offlineplaya.shared.presentation.metadata.BurnMetadataCoordinator
import com.offlineplaya.shared.presentation.navigation.AppNavigator
import com.offlineplaya.shared.presentation.playlist.PlaylistStateHolder
import com.offlineplaya.shared.presentation.settings.ArtworkStateHolder
import com.offlineplaya.shared.presentation.settings.LyricsPreferencesStateHolder
import com.offlineplaya.shared.presentation.settings.PlaybackTuningStateHolder
import com.offlineplaya.shared.presentation.settings.ThemeStateHolder
import com.offlineplaya.shared.presentation.sync.LibrarySyncCoordinator
import com.offlineplaya.shared.presentation.ui.App
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.core.context.GlobalContext

class MainActivity : ComponentActivity() {

    private val equalizerStateHolder: EqualizerStateHolder by lazy {
        GlobalContext.get().get<EqualizerStateHolder>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // installSplashScreen() must run before super.onCreate so it can swap
        // the activity theme from Theme.MyApp.Splash to postSplashScreenTheme
        // before the window is created.
        val splashScreen = installSplashScreen()
        // The system dismisses the splash as soon as the first frame is ready,
        // which usually beats the Walkman clip. Hold it for the clip duration
        // so the user sees the whole animation.
        val splashStartedAt = SystemClock.uptimeMillis()
        splashScreen.setKeepOnScreenCondition {
            SystemClock.uptimeMillis() - splashStartedAt < SPLASH_HOLD_MS
        }
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge must run AFTER super.onCreate — the activity window
        // doesn't exist before that, so any pre-super call lands on the splash
        // window instead and the post-splash status bar comes back wrong (light
        // icons stuck on a transparent bar under dark Compose content).
        enableEdgeToEdge()
        setContent {
            KoinContext {
                AndroidApp()
            }
        }
    }

    /**
     * Volume keys drive the preamp at the edges of the normal volume range
     * (while the app is foreground — background presses stay system-handled):
     *
     *  - Vol+ with the music stream already at max → bump the preamp one step
     *    instead, up to [EqPreferences.MAX_PREAMP_PERCENT] (+100% ≈ +6 dB).
     *  - Vol− with any preamp engaged → drain the preamp back to 0 first;
     *    only then do further presses lower the stream volume as usual.
     *
     * Key autorepeat means holding the button steps repeatedly.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                val audio = getSystemService(AUDIO_SERVICE) as AudioManager
                val atMax = audio.getStreamVolume(AudioManager.STREAM_MUSIC) >=
                    audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val preamp = equalizerStateHolder.preferences.value.preampPercent
                if (atMax && preamp < EqPreferences.MAX_PREAMP_PERCENT) {
                    equalizerStateHolder.adjustPreampBy(EqPreferences.PREAMP_STEP_PERCENT)
                    return true
                }
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                val preamp = equalizerStateHolder.preferences.value.preampPercent
                if (preamp > 0) {
                    equalizerStateHolder.adjustPreampBy(-EqPreferences.PREAMP_STEP_PERCENT)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}

/** How long to hold the splash so the Walkman clip plays through. */
private const val SPLASH_HOLD_MS = 3000L

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
    val lyricsPreferencesStateHolder: LyricsPreferencesStateHolder = koinInject()
    val playbackTuningStateHolder: PlaybackTuningStateHolder = koinInject()
    val coordinator: LibrarySyncCoordinator = koinInject()
    val burnMetadataCoordinator: BurnMetadataCoordinator = koinInject()
    val library: LibraryStateHolder = koinInject()
    val playlists: PlaylistStateHolder = koinInject()
    val musicPlayer: MusicPlayer = koinInject()
    val equalizerStateHolder: com.offlineplaya.shared.presentation.eq.EqualizerStateHolder = koinInject()
    val lyricsStateHolder: com.offlineplaya.shared.presentation.lyrics.LyricsStateHolder = koinInject()
    val albumColorStateHolder: com.offlineplaya.shared.presentation.theme.AlbumColorStateHolder = koinInject()
    val tagEditorCoordinator: com.offlineplaya.shared.presentation.tag.TagEditorCoordinator = koinInject()
    val smartPlaylists: com.offlineplaya.shared.presentation.history.SmartPlaylistsStateHolder = koinInject()
    val themePreferences by themeStateHolder.preferences.collectAsState()
    val seedColor by albumColorStateHolder.seedColor.collectAsState()
    val artworkPreferences by artworkStateHolder.preferences.collectAsState()
    val lyricsPreferences by lyricsPreferencesStateHolder.preferences.collectAsState()
    val playbackPreferences by playbackTuningStateHolder.preferences.collectAsState()
    val syncStatus by coordinator.status.collectAsState()
    val trackCount by library.totalTrackCount.collectAsState()
    val stack by navigator.stack.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Drive the system-bar icon appearance from the *app's* resolved theme,
    // not the OS theme. enableEdgeToEdge()'s auto-detection keys off the system
    // dark/light setting, so with the in-app "Appearance: Dark" override active
    // on a light-system device it chose dark icons against our dark background —
    // the status-bar clock/icons (and the 3-button nav glyphs) were near
    // invisible. Mirror OfflinePlayaTheme's ColorMode → dark resolution here and
    // re-assert on every commit (SideEffect) so a config change can't revert to
    // the auto value.
    val view = LocalView.current
    val systemDark = isSystemInDarkTheme()
    val appDark = when (themePreferences.colorMode) {
        ColorMode.SYSTEM -> systemDark
        ColorMode.LIGHT -> false
        ColorMode.DARK -> true
    }
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            // Light bars (dark icons) only in light mode; in dark mode use light
            // (white) icons so the clock/battery and nav glyphs stay legible.
            controller.isAppearanceLightStatusBars = !appDark
            controller.isAppearanceLightNavigationBars = !appDark
            // Drop the translucent scrim the system paints behind the 3-button
            // nav bar so the app background runs edge-to-edge to the bottom and
            // the bar stops reading as a separate grey strip. API 29+.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
        }
    }

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

    Box {
        App(
            navigator = navigator,
            library = library,
            playlists = playlists,
            syncCoordinator = coordinator,
            burnMetadataCoordinator = burnMetadataCoordinator,
            musicPlayer = musicPlayer,
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
            onPickFolder = { readPickerLauncher.launch(Unit) },
            onColorModeChange = themeStateHolder::setColorMode,
            onDynamicColorChange = themeStateHolder::setUseDynamicColor,
            onAlbumArtColorChange = themeStateHolder::setUseAlbumArtColor,
            onDownloadRemoteArtChange = artworkStateHolder::setDownloadRemoteArt,
            onDownloadRemoteLyricsChange = lyricsPreferencesStateHolder::setDownloadRemoteLyrics,
            onSaveLyricsAsSidecarChange = lyricsPreferencesStateHolder::setSaveLyricsAsSidecar,
            onCrossfadeEnabledChange = playbackTuningStateHolder::setCrossfadeEnabled,
            onCrossfadeDurationChange = playbackTuningStateHolder::setCrossfadeDurationSeconds,
            dynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        )
    }
}

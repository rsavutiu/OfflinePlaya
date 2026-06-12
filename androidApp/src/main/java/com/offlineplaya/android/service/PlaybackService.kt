package com.offlineplaya.android.service

import android.app.PendingIntent
import android.content.Intent
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.offlineplaya.android.MainActivity
import com.offlineplaya.android.audio.AppEqualizerController
import com.offlineplaya.android.audio.CrossfadeController
import com.offlineplaya.android.audio.PreampVolumePlayer
import com.offlineplaya.android.auto.AutoLibraryCallback
import com.offlineplaya.shared.domain.player.MusicPlayer
import com.offlineplaya.shared.presentation.eq.EqualizerStateHolder
import com.offlineplaya.shared.presentation.settings.PlaybackTuningStateHolder
import com.offlineplaya.shared.presentation.library.LibraryStateHolder
import com.offlineplaya.shared.presentation.playlist.PlaylistStateHolder
import com.offlineplaya.shared.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.core.context.GlobalContext

/**
 * Foreground playback service. Hosts a single [ExoPlayer] + [MediaSession];
 * the in-process [com.offlineplaya.shared.data.player.Media3MusicPlayer]
 * binds to this via a `MediaController`.
 *
 * `MediaSessionService` (rather than `MediaLibraryService`) for now —
 * Android Auto browse-tree support arrives in a later phase when we add the
 * browse interface explicitly.
 */
class PlaybackService : MediaLibraryService() {

    private var mediaSession: MediaLibrarySession? = null
    private var audioSessionId: Int = AudioEffect.ERROR_BAD_VALUE
    private var equalizerController: AppEqualizerController? = null
    private var crossfadeController: CrossfadeController? = null

    /**
     * Service-scoped coroutine context. Used by [equalizerController] so
     * Equalizer state collection lives exactly as long as the service.
     */
    private val serviceScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Generate the audio session id explicitly and pin it on the player so
        // BOTH the EQ and the crossfade secondary engine bind to the same
        // session (rather than reading back whatever the player happened to
        // allocate). See CrossfadeController for why the secondary shares it.
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val sessionId = audioManager.generateAudioSessionId()
        val player = ExoPlayer.Builder(this)
            .setHandleAudioBecomingNoisy(true)
            .setAudioAttributes(MUSIC_ATTRIBUTES, /* handleAudioFocus = */ true)
            .build()
            .apply { setAudioSessionId(sessionId) }

        audioSessionId = sessionId

        // The equalizer controller is the single source of truth for the
        // audio-effects pipeline: it OWNS the choice between our app EQ and
        // the OEM system-EQ broadcast. Started here, after the ExoPlayer
        // exists; released in onDestroy. See [AppEqualizerController] kdoc
        // for why these can't co-exist.
        val koin = GlobalContext.get()
        val stateHolder = koin.get<EqualizerStateHolder>()
        val logger = koin.get<AppLogger>()
        equalizerController = AppEqualizerController(
            context = this,
            audioSessionId = audioSessionId,
            stateHolder = stateHolder,
            scope = serviceScope,
            logger = logger,
        ).also { it.start() }

        // Crossfade overlay: leaves `player` as the untouched MediaSession
        // player and drives a second engine for the track-to-track overlap.
        // Shares the EQ audio session and the service scope. No-op while the
        // user has crossfade disabled (the default-off path is native gapless).
        crossfadeController = CrossfadeController(
            context = this,
            mainPlayer = player,
            audioSessionId = audioSessionId,
            preferences = koin.get<PlaybackTuningStateHolder>().preferences,
            scope = serviceScope,
            logger = logger,
        ).also { it.start() }

        val sessionActivityIntent = PendingIntent.getActivity(
            /* context = */ this,
            /* requestCode = */ 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE,
        )

        // Wrap the raw player so the session claims hardware-volume control
        // with an extended range: the top 10 indices past max stream volume
        // are the preamp zone. Releasing the session player releases the
        // wrapper, which releases the ExoPlayer underneath.
        val volumePlayer = PreampVolumePlayer(
            player = player,
            context = this,
            stateHolder = stateHolder,
            scope = serviceScope,
            logger = logger,
        )
        // MediaLibrarySession (not MediaSession) so Android Auto can browse
        // our library. Callback wires the browse tree into shared state
        // holders; on phone (no Auto browser) this is dormant and behaves
        // identically to the previous MediaSession. It also tells the volume
        // wrapper when the car host attaches so it drops the remote-volume
        // claim (Auto otherwise shows "playing on another device").
        val autoCallback = AutoLibraryCallback(
            context = applicationContext,
            library = koin.get<LibraryStateHolder>(),
            playlists = koin.get<PlaylistStateHolder>(),
            scope = serviceScope,
            logger = logger,
            onCarConnectionChanged = volumePlayer::setCarConnected,
        )
        mediaSession = MediaLibrarySession.Builder(this, volumePlayer, autoCallback)
            .setSessionActivity(sessionActivityIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Stop playback (and the foreground notification) when the user swipes
        // the app away. Without this the notification can linger.
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        crossfadeController?.release()
        crossfadeController = null
        equalizerController?.release()
        equalizerController = null
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private companion object {
        val MUSIC_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
    }
}

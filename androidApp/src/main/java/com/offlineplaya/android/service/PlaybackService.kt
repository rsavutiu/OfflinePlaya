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
import com.offlineplaya.android.audio.AndroidPlayToPcController
import com.offlineplaya.android.audio.AppEqualizerController
import com.offlineplaya.android.audio.CrossfadeController
import com.offlineplaya.android.audio.DacPlaybackController
import com.offlineplaya.android.audio.PcmTap
import com.offlineplaya.android.audio.PreampVolumePlayer
import com.offlineplaya.android.audio.StreamingRenderersFactory
import com.offlineplaya.android.auto.AutoLibraryCallback
import com.offlineplaya.shared.domain.audio.DacOutputManager
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    private var dacController: DacPlaybackController? = null
    private var playToPcController: AndroidPlayToPcController? = null
    private var logger: AppLogger? = null

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
        // The PCM tap lives in the audio sink, which is fixed at player
        // construction — so a streaming-capable RenderersFactory is wired in
        // now and stays a harmless pass-through until "Play to PC" connects.
        val pcmTap = PcmTap()
        val player = ExoPlayer.Builder(this, StreamingRenderersFactory(this, pcmTap))
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
        val logger = koin.get<AppLogger>().also { this.logger = it }

        // USB DAC routing. Built first because its bitPerfectActive flag tells
        // the EQ, preamp, volume ramp and crossfade below to step out of the
        // signal path while the DAC is fed bit-perfect.
        // Play-to-PC controller is a shared singleton (the picker UI holds the
        // same one). Its `active` flag folds into the DSP-bypass gate below.
        val playToPc = koin.get<AndroidPlayToPcController>()

        val dac = DacPlaybackController(
            context = this,
            player = player,
            manager = koin.get<DacOutputManager>(),
            scope = serviceScope,
            logger = logger,
            playToPcActive = playToPc.active,
        ).also {
            dacController = it
            it.start()
        }

        // Single bypass gate: DSP steps out when the DAC is fed bit-perfect OR
        // while streaming to a PC. The stream must carry clean, decoded PCM, so
        // EQ / preamp / crossfade are excluded exactly as for bit-perfect.
        val dspBypass: StateFlow<Boolean> = combine(
            dac.bitPerfectActive,
            playToPc.active,
        ) { bitPerfect, pc -> bitPerfect || pc }
            .stateIn(serviceScope, SharingStarted.Eagerly, false)

        equalizerController = AppEqualizerController(
            context = this,
            audioSessionId = audioSessionId,
            stateHolder = stateHolder,
            bypass = dspBypass,
            scope = serviceScope,
            logger = logger,
        ).also { it.start() }

        // Crossfade overlay: leaves `player` as the untouched MediaSession
        // player and drives a second engine for the track-to-track overlap.
        // The secondary runs on its OWN audio session — sharing the EQ session
        // broke per-track volume (effect chain takes over volume for the
        // session), which made the overlap play both tracks at full loudness.
        // No-op while crossfade is disabled (that path is native gapless).
        crossfadeController = CrossfadeController(
            context = this,
            mainPlayer = player,
            preferences = koin.get<PlaybackTuningStateHolder>().preferences,
            bypass = dspBypass,
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
            bypass = dspBypass,
            scope = serviceScope,
            logger = logger,
        )

        // Hand the live player + PCM tap to the Play-to-PC controller so it can
        // mute local output and capture decoded PCM while streaming.
        playToPc.attachPlayer(player, pcmTap)
        playToPcController = playToPc
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
            queue = koin.get(),
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
        // Teardown must be all-or-most: a throw in any single step must not skip
        // the rest — especially the native player/session release — or the
        // mandatory super.onDestroy(). Isolate every step.
        safeRelease("playToPc") { playToPcController?.detachPlayer() }
        playToPcController = null
        safeRelease("crossfade") { crossfadeController?.release() }
        crossfadeController = null
        safeRelease("equalizer") { equalizerController?.release() }
        equalizerController = null
        safeRelease("dac") { dacController?.release() }
        dacController = null
        safeRelease("serviceScope") { serviceScope.cancel() }
        mediaSession?.let { session ->
            safeRelease("player") { session.player.release() }
            safeRelease("mediaSession") { session.release() }
        }
        mediaSession = null
        logger = null
        super.onDestroy()
    }

    /** Run one teardown step, logging (not propagating) any failure. */
    private inline fun safeRelease(what: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            logger?.e(TAG, "Failed to release $what during onDestroy", e)
        }
    }

    private companion object {
        const val TAG = "PlaybackService"

        val MUSIC_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
    }
}

package com.offlineplaya.android.service

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.offlineplaya.android.MainActivity

/**
 * Foreground playback service. Hosts a single [ExoPlayer] + [MediaSession];
 * the in-process [com.offlineplaya.shared.data.player.Media3MusicPlayer]
 * binds to this via a `MediaController`.
 *
 * `MediaSessionService` (rather than `MediaLibraryService`) for now —
 * Android Auto browse-tree support arrives in a later phase when we add the
 * browse interface explicitly.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setHandleAudioBecomingNoisy(true)
            .setAudioAttributes(MUSIC_ATTRIBUTES, /* handleAudioFocus = */ true)
            .build()

        val sessionActivityIntent = PendingIntent.getActivity(
            /* context = */ this,
            /* requestCode = */ 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
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

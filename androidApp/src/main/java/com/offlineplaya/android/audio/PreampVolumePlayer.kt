package com.offlineplaya.android.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.media3.common.DeviceInfo
import androidx.media3.common.ForwardingSimpleBasePlayer
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.offlineplaya.shared.domain.model.EqPreferences
import com.offlineplaya.shared.presentation.eq.EqualizerStateHolder
import com.offlineplaya.shared.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Session player that takes over the hardware volume keys whenever our
 * [androidx.media3.session.MediaSession] is the active media session — i.e.
 * while music is playing (or recently paused), screen on or off, app in
 * foreground or background.
 *
 * It does this by reporting [DeviceInfo.PLAYBACK_TYPE_REMOTE] with an
 * **extended volume range**: indices `0..streamMax` map 1:1 onto the real
 * `STREAM_MUSIC` volume, and the extra `streamMax+1 .. streamMax+10` indices
 * are the preamp zone — each extra step is +[EqPreferences.PREAMP_STEP_PERCENT]%
 * of loudness boost (see [AppEqualizerController]'s LoudnessEnhancer).
 *
 * Volume keys therefore behave as one continuous ramp:
 *  - Vol+ raises stream volume; at max it keeps going into the preamp.
 *  - Vol− drains the preamp back to 0 first, then lowers stream volume.
 *
 * The same policy lives in MainActivity.onKeyDown for the case where the app
 * is foreground but nothing is playing (no active session to route keys to).
 *
 * Trade-off: because the session claims remote-volume control, the system
 * volume dialog shown during playback uses our combined scale instead of the
 * plain media-volume slider.
 *
 * Android Auto takes "remote playback type" literally and shows a "seems to
 * be playing on another device" line under the track metadata. While a car
 * host is connected ([setCarConnected]) we drop the override entirely — the
 * car owns its own volume, so the preamp-on-volume-keys ramp is meaningless
 * there anyway.
 */
class PreampVolumePlayer(
    player: Player,
    private val context: Context,
    private val stateHolder: EqualizerStateHolder,
    scope: CoroutineScope,
    private val logger: AppLogger,
) : ForwardingSimpleBasePlayer(player) {

    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val streamMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Stream-volume changes from outside this class (quick-settings slider,
     * other apps, BT absolute volume) must be reflected into our reported
     * device volume or the system dialog drifts out of sync.
     */
    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, -1) == AudioManager.STREAM_MUSIC) {
                mainHandler.post { invalidateState() }
            }
        }
    }
    private var receiverRegistered = false
    private var carConnected = false
    private val preampJob: Job

    init {
        runCatching {
            ContextCompat.registerReceiver(
                context,
                volumeReceiver,
                IntentFilter(VOLUME_CHANGED_ACTION),
                ContextCompat.RECEIVER_EXPORTED,
            )
            receiverRegistered = true
        }.onFailure { logger.w(TAG, "Could not register volume receiver: ${it.message}") }

        // Preamp changes from the EQ-page slider (or the activity key
        // handler) move our reported device volume too. Main dispatcher:
        // SimpleBasePlayer state must be touched on the application thread.
        preampJob = scope.launch(Dispatchers.Main) {
            stateHolder.preferences.map { it.preampPercent }
                .distinctUntilChanged()
                .collect { invalidateState() }
        }
    }

    private fun preampSteps(): Int =
        stateHolder.preferences.value.preampPercent / EqPreferences.PREAMP_STEP_PERCENT

    /**
     * Called from the session callback on the application thread when the
     * Android Auto host connects or disconnects.
     */
    fun setCarConnected(connected: Boolean) {
        if (carConnected == connected) return
        carConnected = connected
        invalidateState()
    }

    override fun getState(): SimpleBasePlayer.State {
        val state = super.getState()
        // In the car, report the wrapped player's state untouched: claiming
        // PLAYBACK_TYPE_REMOTE makes Auto print a "playing on another
        // device" warning under the metadata.
        if (carConnected) return state
        @Suppress("DEPRECATION") // legacy controllers still probe the non-flag commands
        val commands = state.availableCommands.buildUpon()
            .addAll(
                Player.COMMAND_GET_DEVICE_VOLUME,
                Player.COMMAND_SET_DEVICE_VOLUME,
                Player.COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS,
                Player.COMMAND_ADJUST_DEVICE_VOLUME,
                Player.COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS,
            )
            .build()
        return state.buildUpon()
            .setDeviceInfo(
                DeviceInfo.Builder(DeviceInfo.PLAYBACK_TYPE_REMOTE)
                    .setMinVolume(0)
                    .setMaxVolume(streamMax + TOTAL_PREAMP_STEPS)
                    .build(),
            )
            .setDeviceVolume(
                audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) + preampSteps(),
            )
            .setIsDeviceMuted(audioManager.isStreamMute(AudioManager.STREAM_MUSIC))
            .setAvailableCommands(commands)
            .build()
    }

    override fun handleSetDeviceVolume(deviceVolume: Int, flags: Int): ListenableFuture<*> {
        val clamped = deviceVolume.coerceIn(0, streamMax + TOTAL_PREAMP_STEPS)
        if (clamped <= streamMax) {
            setStreamVolumeSafely(clamped)
            stateHolder.setPreampPercent(0)
        } else {
            setStreamVolumeSafely(streamMax)
            stateHolder.setPreampPercent(
                (clamped - streamMax) * EqPreferences.PREAMP_STEP_PERCENT,
            )
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleIncreaseDeviceVolume(flags: Int): ListenableFuture<*> {
        if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) < streamMax) {
            adjustStreamVolumeSafely(AudioManager.ADJUST_RAISE)
        } else {
            stateHolder.adjustPreampBy(EqPreferences.PREAMP_STEP_PERCENT)
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleDecreaseDeviceVolume(flags: Int): ListenableFuture<*> {
        if (stateHolder.preferences.value.preampPercent > 0) {
            stateHolder.adjustPreampBy(-EqPreferences.PREAMP_STEP_PERCENT)
        } else {
            adjustStreamVolumeSafely(AudioManager.ADJUST_LOWER)
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleSetDeviceMuted(muted: Boolean, flags: Int): ListenableFuture<*> {
        adjustStreamVolumeSafely(
            if (muted) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE,
        )
        return Futures.immediateVoidFuture()
    }

    override fun handleRelease(): ListenableFuture<*> {
        preampJob.cancel()
        if (receiverRegistered) {
            runCatching { context.unregisterReceiver(volumeReceiver) }
            receiverRegistered = false
        }
        return super.handleRelease()
    }

    /**
     * Volume writes can throw SecurityException under Do-Not-Disturb on some
     * OEMs ("not allowed to change Do Not Disturb state"). Swallow — a key
     * press that does nothing beats crashing the playback service.
     */
    private fun setStreamVolumeSafely(volume: Int) {
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
        } catch (t: Throwable) {
            logger.w(TAG, "setStreamVolume($volume) rejected: ${t.message}")
        }
    }

    private fun adjustStreamVolumeSafely(direction: Int) {
        try {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
        } catch (t: Throwable) {
            logger.w(TAG, "adjustStreamVolume($direction) rejected: ${t.message}")
        }
    }

    private companion object {
        const val TAG = "PreampVolumePlayer"

        /** Preamp zone size in device-volume indices: 100% / 10% = 10 steps. */
        const val TOTAL_PREAMP_STEPS =
            EqPreferences.MAX_PREAMP_PERCENT / EqPreferences.PREAMP_STEP_PERCENT

        /** Hidden-but-stable system broadcast for stream volume changes. */
        const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
        const val EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"
    }
}

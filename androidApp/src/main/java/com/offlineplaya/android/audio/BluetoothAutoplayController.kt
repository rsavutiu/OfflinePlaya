package com.offlineplaya.android.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.offlineplaya.shared.domain.model.PlaybackPreferences
import com.offlineplaya.shared.domain.player.MusicPlayer
import com.offlineplaya.shared.util.AppLogger
import kotlinx.coroutines.flow.StateFlow

/**
 * Resumes playback when a Bluetooth audio device connects — the
 * "get in the car and the music just starts" feature. Opt-in via
 * [PlaybackPreferences.bluetoothAutoplayEnabled] (Settings → Playback).
 *
 * Listens through [AudioManager.registerAudioDeviceCallback] rather than the
 * Bluetooth broadcasts: the audio-routing callback fires only once the device
 * is actually available as an *output* (so play() lands on the headset, not
 * the speaker) and needs no BLUETOOTH_CONNECT permission.
 *
 * Fires only when a queue is loaded but paused — exactly the state the
 * persisted-queue restore leaves the player in on cold start. Never interrupts
 * or restarts an already-playing session, and does nothing when the queue is
 * empty.
 *
 * Owned by the Application (like `AutoRescanController`): the app process must
 * be alive for this to see the connect. If the process is dead, the existing
 * `onPlaybackResumption` path still answers the headset's play *button* — this
 * controller covers the no-button-press case while we're alive.
 */
class BluetoothAutoplayController(
    context: Context,
    private val player: MusicPlayer,
    private val preferences: StateFlow<PlaybackPreferences>,
    private val logger: AppLogger,
) {

    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /** Registration time — see [callback] for why early events are dropped. */
    private var registeredAt = 0L

    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            // registerAudioDeviceCallback synthesizes an immediate "added"
            // event for every device already present. Acting on that would
            // auto-play on app launch just because headphones were already
            // connected — only genuine connects after startup count.
            if (SystemClock.elapsedRealtime() - registeredAt < STARTUP_GRACE_MS) return
            if (!preferences.value.bluetoothAutoplayEnabled) return
            if (addedDevices.none { it.isBluetoothOutput() }) return

            val state = player.playbackState.value
            if (state.currentTrack == null || state.isPlaying) return

            logger.i(TAG, "Bluetooth output connected; resuming playback")
            player.play()
        }
    }

    /** Main-looper handler so [callback] (and player.play()) run on main. */
    fun start() {
        registeredAt = SystemClock.elapsedRealtime()
        audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
    }

    fun stop() {
        audioManager.unregisterAudioDeviceCallback(callback)
    }

    private fun AudioDeviceInfo.isBluetoothOutput(): Boolean {
        if (!isSink) return false
        return type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                (type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    type == AudioDeviceInfo.TYPE_BLE_SPEAKER)) ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                type == AudioDeviceInfo.TYPE_HEARING_AID)
    }

    private companion object {
        const val TAG = "BluetoothAutoplay"

        /**
         * Window after registration inside which "added" events are treated
         * as the synthetic already-connected snapshot, not a fresh connect.
         */
        const val STARTUP_GRACE_MS = 1_000L
    }
}

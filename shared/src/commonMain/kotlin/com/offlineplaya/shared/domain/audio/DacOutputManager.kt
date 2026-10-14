package com.offlineplaya.shared.domain.audio

import kotlinx.coroutines.flow.StateFlow

/**
 * Owns the route to an external USB DAC and, where the platform allows it,
 * a bit-perfect path to it (Android 14+ `AudioMixerAttributes`).
 *
 * Contract for the playback engine:
 *  1. Call [prepareFor] with the track's own format whenever the playing track
 *     changes, then route output according to the returned [OutputMode].
 *  2. Observe [state]. `mode` can change mid-track (DAC unplugged, setting
 *     toggled, platform revoked bit-perfect).
 *  3. While `state.softwareDspAllowed` is `false`, keep EQ, preamp, crossfade
 *     and software volume out of the signal path.
 *  4. Call [releasePlayback] when the player goes away.
 */
interface DacOutputManager {
    val state: StateFlow<DacOutputState>

    /**
     * Starts DAC detection. Reference-counted: the UI and the playback service
     * can both start it; detection stops when every [start] has been matched
     * by a [stop].
     */
    fun start()
    fun stop()

    fun prepareFor(source: PcmFormat): OutputMode

    /** Drops any bit-perfect request and forgets the last track. */
    fun releasePlayback()

    /** Per-DAC user setting for the currently connected DAC; no-op without one. */
    fun setBitPerfectEnabled(enabled: Boolean)
}

/**
 * Per-DAC settings. Synchronous on purpose: [DacOutputManager.prepareFor] runs on
 * the playback path at every track boundary and must not suspend.
 */
interface DacPreferences {
    fun isBitPerfectEnabled(deviceKey: String): Boolean
    fun setBitPerfectEnabled(deviceKey: String, enabled: Boolean)
}

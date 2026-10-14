package com.offlineplaya.shared.domain.audio

import kotlinx.coroutines.flow.StateFlow

/**
 * Owns the route to an external USB DAC and, where the platform allows it,
 * a bit-perfect path to it (Android 14+ `AudioMixerAttributes`).
 *
 * Contract for the playback engine:
 *  1. Call [prepareFor] with the decoded track format before each track starts
 *     and configure the audio sink from the returned [OutputMode].
 *  2. Observe [state]. If `mode` changes mid-track (DAC unplugged, setting
 *     toggled, platform revoked bit-perfect), re-prepare the sink.
 *  3. While `state.softwareDspAllowed` is `false`, keep EQ, preamp, crossfade
 *     and software volume out of the signal path.
 */
interface DacOutputManager {
    val state: StateFlow<DacOutputState>
    fun start()
    fun stop()
    fun prepareFor(source: PcmFormat): OutputMode
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

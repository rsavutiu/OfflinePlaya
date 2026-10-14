package com.offlineplaya.shared.presentation.audio

import com.offlineplaya.shared.domain.audio.DacOutputManager
import com.offlineplaya.shared.domain.audio.DacOutputState
import kotlinx.coroutines.flow.StateFlow

/**
 * UI-facing view of the USB DAC route: the signal-path badge on Now Playing
 * and the bit-perfect switch in Settings → Playback.
 *
 * The UI calls [start] / [stop] with its own lifecycle so a DAC plugged in
 * before anything plays still shows up in Settings. Detection is
 * reference-counted inside [DacOutputManager], so this never interferes with
 * the playback service's own start / stop.
 */
class DacOutputStateHolder(
    private val manager: DacOutputManager,
) {
    val state: StateFlow<DacOutputState> get() = manager.state

    fun start() = manager.start()

    fun stop() = manager.stop()

    fun setBitPerfectEnabled(enabled: Boolean) = manager.setBitPerfectEnabled(enabled)
}

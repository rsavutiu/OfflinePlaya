package com.offlineplaya.shared.domain.stream

import kotlinx.coroutines.flow.StateFlow

/**
 * A PC receiver on the LAN, as shown in the picker. Domain-side mirror of the
 * wire module's `ReceiverInfo` (which lives in the JVM-only `:streamProtocol`
 * module that `commonMain` can't depend on).
 *
 * @property key stable identity for de-duping discovery results and tracking
 *   the selected receiver across re-resolves (service name on mDNS, or
 *   `host:port` for a manual entry).
 */
data class PcReceiver(
    val name: String,
    val host: String,
    val port: Int,
    val key: String = "$host:$port",
)

/**
 * A connection problem to show the user. A small type (not a raw string) so the
 * UI owns the wording and it can be localised — the controller must never emit
 * English or raw exception text as user-facing copy.
 */
sealed interface PlayToPcError {
    /** The connection attempt never succeeded. */
    data class ConnectFailed(val host: String, val port: Int) : PlayToPcError

    /** An established stream dropped (WiFi died, receiver quit). */
    data object ConnectionLost : PlayToPcError
}

/**
 * UI-facing state of the "Play to PC" feature. Strictly off by default — the
 * app stays offline-only until the user turns it on and picks a receiver.
 */
data class PlayToPcState(
    /** `true` while the picker is open and the phone is browsing for receivers. */
    val discovering: Boolean = false,
    val receivers: List<PcReceiver> = emptyList(),
    /** Non-null once streaming to a receiver (socket open, PCM flowing). */
    val connected: PcReceiver? = null,
    /** Set while a connection attempt is in flight (socket not yet up). */
    val connecting: PcReceiver? = null,
    /** Last error surfaced to the UI, or `null`. */
    val error: PlayToPcError? = null,
)

/**
 * Owns the phone's "Play to PC" mode: LAN discovery, the selected receiver, and
 * the connection lifecycle. Implemented on Android (`AndroidPlayToPcController`)
 * on top of `NsdManager` discovery + the TCP `StreamSender`.
 *
 * [active] is the DSP-bypass gate: while it is `true` the EQ, preamp, crossfade
 * and USB-DAC path step out exactly like `bitPerfectActive` does, because the
 * stream must carry clean, decoded PCM (see docs/plans/03-stream-to-pc.md).
 */
interface PlayToPcController {
    val state: StateFlow<PlayToPcState>

    /** `true` once connected to a receiver — folded into the DSP-bypass gate. */
    val active: StateFlow<Boolean>

    /** Start/stop browsing the LAN. Call [startDiscovery] only while the picker is open. */
    fun startDiscovery()
    fun stopDiscovery()

    /** Begin streaming to a discovered receiver. */
    fun connect(receiver: PcReceiver)

    /** Begin streaming to a hand-entered IP:port (fallback for hostile networks). */
    fun connectManual(host: String, port: Int)

    /** Stop streaming and return audio to the phone's own output. */
    fun disconnect()
}

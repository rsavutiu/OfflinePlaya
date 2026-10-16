package com.offlineplaya.stream

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Control-channel messages (the `CONTROL` frame payload, JSON). The audio
 * itself travels in separate `AUDIO` frames — see [StreamProtocol].
 *
 * The phone is the master: play/pause/skip happen there and simply change what
 * PCM is streamed, so the control channel carries only what the receiver needs
 * to react to — the handshake, format changes, volume, buffer flushes on seek,
 * heartbeats and a clean goodbye.
 */
@Serializable
sealed interface StreamControl {

    @Serializable
    @SerialName("hello")
    data class Hello(
        val name: String,
        val protocolVersion: Int = StreamProtocol.VERSION,
    ) : StreamControl

    @Serializable
    @SerialName("format")
    data class Format(val format: StreamFormat) : StreamControl

    @Serializable
    @SerialName("volume")
    data class Volume(val percent: Int) : StreamControl

    /** Drop buffered audio — sent on seek/skip so controls feel immediate. */
    @Serializable
    @SerialName("flush")
    data object Flush : StreamControl

    @Serializable
    @SerialName("heartbeat")
    data object Heartbeat : StreamControl

    @Serializable
    @SerialName("bye")
    data object Bye : StreamControl
}

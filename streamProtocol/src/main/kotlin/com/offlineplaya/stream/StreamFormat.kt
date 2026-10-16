package com.offlineplaya.stream

import kotlinx.serialization.Serializable

/**
 * The PCM format currently on the wire. Announced in a [StreamControl.Format]
 * frame before (and whenever it changes during) the audio stream, so the
 * receiver can (re)open its output line to match.
 *
 * v1 only ever carries signed PCM; [encoding] is kept explicit for the
 * handshake and future-proofing.
 */
@Serializable
data class StreamFormat(
    val sampleRate: Int,
    val channels: Int,
    val bitsPerSample: Int,
    val bigEndian: Boolean = false,
    val encoding: String = "PCM_SIGNED",
) {
    /** Bytes in one interleaved sample frame (all channels). */
    val frameBytes: Int get() = channels * (bitsPerSample / 8)

    /** Byte rate of the live stream — handy for buffer sizing. */
    val bytesPerSecond: Int get() = frameBytes * sampleRate
}

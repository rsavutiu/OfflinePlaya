package com.offlineplaya.android.audio

import androidx.media3.common.C
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import com.offlineplaya.stream.StreamFormat

/**
 * The capture point for decoded PCM. Installed as the first processor in the
 * ExoPlayer audio sink's chain via [StreamingRenderersFactory], so it sees the
 * decoded PCM for every track and simply forwards a copy to its [listener] —
 * local playback is unaffected (the tee outputs its input unchanged).
 *
 * Dumb on purpose: it tracks the current [StreamFormat] (updated on every
 * `flush`, which TeeAudioProcessor fires on (re)configure / end-of-stream) and
 * copies each buffer to the listener. The transport decisions — when to send a
 * `Format`, when to `Flush` — live in [PlayToPcPlaybackController], the single
 * consumer, so ordering is controlled in one place.
 *
 * All callbacks run on ExoPlayer's playback thread.
 */
class PcmTap : TeeAudioProcessor.AudioBufferSink {

    fun interface Listener {
        /** A copy of decoded PCM, tagged with the format it is in. */
        fun onPcm(pcm: ByteArray, format: StreamFormat)
    }

    @Volatile
    var listener: Listener? = null

    /** Current wire format, or `null` for an encoding we can't carry (e.g. float). */
    @Volatile
    var currentFormat: StreamFormat? = null
        private set

    override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        currentFormat = pcmEncodingToStreamFormat(sampleRateHz, channelCount, encoding)
    }

    override fun handleBuffer(buffer: java.nio.ByteBuffer) {
        val l = listener ?: return
        val fmt = currentFormat ?: return // unsupported encoding — don't stream garbage
        // Duplicate before reading so we never move the buffer the sink reuses.
        val src = buffer.duplicate()
        val copy = ByteArray(src.remaining())
        src.get(copy)
        if (copy.isNotEmpty()) l.onPcm(copy, fmt)
    }
}

/**
 * Maps a Media3 PCM encoding to the signed-PCM wire [StreamFormat], or `null`
 * when it isn't a signed-integer PCM we can represent (unsigned 8-bit and
 * float output are not carried in v1 — the receiver only opens signed lines).
 */
fun pcmEncodingToStreamFormat(sampleRate: Int, channels: Int, encoding: Int): StreamFormat? {
    if (sampleRate <= 0 || channels <= 0) return null
    val (bits, bigEndian) = when (encoding) {
        C.ENCODING_PCM_16BIT -> 16 to false
        C.ENCODING_PCM_16BIT_BIG_ENDIAN -> 16 to true
        C.ENCODING_PCM_24BIT -> 24 to false
        C.ENCODING_PCM_24BIT_BIG_ENDIAN -> 24 to true
        C.ENCODING_PCM_32BIT -> 32 to false
        C.ENCODING_PCM_32BIT_BIG_ENDIAN -> 32 to true
        else -> return null // ENCODING_PCM_8BIT (unsigned), ENCODING_PCM_FLOAT, non-PCM
    }
    return StreamFormat(
        sampleRate = sampleRate,
        channels = channels,
        bitsPerSample = bits,
        bigEndian = bigEndian,
    )
}

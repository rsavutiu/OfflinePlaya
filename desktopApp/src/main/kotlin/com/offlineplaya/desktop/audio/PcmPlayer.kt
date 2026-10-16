package com.offlineplaya.desktop.audio

import com.offlineplaya.stream.StreamFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.FloatControl
import javax.sound.sampled.SourceDataLine

/**
 * The live PCM sink. [open] (re)opens the output line for a format; [write]
 * feeds it raw PCM straight from the socket. **Nothing is ever written to
 * disk** — this is a pass-through to the speakers.
 *
 * [write] blocks when the line's buffer is full (that's the back-pressure that
 * paces playback), so it must be called off the UI thread — from the
 * Dispatchers.IO read loop in [com.offlineplaya.desktop.net.StreamServer].
 *
 * v1 uses `javax.sound.sampled`, which plays through the OS mixer (shared
 * mode): lossless over the wire, but not bit-perfect at the PC's DAC. True
 * exclusive-mode output is a later, per-OS add-on (see the design doc).
 */
class PcmPlayer {

    @Volatile
    private var line: SourceDataLine? = null

    fun open(format: StreamFormat) {
        close()
        val af = AudioFormat(
            format.sampleRate.toFloat(),
            format.bitsPerSample,
            format.channels,
            /* signed = */ true,
            /* bigEndian = */ format.bigEndian,
        )
        line = AudioSystem.getSourceDataLine(af).apply {
            open(af)
            start()
        }
    }

    /** Blocking — call from an IO context. No-op until [open] has run. */
    fun write(pcm: ByteArray, length: Int = pcm.size) {
        line?.write(pcm, 0, length)
    }

    /** Drop buffered audio so a seek/skip is heard promptly. */
    fun flush() {
        line?.flush()
    }

    /**
     * v1 scales volume on the phone (PCM is pre-attenuated), keeping this sink
     * dumb. Kept here for when/if we move volume control to the PC: MASTER_GAIN
     * where the line exposes it.
     */
    fun setVolume(percent: Int) {
        val l = line ?: return
        if (!l.isControlSupported(FloatControl.Type.MASTER_GAIN)) return
        val ctrl = l.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
        val clamped = percent.coerceIn(0, 100) / 100f
        // Map 0..1 onto the control's dB range (min..max).
        ctrl.value = (ctrl.minimum + (ctrl.maximum - ctrl.minimum) * clamped)
            .coerceIn(ctrl.minimum, ctrl.maximum)
    }

    fun close() {
        line?.let { l ->
            runCatching {
                l.drain()
                l.stop()
                l.close()
            }
        }
        line = null
    }
}

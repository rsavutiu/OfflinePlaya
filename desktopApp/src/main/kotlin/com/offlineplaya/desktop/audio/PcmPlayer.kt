package com.offlineplaya.desktop.audio

import com.offlineplaya.stream.PcmRingBuffer
import com.offlineplaya.stream.StreamFormat
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.FloatControl
import javax.sound.sampled.SourceDataLine

/**
 * The live PCM sink. [open] (re)opens the output line for a format; [write]
 * feeds it raw PCM straight from the socket reader thread. **Nothing is ever
 * written to disk** — this is a pass-through to the speakers.
 *
 * A ~1 s [PcmRingBuffer] decouples the socket reader (producer) from a private
 * writer thread that owns the [SourceDataLine] (consumer), riding out WiFi
 * jitter and keeping playback gapless:
 *
 *  - [write] blocks the reader while the ring is full (TCP back-pressure); the
 *    sender paces at playback rate, so in steady state this barely triggers.
 *  - the writer thread is the ONLY thread that touches the line — so [flush]
 *    (seek/skip) just clears the ring and hands the line a flush request via a
 *    flag, never calling into the line from the reader thread.
 *  - on underrun the writer feeds a short silence chunk rather than letting the
 *    line starve, so a WiFi hiccup is silence, not a crackle.
 *
 * v1 uses `javax.sound.sampled`, which plays through the OS mixer (shared
 * mode): lossless over the wire, but not bit-perfect at the PC's DAC. True
 * exclusive-mode output is a later, per-OS add-on (see the design doc).
 */
class PcmPlayer {

    @Volatile
    private var line: SourceDataLine? = null

    @Volatile
    private var format: StreamFormat? = null

    @Volatile
    private var ring: PcmRingBuffer? = null

    @Volatile
    private var writer: Thread? = null

    /** Set by [flush]; consumed on the writer thread so only it touches the line. */
    private val flushRequested = AtomicBoolean(false)

    private val running = AtomicBoolean(false)

    /**
     * (Re)opens the output line for [newFormat]. A no-op if the format is
     * unchanged and the line is already open, so a redundant Format frame
     * doesn't cause an audible gap.
     */
    @Synchronized
    fun open(newFormat: StreamFormat) {
        if (newFormat == format && line != null) return
        closeInternal(drain = false) // format change: don't block draining the old line

        val af = AudioFormat(
            newFormat.sampleRate.toFloat(),
            newFormat.bitsPerSample,
            newFormat.channels,
            /* signed = */ true,
            /* bigEndian = */ newFormat.bigEndian,
        )
        val newLine = AudioSystem.getSourceDataLine(af).apply {
            open(af)
            start()
        }
        // ~1 s of slack, rounded up to a whole sample frame.
        val capacity = newFormat.bytesPerSecond + newFormat.frameBytes
        val newRing = PcmRingBuffer(capacity)

        line = newLine
        ring = newRing
        format = newFormat
        flushRequested.set(false)
        running.set(true)
        writer = Thread({ writeLoop(newLine, newRing, newFormat) }, "PcmPlayer-writer").apply {
            isDaemon = true
            start()
        }
    }

    /** Blocking — call from the socket reader (IO) thread. No-op until [open]. */
    fun write(pcm: ByteArray, length: Int = pcm.size) {
        ring?.write(pcm, 0, length)
    }

    /** Drop buffered audio so a seek/skip is heard promptly (reader thread). */
    fun flush() {
        ring?.flush()
        flushRequested.set(true)
    }

    fun setVolume(percent: Int) {
        val l = line ?: return
        if (!l.isControlSupported(FloatControl.Type.MASTER_GAIN)) return
        val ctrl = l.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
        val clamped = percent.coerceIn(0, 100) / 100f
        ctrl.value = (ctrl.minimum + (ctrl.maximum - ctrl.minimum) * clamped)
            .coerceIn(ctrl.minimum, ctrl.maximum)
    }

    @Synchronized
    fun close() = closeInternal(drain = true)

    private fun closeInternal(drain: Boolean) {
        running.set(false)
        ring?.close()
        writer?.let { t ->
            runCatching { t.join(500) }
        }
        writer = null
        line?.let { l ->
            runCatching {
                if (drain) l.drain()
                l.stop()
                l.close()
            }
        }
        line = null
        ring = null
        format = null
    }

    /**
     * The sole owner of the [SourceDataLine]. Pulls whole sample frames from the
     * ring and writes them; pads underruns with a short silence chunk; honours a
     * pending flush by draining the line's own buffer on this thread.
     */
    private fun writeLoop(l: SourceDataLine, r: PcmRingBuffer, fmt: StreamFormat) {
        val frame = fmt.frameBytes
        // ~20 ms per write, rounded to a whole frame.
        val chunkBytes = ((fmt.bytesPerSecond / 50) / frame).coerceAtLeast(1) * frame
        val buf = ByteArray(chunkBytes)
        val silence = ByteArray(chunkBytes)
        var leftover = 0 // bytes carried over that didn't complete a frame

        while (running.get() && !r.isClosed) {
            if (flushRequested.getAndSet(false)) {
                runCatching { l.flush() }
                leftover = 0
            }
            val n = r.read(buf, leftover, chunkBytes - leftover, timeoutMillis = 20) + leftover
            if (n == 0) {
                // Underrun: keep the line fed so it never audibly starves.
                runCatching { l.write(silence, 0, minOf(chunkBytes, frame * 8)) }
                continue
            }
            val writable = n - (n % frame)
            if (writable > 0) runCatching { l.write(buf, 0, writable) }
            leftover = n - writable
            if (leftover > 0) System.arraycopy(buf, writable, buf, 0, leftover)
        }
    }
}

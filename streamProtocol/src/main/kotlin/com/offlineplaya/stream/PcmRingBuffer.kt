package com.offlineplaya.stream

/**
 * A bounded byte ring buffer that decouples the socket reader thread (producer)
 * from the audio-line writer thread (consumer) on the receiver, riding out WiFi
 * jitter with ~1 s of slack.
 *
 * - [write] blocks the reader while the buffer is full, which back-pressures
 *   TCP — the sender paces at playback rate, so in steady state this barely
 *   triggers; a WiFi burst just fills the ring and is drained in real time.
 * - [read] never blocks longer than `timeoutMillis` and returns whatever is
 *   available (possibly 0). The writer pads the shortfall with silence rather
 *   than stalling the line — a brief underrun is silence, never a crackle.
 * - [flush] drops everything buffered (seek/skip) so a transport command is
 *   heard immediately instead of a bufferful later.
 *
 * Thread-safe: exactly one producer and one consumer are expected, but all
 * state is guarded so [flush] / [close] may be called from any thread.
 */
class PcmRingBuffer(val capacity: Int) {
    init {
        require(capacity > 0) { "capacity must be positive: $capacity" }
    }

    private val buffer = ByteArray(capacity)
    private val lock = Object()

    private var head = 0 // next read
    private var tail = 0 // next write
    private var size = 0
    private var closed = false

    /** Current number of buffered bytes. */
    val available: Int
        get() = synchronized(lock) { size }

    /**
     * Appends [len] bytes from [src] starting at [off], blocking while the ring
     * is full. Returns the number of bytes written — short (even 0) only when
     * the buffer is [close]d or [flush]ed mid-write; the caller should stop.
     */
    fun write(src: ByteArray, off: Int = 0, len: Int = src.size): Int = synchronized(lock) {
        var written = 0
        while (written < len) {
            while (size == capacity && !closed) lock.wait()
            if (closed) return written
            val n = minOf(len - written, capacity - size)
            var i = 0
            while (i < n) {
                buffer[tail] = src[off + written + i]
                tail = (tail + 1) % capacity
                i++
            }
            size += n
            written += n
            lock.notifyAll()
        }
        written
    }

    /**
     * Copies up to [maxLen] bytes into [dst] at [dstOff], waiting up to
     * [timeoutMillis] for at least one byte. Returns the count copied (0 on
     * timeout / close / empty). Never blocks for the full buffer.
     */
    fun read(dst: ByteArray, dstOff: Int, maxLen: Int, timeoutMillis: Long): Int = synchronized(lock) {
        if (size == 0 && !closed && timeoutMillis > 0) lock.wait(timeoutMillis)
        if (size == 0) return 0
        val n = minOf(maxLen, size)
        var i = 0
        while (i < n) {
            dst[dstOff + i] = buffer[head]
            head = (head + 1) % capacity
            i++
        }
        size -= n
        lock.notifyAll()
        n
    }

    /** Discards all buffered bytes and wakes a blocked [write]. */
    fun flush() = synchronized(lock) {
        head = 0
        tail = 0
        size = 0
        lock.notifyAll()
    }

    /** Marks the buffer closed and wakes every waiter. Idempotent. */
    fun close() = synchronized(lock) {
        closed = true
        lock.notifyAll()
    }

    val isClosed: Boolean
        get() = synchronized(lock) { closed }
}

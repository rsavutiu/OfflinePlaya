package com.offlineplaya.android.net

import com.offlineplaya.shared.util.AppLogger
import com.offlineplaya.stream.StreamControl
import com.offlineplaya.stream.StreamFormat
import com.offlineplaya.stream.StreamFraming
import com.offlineplaya.stream.StreamProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The phone side of the stream: opens one TCP connection to a PC receiver and
 * pushes length-prefixed frames ([StreamFraming]) — `Hello`, `Format`, the live
 * `AUDIO` frames, `Flush` on seek/skip, `Heartbeat`, and `Bye` on stop.
 *
 * All socket I/O happens on a private [Dispatchers.IO] scope. Frames go through
 * a single ordered channel drained by one writer coroutine, so the socket is
 * only ever written from one place and frame order is preserved — the in-band
 * [enqueueFlush] therefore always precedes the post-seek audio that follows it.
 *
 * Audio frames are dropped on queue overflow (a WiFi stall must never stall the
 * player's audio thread); control frames block briefly to guarantee delivery.
 */
class StreamSender(private val logger: AppLogger) {

    private sealed interface OutFrame {
        data class Control(val control: StreamControl) : OutFrame
        data class Audio(val pcm: ByteArray) : OutFrame
    }

    @Volatile
    private var scope: CoroutineScope? = null

    @Volatile
    private var socket: Socket? = null

    private var channel: Channel<OutFrame>? = null

    private val connected = AtomicBoolean(false)

    @Volatile
    private var lastFormat: StreamFormat? = null

    val isConnected: Boolean get() = connected.get()

    /**
     * Opens the connection and starts the writer + heartbeat. Blocking connect
     * runs on IO. Returns `true` once `Hello` is away; `onLost` fires (once) if
     * the socket later dies, so the controller can update UI / return to local
     * output.
     */
    suspend fun connect(
        host: String,
        port: Int,
        deviceName: String,
        onLost: (String) -> Unit,
    ): Boolean {
        close() // ensure any previous connection is torn down
        return try {
            val sock = Socket()
            sock.tcpNoDelay = true
            sock.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
            val out = DataOutputStream(sock.getOutputStream().buffered())
            val ch = Channel<OutFrame>(capacity = QUEUE_CAPACITY, onBufferOverflow = BufferOverflow.SUSPEND)
            val sc = CoroutineScope(SupervisorJob() + Dispatchers.IO)

            socket = sock
            channel = ch
            scope = sc
            lastFormat = null
            connected.set(true)

            // Hello first, so the receiver shows "Playing from <phone>".
            StreamFraming.writeControl(out, StreamControl.Hello(deviceName))
            out.flush()

            sc.launch { writeLoop(out, ch, onLost) }
            sc.launch { heartbeatLoop(ch) }
            true
        } catch (e: Exception) {
            logger.w(TAG, "connect to $host:$port failed: ${e.message}")
            close()
            false
        }
    }

    private suspend fun writeLoop(out: DataOutputStream, ch: Channel<OutFrame>, onLost: (String) -> Unit) {
        try {
            for (frame in ch) {
                when (frame) {
                    is OutFrame.Control -> StreamFraming.writeControl(out, frame.control)
                    is OutFrame.Audio -> StreamFraming.writeFrame(out, StreamProtocol.TYPE_AUDIO, frame.pcm)
                }
                // Flush when the queue is momentarily empty so audio doesn't sit
                // in the buffered stream waiting for the next frame.
                if (ch.isEmpty) out.flush()
            }
        } catch (e: Exception) {
            if (connected.getAndSet(false)) {
                logger.w(TAG, "write loop ended: ${e.message}")
                onLost(e.message ?: "connection lost")
            }
        }
    }

    private suspend fun heartbeatLoop(ch: Channel<OutFrame>) {
        while (scope?.isActive == true) {
            delay(HEARTBEAT_INTERVAL_MS)
            ch.trySend(OutFrame.Control(StreamControl.Heartbeat))
        }
    }

    /** (Re)announces the PCM format; no-op if unchanged since the last send. */
    fun sendFormat(format: StreamFormat) {
        if (format == lastFormat) return
        lastFormat = format
        enqueueControlBlocking(StreamControl.Format(format))
    }

    /** Drops the receiver's buffer so a seek/skip is heard immediately. */
    fun enqueueFlush() = enqueueControlBlocking(StreamControl.Flush)

    /** Non-blocking: queue PCM, dropping it if the network is behind. */
    fun enqueuePcm(pcm: ByteArray) {
        channel?.trySend(OutFrame.Audio(pcm))
    }

    private fun enqueueControlBlocking(control: StreamControl) {
        val ch = channel ?: return
        runCatching { ch.trySendBlocking(OutFrame.Control(control)) }
    }

    /** Sends a clean goodbye (best-effort) and tears the connection down. */
    fun close() {
        if (connected.getAndSet(false)) {
            channel?.trySend(OutFrame.Control(StreamControl.Bye))
        }
        channel?.close()
        channel = null
        runCatching { socket?.close() }
        socket = null
        scope?.cancel()
        scope = null
        lastFormat = null
    }

    private companion object {
        const val TAG = "StreamSender"
        const val CONNECT_TIMEOUT_MS = 4000
        const val HEARTBEAT_INTERVAL_MS = 2000L
        // ~enough frames for a couple of seconds of audio chunks before we drop.
        const val QUEUE_CAPACITY = 256
    }
}

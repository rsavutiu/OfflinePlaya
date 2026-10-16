package com.offlineplaya.desktop.net

import com.offlineplaya.desktop.audio.PcmPlayer
import com.offlineplaya.stream.StreamControl
import com.offlineplaya.stream.StreamFormat
import com.offlineplaya.stream.StreamFraming
import com.offlineplaya.stream.StreamProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.net.ServerSocket
import java.net.Socket
import kotlin.coroutines.coroutineContext

/**
 * Accepts one sender connection at a time and plays its live PCM through a
 * [PcmPlayer]. Both `accept()` and the frame reads block, so [start] must run
 * on [Dispatchers.IO] — never the Compose UI thread, or the window freezes.
 *
 * Skeleton: no pairing/PIN check, no heartbeat timeout, no ring-buffer tuning
 * yet (see docs/plans/03-stream-to-pc.md). It is enough to accept a connection
 * and render frames.
 */
class StreamServer(private val port: Int = StreamProtocol.DEFAULT_PORT) {

    @Volatile
    private var serverSocket: ServerSocket? = null

    suspend fun start(
        onStatus: (String) -> Unit,
        onFormat: (StreamFormat?) -> Unit = {},
    ) = withContext(Dispatchers.IO) {
        val server = ServerSocket(port).also { serverSocket = it }
        onStatus("Listening on port $port")
        val scope = CoroutineScope(coroutineContext)
        while (coroutineContext.isActive) {
            val socket = try {
                server.accept()
            } catch (e: Exception) {
                if (coroutineContext.isActive) onStatus("Stopped: ${e.message}")
                break
            }
            onStatus("Connected: ${socket.inetAddress.hostAddress}")
            scope.launch(Dispatchers.IO) { handle(socket, onStatus, onFormat) }
        }
    }

    private suspend fun handle(
        socket: Socket,
        onStatus: (String) -> Unit,
        onFormat: (StreamFormat?) -> Unit,
    ) {
        val player = PcmPlayer()
        try {
            // Liveness: the sender heartbeats every ~2 s, so a read that stalls
            // this long means the WiFi (or the phone) is gone — bail instead of
            // blocking on a dead socket until TCP keepalive notices (minutes).
            socket.soTimeout = HEARTBEAT_TIMEOUT_MS
            val input = DataInputStream(socket.getInputStream().buffered())
            while (!socket.isClosed) {
                coroutineContext.ensureActive()
                val frame = StreamFraming.readFrame(input)
                when (frame.type) {
                    StreamProtocol.TYPE_CONTROL ->
                        onControl(StreamProtocol.decodeControl(frame.payload), player, onStatus, onFormat)
                    StreamProtocol.TYPE_AUDIO -> player.write(frame.payload)
                    else -> { /* unknown frame type — skip */ }
                }
            }
        } catch (e: Exception) {
            onStatus("Disconnected: ${e.message}")
        } finally {
            player.close()
            onFormat(null)
            runCatching { socket.close() }
            // The accept loop is still running, so we're ready for the next
            // phone — return the UI to "waiting" rather than leaving it stuck
            // on the disconnect message.
            if (coroutineContext.isActive) onStatus("Listening on port $port")
        }
    }

    private fun onControl(
        control: StreamControl,
        player: PcmPlayer,
        onStatus: (String) -> Unit,
        onFormat: (StreamFormat?) -> Unit,
    ) {
        when (control) {
            is StreamControl.Hello -> onStatus("Playing from ${control.name}")
            is StreamControl.Format -> {
                player.open(control.format)
                onFormat(control.format)
            }
            is StreamControl.Volume -> player.setVolume(control.percent)
            StreamControl.Flush -> player.flush()
            StreamControl.Heartbeat -> Unit
            StreamControl.Bye -> onStatus("Sender disconnected")
        }
    }

    fun stop() {
        runCatching { serverSocket?.close() }
        serverSocket = null
    }

    private companion object {
        /** A few missed ~2 s heartbeats → treat the socket as dead. */
        const val HEARTBEAT_TIMEOUT_MS = 8000
    }
}

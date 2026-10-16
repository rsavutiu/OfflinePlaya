package com.offlineplaya.desktop.net

import com.offlineplaya.desktop.audio.PcmPlayer
import com.offlineplaya.stream.StreamControl
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

    suspend fun start(onStatus: (String) -> Unit) = withContext(Dispatchers.IO) {
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
            scope.launch(Dispatchers.IO) { handle(socket, onStatus) }
        }
    }

    private suspend fun handle(socket: Socket, onStatus: (String) -> Unit) {
        val player = PcmPlayer()
        try {
            val input = DataInputStream(socket.getInputStream().buffered())
            while (!socket.isClosed) {
                coroutineContext.ensureActive()
                val type = input.readByte()
                val length = input.readInt()
                val payload = ByteArray(length)
                input.readFully(payload)
                when (type) {
                    StreamProtocol.TYPE_CONTROL ->
                        onControl(StreamProtocol.decodeControl(payload), player, onStatus)
                    StreamProtocol.TYPE_AUDIO -> player.write(payload)
                    else -> { /* unknown frame type — skip */ }
                }
            }
        } catch (e: Exception) {
            onStatus("Disconnected: ${e.message}")
        } finally {
            player.close()
            runCatching { socket.close() }
        }
    }

    private fun onControl(
        control: StreamControl,
        player: PcmPlayer,
        onStatus: (String) -> Unit,
    ) {
        when (control) {
            is StreamControl.Hello -> onStatus("Playing from ${control.name}")
            is StreamControl.Format -> player.open(control.format)
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
}

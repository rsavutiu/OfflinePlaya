package com.offlineplaya.stream

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException

/**
 * The single, shared implementation of the length-prefixed frame on the wire,
 * so the phone sender and the PC receiver can never drift apart on it. See
 * [StreamProtocol] for the layout:
 *
 * ```
 * [ 1 byte type ][ 4 bytes big-endian length ][ payload ]
 * ```
 *
 * Both sides do blocking socket I/O off the main thread (Dispatchers.IO on the
 * phone, a reader thread on the PC).
 */
object StreamFraming {

    /**
     * Hard cap on a single frame's payload, as a guard against a corrupt or
     * hostile length word making us allocate gigabytes. Audio frames are a few
     * KB; control frames are tiny. 8 MB is comfortably above a large audio
     * chunk at 24/192 and still bounded.
     */
    const val MAX_FRAME_BYTES = 8 * 1024 * 1024

    /** A decoded frame: its [type] byte and raw [payload]. */
    data class Frame(val type: Byte, val payload: ByteArray) {
        override fun equals(other: Any?): Boolean =
            this === other ||
                (other is Frame && type == other.type && payload.contentEquals(other.payload))

        override fun hashCode(): Int = 31 * type + payload.contentHashCode()
    }

    /**
     * Writes one frame. The caller is responsible for flushing / synchronising
     * the stream; [writeControl] and the audio path serialise their writes so
     * frames never interleave on the socket.
     */
    fun writeFrame(out: DataOutputStream, type: Byte, payload: ByteArray, length: Int = payload.size) {
        require(length in 0..MAX_FRAME_BYTES) { "frame length out of range: $length" }
        out.writeByte(type.toInt())
        out.writeInt(length)
        out.write(payload, 0, length)
    }

    fun writeControl(out: DataOutputStream, control: StreamControl) {
        writeFrame(out, StreamProtocol.TYPE_CONTROL, StreamProtocol.encodeControl(control))
    }

    /**
     * Reads exactly one frame, blocking until it arrives. Throws [EOFException]
     * on a clean close and [IOException] on a malformed length word.
     */
    fun readFrame(input: DataInputStream): Frame {
        val type = input.readByte()
        val length = input.readInt()
        if (length < 0 || length > MAX_FRAME_BYTES) {
            throw IOException("frame length out of range: $length")
        }
        val payload = ByteArray(length)
        input.readFully(payload)
        return Frame(type, payload)
    }
}

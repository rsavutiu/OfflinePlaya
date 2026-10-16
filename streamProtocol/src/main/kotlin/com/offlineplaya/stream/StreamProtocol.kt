package com.offlineplaya.stream

import kotlinx.serialization.json.Json

/**
 * The OfflinePlaya stream wire format, shared verbatim by sender (phone) and
 * receiver (PC).
 *
 * One TCP connection carries length-prefixed frames:
 *
 * ```
 * [ 1 byte type ][ 4 bytes big-endian length ][ payload ]
 *   TYPE_CONTROL (0x01)  payload = UTF-8 JSON of a StreamControl
 *   TYPE_AUDIO   (0x02)  payload = raw interleaved PCM for the current format
 * ```
 */
object StreamProtocol {
    const val VERSION = 1

    /** DNS-SD service type the receiver advertises and the phone browses for. */
    const val SERVICE_TYPE = "_offlineplaya._tcp"

    /** Default TCP port (override for odd networks / multiple receivers). */
    const val DEFAULT_PORT = 45117

    const val TYPE_CONTROL: Byte = 0x01
    const val TYPE_AUDIO: Byte = 0x02

    private val json = Json {
        classDiscriminator = "t"
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encodeControl(control: StreamControl): ByteArray =
        json.encodeToString(StreamControl.serializer(), control).encodeToByteArray()

    fun decodeControl(payload: ByteArray): StreamControl =
        json.decodeFromString(StreamControl.serializer(), payload.decodeToString())
}

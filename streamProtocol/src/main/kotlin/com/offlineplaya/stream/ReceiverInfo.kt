package com.offlineplaya.stream

import kotlinx.serialization.Serializable

/** A discovered (or manually entered) PC receiver on the LAN. */
@Serializable
data class ReceiverInfo(
    val name: String,
    val host: String,
    val port: Int = StreamProtocol.DEFAULT_PORT,
)

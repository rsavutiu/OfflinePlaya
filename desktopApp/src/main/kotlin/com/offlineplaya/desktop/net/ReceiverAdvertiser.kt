package com.offlineplaya.desktop.net

import com.offlineplaya.stream.StreamProtocol

/**
 * Advertises this receiver on the LAN so the phone can discover it.
 *
 * TODO(stream-to-pc): advertise [StreamProtocol.SERVICE_TYPE] via jmdns
 * (`org.jmdns:jmdns`, pure-Java, cross-platform) once the dependency version is
 * pinned. Until then this is a no-op and the phone connects by manual IP:port
 * on [StreamProtocol.DEFAULT_PORT]. See docs/plans/03-stream-to-pc.md.
 */
class ReceiverAdvertiser(
    private val name: String,
    private val port: Int = StreamProtocol.DEFAULT_PORT,
) {
    fun start() {
        // no-op until jmdns is wired
    }

    fun stop() {
        // no-op
    }
}

package com.offlineplaya.desktop.net

import com.offlineplaya.stream.StreamProtocol
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo

/**
 * Advertises this receiver on the LAN via mDNS / DNS-SD so the phone's
 * `NsdManager` can discover it without the user typing an IP.
 *
 * jmdns is pure Java, so this runs identically on Windows, macOS and Linux.
 * The service type is [StreamProtocol.SERVICE_TYPE] with the `.local.` suffix
 * DNS-SD requires.
 *
 * On a machine with WSL / Hyper-V / VPN adapters, binding jmdns to the default
 * address often lands on a virtual NIC the phone can't see, so we pick a real
 * site-local IPv4 interface explicitly.
 */
class ReceiverAdvertiser(
    private val name: String,
    private val port: Int = StreamProtocol.DEFAULT_PORT,
) {
    @Volatile
    private var jmdns: JmDNS? = null

    fun start() {
        if (jmdns != null) return
        runCatching {
            val address = siteLocalIpv4() ?: InetAddress.getLocalHost()
            val instance = JmDNS.create(address, name)
            val info = ServiceInfo.create(
                /* type = */ "${StreamProtocol.SERVICE_TYPE}.local.",
                /* name = */ name,
                /* port = */ port,
                /* text = */ "OfflinePlaya receiver",
            )
            instance.registerService(info)
            jmdns = instance
        }
        // A failure here just means no auto-discovery — the phone can still
        // connect by manual IP:port. Never crash the receiver over mDNS.
    }

    fun stop() {
        val instance = jmdns ?: return
        jmdns = null
        runCatching {
            instance.unregisterAllServices()
            instance.close()
        }
    }

    private fun siteLocalIpv4(): InetAddress? =
        runCatching {
            NetworkInterface.getNetworkInterfaces().asSequence()
                .filter { it.isUp && !it.isLoopback && !it.isVirtual }
                .flatMap { it.inetAddresses.asSequence() }
                .firstOrNull { it is Inet4Address && it.isSiteLocalAddress }
        }.getOrNull()
}

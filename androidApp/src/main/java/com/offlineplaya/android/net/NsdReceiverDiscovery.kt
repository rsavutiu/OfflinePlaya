package com.offlineplaya.android.net

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.offlineplaya.shared.domain.stream.PcReceiver
import com.offlineplaya.shared.util.AppLogger
import com.offlineplaya.stream.StreamProtocol
import java.util.ArrayDeque

/**
 * Browses the LAN for OfflinePlaya receivers via `NsdManager` (DNS-SD), the
 * Android counterpart of the desktop's jmdns advertiser. Discovery runs only
 * while the picker is open — an offline app must not hold the network open in
 * the background.
 *
 * Resolves are serialised: before API 34 two concurrent `resolveService` calls
 * fail with `FAILURE_ALREADY_ACTIVE`, so pending services queue and resolve one
 * at a time. `NsdManager` delivers callbacks on a binder thread; the resolved
 * set is guarded and pushed to [onChanged] as whole-list snapshots.
 */
class NsdReceiverDiscovery(
    context: Context,
    private val logger: AppLogger,
) {
    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    // DNS-SD service type. NsdManager wants a trailing dot.
    private val serviceType = "${StreamProtocol.SERVICE_TYPE}."

    private val lock = Any()
    private val resolved = LinkedHashMap<String, PcReceiver>()
    private val pending = ArrayDeque<NsdServiceInfo>()
    private var resolving = false

    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var onChanged: ((List<PcReceiver>) -> Unit)? = null

    fun start(onChanged: (List<PcReceiver>) -> Unit) {
        if (discoveryListener != null) return
        this.onChanged = onChanged
        synchronized(lock) {
            resolved.clear()
            pending.clear()
            resolving = false
        }
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {}
            override fun onDiscoveryStopped(serviceType: String) {}

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                logger.w(TAG, "startDiscovery failed: $errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                logger.w(TAG, "stopDiscovery failed: $errorCode")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                synchronized(lock) {
                    pending.add(serviceInfo)
                    pump()
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                val changed = synchronized(lock) { resolved.remove(serviceInfo.serviceName) != null }
                if (changed) emit()
            }
        }
        discoveryListener = listener
        runCatching { nsd.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener) }
            .onFailure { logger.w(TAG, "discoverServices threw: ${it.message}") }
    }

    fun stop() {
        discoveryListener?.let { l -> runCatching { nsd.stopServiceDiscovery(l) } }
        discoveryListener = null
        onChanged = null
        synchronized(lock) {
            resolved.clear()
            pending.clear()
            resolving = false
        }
    }

    /** Must hold [lock]. Kicks off the next resolve if idle. */
    private fun pump() {
        if (resolving) return
        val next = pending.poll() ?: return
        resolving = true
        runCatching {
            nsd.resolveService(next, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    logger.w(TAG, "resolve failed for ${serviceInfo.serviceName}: $errorCode")
                    synchronized(lock) { resolving = false; pump() }
                }

                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    val host = serviceInfo.host?.hostAddress
                    if (host != null) {
                        val receiver = PcReceiver(
                            name = serviceInfo.serviceName,
                            host = host,
                            port = serviceInfo.port,
                        )
                        synchronized(lock) { resolved[serviceInfo.serviceName] = receiver }
                        emit()
                    }
                    synchronized(lock) { resolving = false; pump() }
                }
            })
        }.onFailure {
            logger.w(TAG, "resolveService threw: ${it.message}")
            synchronized(lock) { resolving = false; pump() }
        }
    }

    private fun emit() {
        val snapshot = synchronized(lock) { resolved.values.toList() }
        onChanged?.invoke(snapshot)
    }

    private companion object {
        const val TAG = "NsdDiscovery"
    }
}

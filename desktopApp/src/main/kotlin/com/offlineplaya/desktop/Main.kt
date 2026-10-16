package com.offlineplaya.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.offlineplaya.desktop.net.ReceiverAdvertiser
import com.offlineplaya.desktop.net.StreamServer
import com.offlineplaya.stream.StreamFormat
import com.offlineplaya.stream.StreamProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import androidx.compose.ui.unit.DpSize

// Walkman-orange brand accent, shared with the phone app.
private val BrandOrange = Color(0xFFF47B20)
private val AppBackground = Color(0xFF111114)
private val CardBackground = Color(0xFF1C1C22)
private val TextPrimary = Color(0xFFECECEF)
private val TextMuted = Color(0xFF9A9AA4)
private val ReadyGreen = Color(0xFF4CAF50)
private val ErrorRed = Color(0xFFE5534B)

fun main() = application {
    val state = rememberWindowState(size = DpSize(460.dp, 560.dp))
    Window(onCloseRequest = ::exitApplication, state = state, title = "OfflinePlaya Receiver") {
        ReceiverApp()
    }
}

@Composable
private fun ReceiverApp() {
    val scope = rememberCoroutineScope()
    val receiverName = remember { defaultReceiverName() }
    val localIp = remember { localIpv4() }
    val server = remember { StreamServer(StreamProtocol.DEFAULT_PORT) }
    val advertiser = remember { ReceiverAdvertiser(receiverName, StreamProtocol.DEFAULT_PORT) }
    var status by remember { mutableStateOf("Starting…") }
    var format by remember { mutableStateOf<StreamFormat?>(null) }

    DisposableEffect(Unit) {
        advertiser.start()
        val job = scope.launch(Dispatchers.IO) {
            runCatching {
                server.start(
                    onStatus = { line -> scope.launch { withContext(Dispatchers.Main) { status = line } } },
                    onFormat = { f -> scope.launch { withContext(Dispatchers.Main) { format = f } } },
                )
            }.onFailure { status = "Error: ${it.message}" }
        }
        onDispose {
            job.cancel()
            server.stop()
            advertiser.stop()
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = BrandOrange, background = AppBackground)) {
        Surface(modifier = Modifier.fillMaxSize(), color = AppBackground) {
            Column(
                modifier = Modifier.fillMaxSize().padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Header()
                StatusCard(receiverName = receiverName, status = status)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    InfoTile("ADDRESS", "$localIp:${StreamProtocol.DEFAULT_PORT}", Modifier.weight(1f))
                    InfoTile("FORMAT", format?.describe() ?: "—", Modifier.weight(1f), accent = format != null)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "Discoverable on your Wi-Fi — pick this receiver in OfflinePlaya on your " +
                        "phone (Settings → Audio → Play to PC), or connect by the address above. " +
                        "Audio plays live through this PC and is never saved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun Header() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(28.dp).clip(RoundedCornerShape(7.dp)).background(BrandOrange),
            contentAlignment = Alignment.Center,
        ) {
            Text("▶", color = Color.Black, fontSize = 15.sp)
        }
        Text(
            "OfflinePlaya Receiver",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
        )
    }
}

@Composable
private fun StatusCard(receiverName: String, status: String) {
    // Map the server's status line to a prominent state label + detail.
    val connected = status.startsWith("Playing from") || status.startsWith("Connected")
    val ready = status.startsWith("Listening")
    val disconnected = status.startsWith("Sender disconnected") || status.startsWith("Disconnected")
    val failed = status.startsWith("Error") || status.startsWith("Stopped")
    val (label, dot) = when {
        status.startsWith("Playing from") -> "CONNECTED" to BrandOrange
        connected -> "CONNECTED" to BrandOrange
        ready -> "WAITING FOR A PHONE" to ReadyGreen
        disconnected -> "DISCONNECTED" to TextMuted
        failed -> "ERROR" to ErrorRed
        else -> "STARTING…" to TextMuted
    }
    val detail = when {
        status.startsWith("Playing from") -> "Playing from ${status.removePrefix("Playing from ").trim()}"
        status.startsWith("Connected") -> status.removePrefix("Connected: ").trim()
        ready -> "Ready on port ${StreamProtocol.DEFAULT_PORT}"
        else -> status
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(receiverName, style = MaterialTheme.typography.titleMedium, color = TextMuted)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(dot))
                Text(
                    label,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (dot == TextMuted) TextPrimary else dot,
                )
            }
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        }
    }
}

@Composable
private fun InfoTile(label: String, value: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (accent) BrandOrange else TextPrimary,
            )
        }
    }
}

/** e.g. "44.1 kHz · 16-bit · stereo" — what's actually on the wire right now. */
private fun StreamFormat.describe(): String {
    val khz = (sampleRate / 1000.0)
    val khzStr = if (khz == khz.toInt().toDouble()) "${khz.toInt()}" else "%.1f".format(khz)
    val ch = when (channels) {
        1 -> "mono"
        2 -> "stereo"
        else -> "${channels}ch"
    }
    return "$khzStr kHz · $bitsPerSample-bit · $ch"
}

private fun defaultReceiverName(): String =
    runCatching { InetAddress.getLocalHost().hostName }.getOrDefault("OfflinePlaya PC")

private fun localIpv4(): String =
    runCatching {
        NetworkInterface.getNetworkInterfaces().asSequence()
            .filter { it.isUp && !it.isLoopback && !it.isVirtual }
            .flatMap { it.inetAddresses.asSequence() }
            .firstOrNull { it is Inet4Address && it.isSiteLocalAddress }
            ?.hostAddress
    }.getOrNull() ?: "127.0.0.1"

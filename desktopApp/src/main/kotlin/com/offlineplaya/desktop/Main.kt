package com.offlineplaya.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.offlineplaya.desktop.net.ReceiverAdvertiser
import com.offlineplaya.desktop.net.StreamServer
import com.offlineplaya.stream.StreamProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "OfflinePlaya Receiver") {
        ReceiverApp()
    }
}

@Composable
private fun ReceiverApp() {
    val scope = rememberCoroutineScope()
    val receiverName = remember { defaultReceiverName() }
    val pin = remember { generatePin() }
    val server = remember { StreamServer(StreamProtocol.DEFAULT_PORT) }
    val advertiser = remember { ReceiverAdvertiser(receiverName, StreamProtocol.DEFAULT_PORT) }
    var status by remember { mutableStateOf("Starting…") }

    DisposableEffect(Unit) {
        advertiser.start()
        val job = scope.launch(Dispatchers.IO) {
            runCatching {
                server.start { line -> scope.launch { withContext(Dispatchers.Main) { status = line } } }
            }.onFailure { status = "Error: ${it.message}" }
        }
        onDispose {
            job.cancel()
            server.stop()
            advertiser.stop()
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("OfflinePlaya Receiver", style = MaterialTheme.typography.headlineSmall)
                Text("Name: $receiverName")
                Text("Port: ${StreamProtocol.DEFAULT_PORT}")
                Text("Pairing PIN: $pin", style = MaterialTheme.typography.titleMedium)
                Text("Status: $status", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Skeleton build — accepts a connection and plays live PCM. " +
                        "Discovery (mDNS) and PIN enforcement are not wired yet; " +
                        "connect by IP:port for now.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun defaultReceiverName(): String =
    runCatching { InetAddress.getLocalHost().hostName }.getOrDefault("OfflinePlaya PC")

private fun generatePin(): String = (100_000..999_999).random().toString()

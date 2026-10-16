package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.stream.PcReceiver
import com.offlineplaya.shared.domain.stream.PlayToPcError
import com.offlineplaya.shared.domain.stream.PlayToPcState
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.AppSpacing
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.play_to_pc_blurb
import offlineplaya.shared.generated.resources.play_to_pc_connect
import offlineplaya.shared.generated.resources.play_to_pc_connecting_to
import offlineplaya.shared.generated.resources.play_to_pc_disconnect
import offlineplaya.shared.generated.resources.play_to_pc_error_connect_failed
import offlineplaya.shared.generated.resources.play_to_pc_error_connection_lost
import offlineplaya.shared.generated.resources.play_to_pc_manual_header
import offlineplaya.shared.generated.resources.play_to_pc_manual_host
import offlineplaya.shared.generated.resources.play_to_pc_manual_port
import offlineplaya.shared.generated.resources.play_to_pc_none_found
import offlineplaya.shared.generated.resources.play_to_pc_searching
import offlineplaya.shared.generated.resources.play_to_pc_streaming_to
import offlineplaya.shared.generated.resources.play_to_pc_title
import org.jetbrains.compose.resources.stringResource

/**
 * The "Play to PC" picker: browse the LAN for receivers, connect to one (or a
 * hand-entered IP:port), and disconnect. Deliberately thin — the app stays
 * offline-only until the user opens this and connects.
 *
 * Takes state + lambdas (no DI) so it stays `@PreviewScreenSizes`-able; the
 * controller is injected and discovery is started/stopped in App.kt.
 */
@Composable
fun PlayToPcPage(
    state: PlayToPcState,
    onConnect: (PcReceiver) -> Unit,
    onConnectManual: (host: String, port: Int) -> Unit,
    onDisconnect: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { AppTopBar(title = stringResource(Res.string.play_to_pc_title), onBack = onBack) },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = AppSpacing.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            Text(
                text = stringResource(Res.string.play_to_pc_blurb),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            state.error?.let { err ->
                Text(
                    text = err.message(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            val connected = state.connected
            if (connected != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(AppSpacing.lg)) {
                        Text(
                            text = stringResource(Res.string.play_to_pc_streaming_to, connected.name),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "${connected.host}:${connected.port}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = onDisconnect,
                            modifier = Modifier.padding(top = AppSpacing.md),
                        ) { Text(stringResource(Res.string.play_to_pc_disconnect)) }
                    }
                }
            } else {
                DiscoverySection(state = state, onConnect = onConnect)
                ManualConnectSection(onConnectManual = onConnectManual)
            }
        }
    }
}

@Composable
private fun DiscoverySection(state: PlayToPcState, onConnect: (PcReceiver) -> Unit) {
    if (state.receivers.isEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
            if (state.discovering) CircularProgressIndicator(modifier = Modifier.padding(end = AppSpacing.xs))
            Text(
                text = stringResource(
                    if (state.discovering) Res.string.play_to_pc_searching
                    else Res.string.play_to_pc_none_found,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    } else {
        state.receivers.forEach { receiver ->
            val connecting = state.connecting?.key == receiver.key
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(AppSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(receiver.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${receiver.host}:${receiver.port}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (connecting) {
                        Text(
                            stringResource(Res.string.play_to_pc_connecting_to, receiver.name),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    } else {
                        OutlinedButton(onClick = { onConnect(receiver) }) {
                            Text(stringResource(Res.string.play_to_pc_connect))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualConnectSection(onConnectManual: (host: String, port: Int) -> Unit) {
    var host by rememberSaveable { mutableStateOf("") }
    var port by rememberSaveable { mutableStateOf("45117") }
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        Text(
            stringResource(Res.string.play_to_pc_manual_header),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = AppSpacing.md),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
            OutlinedTextField(
                value = host,
                onValueChange = { host = it.trim() },
                label = { Text(stringResource(Res.string.play_to_pc_manual_host)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.weight(2f),
            )
            OutlinedTextField(
                value = port,
                onValueChange = { port = it.filter(Char::isDigit) },
                label = { Text(stringResource(Res.string.play_to_pc_manual_port)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }
        Button(
            onClick = { onConnectManual(host, port.toIntOrNull() ?: 45117) },
            enabled = host.isNotBlank() && port.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(Res.string.play_to_pc_connect)) }
    }
}

@Composable
private fun PlayToPcError.message(): String = when (this) {
    is PlayToPcError.ConnectFailed -> stringResource(Res.string.play_to_pc_error_connect_failed, "$host:$port")
    PlayToPcError.ConnectionLost -> stringResource(Res.string.play_to_pc_error_connection_lost)
}

@PreviewScreenSizes
@Composable
private fun PlayToPcPageDiscoveringPreview() {
    PreviewTheme {
        PlayToPcPage(
            state = PlayToPcState(
                discovering = true,
                receivers = listOf(
                    PcReceiver("Living Room PC", "192.168.1.20", 45117),
                    PcReceiver("Studio", "192.168.1.31", 45117),
                ),
            ),
            onConnect = {}, onConnectManual = { _, _ -> }, onDisconnect = {}, onBack = {},
        )
    }
}

@PreviewScreenSizes
@Composable
private fun PlayToPcPageConnectedPreview() {
    PreviewTheme {
        PlayToPcPage(
            state = PlayToPcState(connected = PcReceiver("Living Room PC", "192.168.1.20", 45117)),
            onConnect = {}, onConnectManual = { _, _ -> }, onDisconnect = {}, onBack = {},
        )
    }
}

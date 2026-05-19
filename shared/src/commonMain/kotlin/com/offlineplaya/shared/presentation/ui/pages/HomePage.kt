package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.usecase.SyncReport
import com.offlineplaya.shared.presentation.sync.SyncStatus
import com.offlineplaya.shared.presentation.ui.atoms.AppButton
import com.offlineplaya.shared.presentation.ui.atoms.AppHeadline
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.molecules.SyncStatusLine
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * Home page: title bar with a Settings affordance, centered call-to-action to
 * pick a music folder, and the live scan status. The picker itself is a
 * platform concern — the page takes [onPickFolder] and [onOpenSettings]
 * callbacks; the host wires SAF (Android) etc.
 */
@Composable
fun HomePage(
    status: SyncStatus,
    onPickFolder: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scanning = status is SyncStatus.Scanning
    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = "OfflinePlaya",
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AppHeadline(text = "OfflinePlaya")
                SyncStatusLine(status = status)
                AppButton(
                    text = if (scanning) "Scanning…" else "Pick music folder",
                    onClick = onPickFolder,
                    enabled = !scanning,
                )
            }
        }
    }
}

@Preview
@Composable
private fun HomePageIdlePreview() {
    PreviewTheme {
        HomePage(status = SyncStatus.Idle, onPickFolder = {}, onOpenSettings = {})
    }
}

@Preview
@Composable
private fun HomePageScanningPreview() {
    PreviewTheme {
        HomePage(
            status = SyncStatus.Scanning("content://tree/root"),
            onPickFolder = {},
            onOpenSettings = {},
        )
    }
}

@Preview
@Composable
private fun HomePageCompletedPreview() {
    PreviewTheme {
        HomePage(
            status = SyncStatus.Completed(
                SyncReport(foldersUpserted = 6, tracksDiscovered = 87, tracksScanned = 87, tracksFailed = 0),
            ),
            onPickFolder = {},
            onOpenSettings = {},
        )
    }
}

@Preview
@Composable
private fun HomePageFailedPreview() {
    PreviewTheme {
        HomePage(
            status = SyncStatus.Failed("permission denied"),
            onPickFolder = {},
            onOpenSettings = {},
        )
    }
}

@Preview
@Composable
private fun HomePageCompletedDarkPreview() {
    PreviewTheme(darkTheme = true) {
        HomePage(
            status = SyncStatus.Completed(
                SyncReport(foldersUpserted = 6, tracksDiscovered = 87, tracksScanned = 85, tracksFailed = 2),
            ),
            onPickFolder = {},
            onOpenSettings = {},
        )
    }
}

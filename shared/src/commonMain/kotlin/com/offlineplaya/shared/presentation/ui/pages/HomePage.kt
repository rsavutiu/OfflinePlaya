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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
 * Home page: title bar with a Settings affordance, centered actions to pick a
 * music folder and (once anything is scanned) open the library, and a live
 * scan status caption. Pure presentation — every action is a callback.
 */
@Composable
fun HomePage(
    status: SyncStatus,
    trackCount: Long,
    onPickFolder: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scanning = status is SyncStatus.Scanning
    Scaffold(
        modifier = modifier,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppHeadline(text = "OfflinePlaya")
                SyncStatusLine(status = status)

                AppButton(
                    text = if (scanning) "Scanning…" else "Pick music folder",
                    onClick = onPickFolder,
                    enabled = !scanning,
                )

                if (trackCount > 0) {
                    OutlinedButton(
                        onClick = onOpenLibrary,
                        enabled = !scanning,
                    ) {
                        Text("Open library ($trackCount tracks)")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun HomePageIdlePreview() {
    PreviewTheme {
        HomePage(
            status = SyncStatus.Idle,
            trackCount = 0,
            onPickFolder = {}, onOpenLibrary = {}, onOpenSettings = {},
        )
    }
}

@Preview
@Composable
private fun HomePageScanningPreview() {
    PreviewTheme {
        HomePage(
            status = SyncStatus.Scanning("content://tree/root"),
            trackCount = 0,
            onPickFolder = {}, onOpenLibrary = {}, onOpenSettings = {},
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
            trackCount = 87,
            onPickFolder = {}, onOpenLibrary = {}, onOpenSettings = {},
        )
    }
}

@Preview
@Composable
private fun HomePageFailedPreview() {
    PreviewTheme {
        HomePage(
            status = SyncStatus.Failed("permission denied"),
            trackCount = 142,
            onPickFolder = {}, onOpenLibrary = {}, onOpenSettings = {},
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
            trackCount = 85,
            onPickFolder = {}, onOpenLibrary = {}, onOpenSettings = {},
        )
    }
}

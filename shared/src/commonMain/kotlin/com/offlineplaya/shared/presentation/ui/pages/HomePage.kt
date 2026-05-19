package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.usecase.SyncReport
import com.offlineplaya.shared.presentation.sync.SyncStatus
import com.offlineplaya.shared.presentation.ui.atoms.AppButton
import com.offlineplaya.shared.presentation.ui.atoms.AppHeadline
import com.offlineplaya.shared.presentation.ui.molecules.SyncStatusLine
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.templates.CenteredScaffoldTemplate
import com.offlineplaya.shared.presentation.ui.theme.OfflinePlayaTheme

/**
 * Phase-2 home page: lets the user pick a music folder and shows the current
 * scan status. The picker itself is a platform concern — the page takes an
 * [onPickFolder] callback and the host wires SAF (Android) / file dialog
 * (Desktop) / etc.
 */
@Composable
fun HomePage(
    status: SyncStatus,
    onPickFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scanning = status is SyncStatus.Scanning
    CenteredScaffoldTemplate(modifier = modifier) {
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

@Preview
@Composable
private fun HomePageIdlePreview() {
    OfflinePlayaTheme {
        HomePage(status = SyncStatus.Idle, onPickFolder = {})
    }
}

@Preview
@Composable
private fun HomePageScanningPreview() {
    OfflinePlayaTheme {
        HomePage(status = SyncStatus.Scanning("content://tree/root"), onPickFolder = {})
    }
}

@Preview
@Composable
private fun HomePageCompletedPreview() {
    OfflinePlayaTheme {
        HomePage(
            status = SyncStatus.Completed(
                SyncReport(foldersUpserted = 6, tracksDiscovered = 87, tracksScanned = 87, tracksFailed = 0),
            ),
            onPickFolder = {},
        )
    }
}

@Preview
@Composable
private fun HomePageFailedPreview() {
    OfflinePlayaTheme {
        HomePage(
            status = SyncStatus.Failed("permission denied"),
            onPickFolder = {},
        )
    }
}

@Preview
@Composable
private fun HomePageCompletedDarkPreview() {
    OfflinePlayaTheme(darkTheme = true) {
        HomePage(
            status = SyncStatus.Completed(
                SyncReport(foldersUpserted = 6, tracksDiscovered = 87, tracksScanned = 85, tracksFailed = 2),
            ),
            onPickFolder = {},
        )
    }
}

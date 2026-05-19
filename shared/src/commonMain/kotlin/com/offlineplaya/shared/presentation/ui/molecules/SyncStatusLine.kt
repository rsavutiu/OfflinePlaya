package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.offlineplaya.shared.domain.usecase.SyncReport
import com.offlineplaya.shared.presentation.sync.SyncStatus
import com.offlineplaya.shared.presentation.ui.atoms.AppCaption
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.OfflinePlayaTheme

/**
 * Single-line status caption mirroring the current [SyncStatus]. Renders the
 * counts on completion, the URI being scanned, or the error message.
 *
 * Pure presentation — derives its text from the sealed type, no DI lookups.
 */
@Composable
fun SyncStatusLine(
    status: SyncStatus,
    modifier: Modifier = Modifier,
) {
    AppCaption(text = status.describe(), modifier = modifier)
}

private fun SyncStatus.describe(): String = when (this) {
    SyncStatus.Idle ->
        "No folders added yet. Pick one to start."
    is SyncStatus.Scanning ->
        "Scanning…"
    is SyncStatus.Completed -> with(report) {
        "Scanned $tracksScanned tracks across $foldersUpserted folders" +
            if (tracksFailed > 0) " ($tracksFailed failed)" else ""
    }
    is SyncStatus.Failed ->
        "Scan failed: $message"
}

@Preview
@Composable
private fun SyncStatusLineIdlePreview() {
    OfflinePlayaTheme {
        Surface { SyncStatusLine(SyncStatus.Idle) }
    }
}

@Preview
@Composable
private fun SyncStatusLineScanningPreview() {
    OfflinePlayaTheme {
        Surface { SyncStatusLine(SyncStatus.Scanning("content://tree/root")) }
    }
}

@Preview
@Composable
private fun SyncStatusLineCompletedPreview() {
    OfflinePlayaTheme {
        Surface {
            SyncStatusLine(
                SyncStatus.Completed(
                    SyncReport(foldersUpserted = 12, tracksDiscovered = 184, tracksScanned = 180, tracksFailed = 4),
                ),
            )
        }
    }
}

@Preview
@Composable
private fun SyncStatusLineFailedPreview() {
    OfflinePlayaTheme {
        Surface {
            SyncStatusLine(SyncStatus.Failed("permission denied"))
        }
    }
}

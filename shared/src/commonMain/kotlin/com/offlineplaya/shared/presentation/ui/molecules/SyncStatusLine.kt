package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.offlineplaya.shared.domain.usecase.SyncReport
import com.offlineplaya.shared.presentation.sync.SyncStatus
import com.offlineplaya.shared.presentation.ui.atoms.AppCaption
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.OfflinePlayaTheme
import org.jetbrains.compose.resources.stringResource
import offlineplaya.shared.generated.resources.*

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

@Composable
private fun SyncStatus.describe(): String = when (this) {
    SyncStatus.Idle ->
        stringResource(Res.string.sync_status_idle)
    is SyncStatus.Scanning ->
        stringResource(Res.string.sync_status_scanning)
    is SyncStatus.Completed -> with(report) {
        stringResource(Res.string.sync_status_completed, tracksScanned, foldersUpserted) +
            if (tracksFailed > 0) " ($tracksFailed failed)" else ""
    }

    is SyncStatus.AlreadyAdded ->
        stringResource(Res.string.sync_status_already_added, displayName)
    is SyncStatus.Failed ->
        stringResource(Res.string.sync_status_failed, message)
}

@PreviewScreenSizes
@Composable
private fun SyncStatusLineIdlePreview() {
    OfflinePlayaTheme {
        Surface { SyncStatusLine(SyncStatus.Idle) }
    }
}

@PreviewScreenSizes
@Composable
private fun SyncStatusLineScanningPreview() {
    OfflinePlayaTheme {
        Surface { SyncStatusLine(SyncStatus.Scanning("content://tree/root")) }
    }
}

@PreviewScreenSizes
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

@PreviewScreenSizes
@Composable
private fun SyncStatusLineAlreadyAddedPreview() {
    OfflinePlayaTheme {
        Surface {
            SyncStatusLine(SyncStatus.AlreadyAdded("content://tree/root", "Music"))
        }
    }
}

@PreviewScreenSizes
@Composable
private fun SyncStatusLineFailedPreview() {
    OfflinePlayaTheme {
        Surface {
            SyncStatusLine(SyncStatus.Failed("permission denied"))
        }
    }
}

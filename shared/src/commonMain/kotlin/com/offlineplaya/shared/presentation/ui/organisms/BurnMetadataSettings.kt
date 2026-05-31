package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.ArtworkPreferences
import com.offlineplaya.shared.domain.usecase.EmbedReport
import com.offlineplaya.shared.presentation.ui.molecules.SettingsSection
import com.offlineplaya.shared.presentation.ui.molecules.SwitchRow
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.burn_button
import offlineplaya.shared.generated.resources.burn_completed_message
import offlineplaya.shared.generated.resources.burn_completed_title
import offlineplaya.shared.generated.resources.burn_download_remote_art
import offlineplaya.shared.generated.resources.burn_download_remote_art_subtitle
import offlineplaya.shared.generated.resources.burn_explainer
import offlineplaya.shared.generated.resources.burn_failed_title
import offlineplaya.shared.generated.resources.burn_progress
import offlineplaya.shared.generated.resources.burn_result
import offlineplaya.shared.generated.resources.burn_start_button
import offlineplaya.shared.generated.resources.common_ok
import offlineplaya.shared.generated.resources.settings_section_metadata_tags
import org.jetbrains.compose.resources.stringResource

@Composable
fun BurnMetadataSettings(
    artworkPreferences: ArtworkPreferences,
    onDownloadRemoteArtChange: (Boolean) -> Unit,
    report: EmbedReport,
    onBurnClick: () -> Unit,
    onAcknowledgeReport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isRunning = report is EmbedReport.Running
    val isCompleted = report is EmbedReport.Completed
    val isFailed = report is EmbedReport.Failed

    SettingsSection(
        title = stringResource(Res.string.settings_section_metadata_tags),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(bottom = 8.dp)) {
            SwitchRow(
                title = stringResource(Res.string.burn_download_remote_art),
                subtitle = stringResource(Res.string.burn_download_remote_art_subtitle),
                checked = artworkPreferences.downloadRemoteArt,
                onCheckedChange = onDownloadRemoteArtChange,
                enabled = !isRunning,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(Res.string.burn_button),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Text(
                text = stringResource(Res.string.burn_explainer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            if (isRunning) {
                val r = report as EmbedReport.Running
                val progress = if (r.total > 0) r.processed.toFloat() / r.total else 0f

                Column(modifier = Modifier.padding(16.dp)) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.burn_progress, r.processed, r.total),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(Res.string.burn_result, r.embedded, r.failed),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                Button(
                    onClick = onBurnClick,
                    enabled = !isRunning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(stringResource(Res.string.burn_start_button))
                }
            }

            AnimatedVisibility(visible = isCompleted || isFailed) {
                Surface(
                    color = if (isFailed) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (isFailed) stringResource(Res.string.burn_failed_title) else stringResource(
                                Res.string.burn_completed_title
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (isCompleted) {
                            val c = report as EmbedReport.Completed
                            Text(
                                text = stringResource(
                                    Res.string.burn_completed_message,
                                    c.embedded,
                                    c.failed
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else if (isFailed) {
                            val f = report as EmbedReport.Failed
                            Text(text = f.message, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(
                            onClick = onAcknowledgeReport,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(stringResource(Res.string.common_ok))
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun BurnMetadataSettingsIdlePreview() {
    PreviewTheme {
        BurnMetadataSettings(
            artworkPreferences = ArtworkPreferences.Default,
            onDownloadRemoteArtChange = {},
            report = EmbedReport.Idle,
            onBurnClick = {},
            onAcknowledgeReport = {}
        )
    }
}

@Preview
@Composable
private fun BurnMetadataSettingsRunningPreview() {
    PreviewTheme {
        BurnMetadataSettings(
            artworkPreferences = ArtworkPreferences.Default,
            onDownloadRemoteArtChange = {},
            report = EmbedReport.Running(processed = 45, total = 100, embedded = 40, failed = 2),
            onBurnClick = {},
            onAcknowledgeReport = {}
        )
    }
}

package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.ArtworkPreferences
import com.offlineplaya.shared.presentation.ui.molecules.SettingsSection
import com.offlineplaya.shared.presentation.ui.molecules.SwitchRow
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * Album-art section: toggle for remote lookups, plus a one-shot "burn covers
 * into a folder" action. The burn-covers action is destructive (writes ID3 /
 * Vorbis tags), so the description spells out *why* you'd want it.
 */
@Composable
fun AlbumArtSettings(
    artworkPreferences: ArtworkPreferences,
    onDownloadRemoteArtChange: (Boolean) -> Unit,
    onEmbedFolderClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSection(title = "Album art", modifier = modifier) {
        Column {
            SwitchRow(
                title = "Download album art",
                subtitle = "Look up missing covers from MusicBrainz and Cover Art Archive.",
                checked = artworkPreferences.downloadRemoteArt,
                onCheckedChange = onDownloadRemoteArtChange,
            )
            Text(
                text = "Burn cover art into a folder's audio files",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp),
            )
            Text(
                text = "Pick a folder and we'll write covers into the audio files. " +
                        "Useful for copying to another device or app — in-app playback " +
                        "already shows them.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            )
            OutlinedButton(
                onClick = onEmbedFolderClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 44.dp)
                    .padding(horizontal = 16.dp, vertical = 2.dp),
            ) {
                Text("Pick folder and burn covers")
            }
        }
    }
}

@PreviewScreenSizes
@Composable
private fun AlbumArtSettingsPreview() {
    PreviewTheme {
        AlbumArtSettings(
            artworkPreferences = ArtworkPreferences.Default,
            onDownloadRemoteArtChange = {},
            onEmbedFolderClick = {},
        )
    }
}

package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * Two-line headline for the Now Playing screen: bold track title above
 * "artist — album". Both lines truncate to a single line with ellipsis.
 */
@Composable
fun NowPlayingHeadline(
    track: Track,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = track.title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${track.artistName} — ${track.albumName}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@PreviewScreenSizes
@Composable
private fun NowPlayingHeadlinePreview() {
    PreviewTheme(darkTheme = true) {
        Surface {
            NowPlayingHeadline(
                track = Track(
                    id = 1L, documentUri = "x", treeUri = "x", relativePath = "x",
                    fileName = "x", title = "Once", artistName = "Pearl Jam",
                    albumArtistName = null, albumName = "Ten", genre = null,
                    year = 1991, trackNumber = 1, discNumber = 1,
                    durationMs = 224_000L, bitrate = null, sampleRate = null,
                    channels = null, codec = null, artistId = null, albumId = null,
                    folderId = 1L, scanStatus = ScanStatus.SCANNED,
                ),
            )
        }
    }
}

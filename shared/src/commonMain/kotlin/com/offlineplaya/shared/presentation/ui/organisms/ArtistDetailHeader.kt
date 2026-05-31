package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.offlineplaya.shared.domain.model.Artist
import com.offlineplaya.shared.presentation.ui.LocalOrientation
import com.offlineplaya.shared.presentation.ui.atoms.ArtistAvatar
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.AppSpacing
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * Artist-detail hero. Portrait shows a 200dp cropped artist image with a
 * counts line below; landscape compresses to a 56dp avatar inline with the
 * counts so the album grid shows up immediately instead of being pushed below
 * a tall hero.
 *
 * Coil resolves remote artist images; the `error` fallback drops in
 * [ArtistAvatar] so a missing image still produces a meaningful header.
 */
@Composable
fun ArtistDetailHeader(
    artist: Artist?,
    modifier: Modifier = Modifier,
) {
    if (LocalOrientation.current.isLandscape) {
        LandscapeBody(artist = artist, modifier = modifier)
    } else {
        PortraitBody(artist = artist, modifier = modifier)
    }
}

@Composable
private fun PortraitBody(artist: Artist?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)) {
            ArtistImage(artist = artist, fallbackSize = 120.dp)
        }
        if (artist != null) {
            Spacer(Modifier.height(AppSpacing.sm))
            Text(
                text = artistCountsLine(artist),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = AppSpacing.sm),
            )
        }
    }
}

@Composable
private fun LandscapeBody(artist: Artist?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        Box(modifier = Modifier.size(56.dp)) {
            ArtistImage(artist = artist, fallbackSize = 56.dp)
        }
        if (artist != null) {
            Text(
                text = artistCountsLine(artist),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ArtistImage(
    artist: Artist?,
    fallbackSize: androidx.compose.ui.unit.Dp,
) {
    val model: Any? = artist?.imageUrl ?: artist
    SubcomposeAsyncImage(
        model = model,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
        loading = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ArtistAvatar(artist = null, size = fallbackSize)
            }
        },
        error = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ArtistAvatar(artist = artist, size = fallbackSize)
            }
        },
        success = { SubcomposeAsyncImageContent() },
    )
}

private fun artistCountsLine(artist: Artist): String {
    val albumPart = "${artist.albumCount} ${if (artist.albumCount == 1) "album" else "albums"}"
    val trackPart = "${artist.trackCount} ${if (artist.trackCount == 1) "track" else "tracks"}"
    return "$albumPart · $trackPart"
}

@PreviewScreenSizes
@Composable
private fun ArtistDetailHeaderPreview() {
    PreviewTheme {
        ArtistDetailHeader(artist = Artist(1, "Pearl Jam", 11, 142))
    }
}

package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.Album
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.atoms.AlbumArtThumb

/**
 * Horizontal shelf of recently-played albums on the Home page. Each tile is
 * a clickable 96dp cover + name. The representative track is resolved per
 * album so Coil can fetch artwork through the existing track-art chain.
 */
@Composable
fun RecentAlbumsShelf(
    albums: List<Album>,
    representativeTrackOfAlbum: suspend (Long) -> Track?,
    onOpenAlbum: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = albums, key = { it.id }) { album ->
            RecentAlbumItem(
                album = album,
                representativeTrackOfAlbum = representativeTrackOfAlbum,
                onClick = { onOpenAlbum(album.id) },
            )
        }
    }
}

@Composable
private fun RecentAlbumItem(
    album: Album,
    representativeTrackOfAlbum: suspend (Long) -> Track?,
    onClick: () -> Unit,
) {
    var rep by remember(album.id) { mutableStateOf<Track?>(null) }
    LaunchedEffect(album.id) { rep = representativeTrackOfAlbum(album.id) }

    Column(
        modifier = Modifier
            .width(96.dp)
            .clickable(onClick = onClick),
    ) {
        AlbumArtThumb(track = rep, size = 96.dp, cornerRadius = 12.dp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = album.name,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

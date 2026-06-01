package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.Album
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.molecules.AlbumRow
import com.offlineplaya.shared.presentation.ui.molecules.EmptyState
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.empty_album_subtitle
import offlineplaya.shared.generated.resources.empty_album_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun AlbumList(
    albums: PersistentList<Album>,
    onAlbumClick: (Long) -> Unit,
    onPlayAlbum: (Album) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    representativeTrackProvider: suspend (Long) -> Track? = { null },
) {
    if (albums.isEmpty()) {
        EmptyState(
            title = stringResource(Res.string.empty_album_title),
            subtitle = stringResource(Res.string.empty_album_subtitle),
            modifier = modifier,
        )
        return
    }
    // Adaptive columns: single column on a portrait phone, two-plus on wide
    // screens so the album browse fills landscape width tidily.
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = COLUMN_MIN_WIDTH),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = albums, key = { it.id }) { album ->
            AlbumRow(
                album = album,
                onClick = { onAlbumClick(album.id) },
                representativeTrackProvider = representativeTrackProvider,
                onPlay = { onPlayAlbum(album) }
            )
        }
    }
}

/** Min cell width before the grid adds another column. Tuned so portrait phones stay single-column. */
private val COLUMN_MIN_WIDTH = 360.dp

@PreviewScreenSizes
@Composable
private fun AlbumListPopulatedPreview() {
    PreviewTheme {
        Surface {
            AlbumList(
                albums = persistentListOf(
                    Album(1, "Ten", artistId = 1, year = 1991, trackCount = 11, durationMs = 0),
                    Album(2, "Vs.", artistId = 1, year = 1993, trackCount = 12, durationMs = 0),
                    Album(3, "Live at the Gorge", artistId = 1, year = null, trackCount = 24, durationMs = 0),
                ),
                onAlbumClick = {},
                onPlayAlbum = {},
            )
        }
    }
}

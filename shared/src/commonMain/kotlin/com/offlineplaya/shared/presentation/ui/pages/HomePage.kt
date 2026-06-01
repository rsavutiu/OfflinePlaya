package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.Album
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.usecase.SyncReport
import com.offlineplaya.shared.presentation.sync.SyncStatus
import com.offlineplaya.shared.presentation.ui.LocalOrientation
import com.offlineplaya.shared.presentation.ui.Orientation
import com.offlineplaya.shared.presentation.ui.atoms.SectionLabel
import com.offlineplaya.shared.presentation.ui.molecules.HomeHeader
import com.offlineplaya.shared.presentation.ui.molecules.HomeStatsRow
import com.offlineplaya.shared.presentation.ui.organisms.HomeBrowseGrid
import com.offlineplaya.shared.presentation.ui.organisms.RecentAlbumsShelf
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.templates.ResponsiveContent
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.home_label_browse
import offlineplaya.shared.generated.resources.home_label_recently_played
import org.jetbrains.compose.resources.stringResource

/**
 * Landing page. When the library has content (or a scan is in flight), shows
 * header + stats + recently-played + browse grid stacked into the available
 * height (no scroll — the grid absorbs leftover space via `weight(1f)`).
 */
@Composable
fun HomePage(
    status: SyncStatus,
    trackCount: Long,
    folderCount: Int,
    albumCount: Int,
    artistCount: Int,
    playlistCount: Int,
    recentAlbums: PersistentList<Album>,
    representativeTrackOfAlbum: suspend (Long) -> Track? = { null },
    onOpenLibrary: () -> Unit,
    onOpenAllTracks: () -> Unit,
    onOpenAlbums: () -> Unit,
    onOpenArtists: () -> Unit,
    onOpenPlaylists: () -> Unit,
    onOpenAlbum: (Long) -> Unit = {},
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        ResponsiveContent(modifier = Modifier.padding(padding)) {
            PopulatedHome(
                status = status,
                trackCount = trackCount,
                folderCount = folderCount,
                albumCount = albumCount,
                artistCount = artistCount,
                playlistCount = playlistCount,
                recentAlbums = recentAlbums,
                representativeTrackOfAlbum = representativeTrackOfAlbum,
                onOpenAllTracks = onOpenAllTracks,
                onOpenAlbums = onOpenAlbums,
                onOpenArtists = onOpenArtists,
                onOpenPlaylists = onOpenPlaylists,
                onOpenAlbum = onOpenAlbum,
                onOpenSearch = onOpenSearch,
                onOpenSettings = onOpenSettings,
            )
        }
    }
}

@Composable
private fun PopulatedHome(
    status: SyncStatus,
    trackCount: Long,
    folderCount: Int,
    albumCount: Int,
    artistCount: Int,
    playlistCount: Int,
    recentAlbums: PersistentList<Album>,
    representativeTrackOfAlbum: suspend (Long) -> Track?,
    onOpenAllTracks: () -> Unit,
    onOpenAlbums: () -> Unit,
    onOpenArtists: () -> Unit,
    onOpenPlaylists: () -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        when (LocalOrientation.current) {
            Orientation.PORTRAIT -> {
                HomeHeader(onOpenSearch = onOpenSearch, onOpenSettings = onOpenSettings)
                HomeStatsRow(
                    trackCount = trackCount,
                    folderCount = folderCount,
                    status = status,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            Orientation.LANDSCAPE -> {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    HomeHeader(
                        modifier = Modifier.fillMaxWidth(0.3f),
                        onOpenSearch = onOpenSearch,
                        onOpenSettings = onOpenSettings,
                    )
                    HomeStatsRow(
                        modifier = Modifier
                            .fillMaxWidth(0.3f)
                            .padding(horizontal = 8.dp),
                        trackCount = trackCount,
                        folderCount = folderCount,
                        status = status,
                    )
                }
            }
        }

        if (recentAlbums.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            SectionLabel(stringResource(Res.string.home_label_recently_played))
            Spacer(modifier = Modifier.height(4.dp))
            RecentAlbumsShelf(
                albums = recentAlbums,
                representativeTrackOfAlbum = representativeTrackOfAlbum,
                onOpenAlbum = onOpenAlbum,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        SectionLabel(stringResource(Res.string.home_label_browse))
        Spacer(modifier = Modifier.height(4.dp))

        HomeBrowseGrid(
            trackCount = trackCount,
            albumCount = albumCount,
            artistCount = artistCount,
            playlistCount = playlistCount,
            collageSource = recentAlbums,
            representativeTrackOfAlbum = representativeTrackOfAlbum,
            onOpenAllTracks = onOpenAllTracks,
            onOpenAlbums = onOpenAlbums,
            onOpenArtists = onOpenArtists,
            onOpenPlaylists = onOpenPlaylists,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .weight(1f),
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@PreviewScreenSizes
@Composable
private fun HomePageIdlePreview() {
    PreviewTheme(darkTheme = true) {
        HomePage(
            status = SyncStatus.Idle,
            trackCount = 0,
            folderCount = 0,
            albumCount = 0, artistCount = 0, playlistCount = 0,
            recentAlbums = persistentListOf(),
            onOpenLibrary = {}, onOpenAllTracks = {},
            onOpenAlbums = {}, onOpenArtists = {},
            onOpenPlaylists = {}, onOpenSearch = {}, onOpenSettings = {},
        )
    }
}

@PreviewScreenSizes
@Composable
private fun HomePagePopulatedPreview() {
    PreviewTheme(darkTheme = true) {
        HomePage(
            status = SyncStatus.Completed(
                SyncReport(foldersUpserted = 22, tracksDiscovered = 212, tracksScanned = 212, tracksFailed = 0),
            ),
            trackCount = 212,
            folderCount = 22,
            albumCount = 18, artistCount = 12, playlistCount = 4,
            recentAlbums = persistentListOf(
                Album(1, "Crystal Castles (II)", 1, 2010, 14, 3_060_000),
                Album(2, "A Moon Shaped Pool", 2, 2016, 11, 3_240_000),
                Album(3, "Shepherd Moons", 3, 1991, 13, 2_880_000),
                Album(4, "Ten", 4, 1991, 11, 3_320_000),
            ),
            onOpenLibrary = {}, onOpenAllTracks = {},
            onOpenAlbums = {}, onOpenArtists = {},
            onOpenPlaylists = {}, onOpenSearch = {}, onOpenSettings = {},
        )
    }
}

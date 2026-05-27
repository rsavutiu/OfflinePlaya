package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.offlineplaya.shared.domain.model.Album
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.usecase.SyncReport
import com.offlineplaya.shared.presentation.sync.SyncStatus
import com.offlineplaya.shared.presentation.ui.atoms.AlbumArtThumb
import com.offlineplaya.shared.util.currentHourOfDay
import org.jetbrains.compose.resources.stringResource
import offlineplaya.shared.generated.resources.*
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * Redesigned home page. No AppTopBar — uses a custom header with greeting,
 * big "Library" title, and icon buttons. Shows stats row, recently-played
 * horizontal scroll, and a 2x2 browse grid. Empty-library state shows a
 * centered CTA to pick a folder.
 */
@Composable
fun HomePage(
    status: SyncStatus,
    trackCount: Long,
    folderCount: Int,
    albumCount: Int,
    artistCount: Int,
    playlistCount: Int,
    recentAlbums: List<Album>,
    representativeTrackOfAlbum: suspend (Long) -> Track? = { null },
    onPickFolder: () -> Unit,
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
    val scanning = status is SyncStatus.Scanning

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (trackCount > 0 || scanning) {
            // ── Library populated (or scanning) ──
            // No verticalScroll — page must fit. The Browse grid absorbs all
            // leftover vertical space via Modifier.weight(1f), so the layout
            // stretches gracefully across phone heights without needing a
            // scrollbar or fixed pixel sizes.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                HomeHeader(
                    onOpenSearch = onOpenSearch,
                    onOpenSettings = onOpenSettings,
                )

                HomeStatsRow(
                    trackCount = trackCount,
                    folderCount = folderCount,
                    status = status,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                if (recentAlbums.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionLabel(stringResource(Res.string.home_label_recently_played))
                    Spacer(modifier = Modifier.height(8.dp))
                    RecentAlbumsShelf(
                        albums = recentAlbums,
                        representativeTrackOfAlbum = representativeTrackOfAlbum,
                        onOpenAlbum = onOpenAlbum,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                SectionLabel(stringResource(Res.string.home_label_browse))

                Spacer(modifier = Modifier.height(8.dp))

                BrowseGrid(
                    trackCount = trackCount,
                    albumCount = albumCount,
                    artistCount = artistCount,
                    playlistCount = playlistCount,
                    // 16 album covers' worth of collage source. Each card gets
                    // a 4-cover slice offset by card index so the four cards
                    // don't look identical; wraps if the library is small.
                    collageSource = recentAlbums,
                    representativeTrackOfAlbum = representativeTrackOfAlbum,
                    scanning = scanning,
                    onOpenAllTracks = onOpenAllTracks,
                    onOpenAlbums = onOpenAlbums,
                    onOpenArtists = onOpenArtists,
                    onOpenPlaylists = onOpenPlaylists,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .weight(1f),
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        } else {
            // ── Empty library — onboarding CTA ──
            EmptyLibraryContent(
                scanning = scanning,
                onPickFolder = onPickFolder,
                onOpenSettings = onOpenSettings,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        }
    }
}

// ─── Custom header (replaces AppTopBar) ───

@Composable
private fun HomeHeader(
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = greetingText(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                text = stringResource(Res.string.home_label_library),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Row {
            IconButton(onClick = onOpenSearch) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(Res.string.now_playing_search),
                    tint = MaterialTheme.colorScheme.outline,
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(Res.string.now_playing_settings),
                    tint = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun greetingText(): String {
    val res = when (currentHourOfDay()) {
        in 5..11 -> Res.string.home_greeting_morning
        in 12..17 -> Res.string.home_greeting_afternoon
        else -> Res.string.home_greeting_evening
    }
    return stringResource(res)
}

// ─── Stats row ───

@Composable
private fun HomeStatsRow(
    trackCount: Long,
    folderCount: Int,
    status: SyncStatus,
    modifier: Modifier = Modifier,
) {
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 10.dp),
    ) {
        StatCell(
            value = trackCount.toString(),
            label = stringResource(Res.string.home_label_tracks),
            modifier = Modifier.weight(1f),
        )
        // Vertical divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(32.dp)
                .background(borderColor),
        )
        StatCell(
            value = folderCount.toString(),
            label = stringResource(Res.string.home_label_folders),
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(32.dp)
                .background(borderColor),
        )
        StatCell(
            value = formatTotalDuration(trackCount),
            label = stringResource(Res.string.home_label_total),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCell(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

/** Rough total duration estimate — 3.5 min average per track. */
private fun formatTotalDuration(trackCount: Long): String {
    val totalMinutes = (trackCount * 3.5).toLong()
    val hours = totalMinutes / 60
    return if (hours > 0) "${hours}h" else "${totalMinutes}m"
}

// ─── Browse 2x2 grid ───

@Composable
private fun BrowseGrid(
    trackCount: Long,
    albumCount: Int,
    artistCount: Int,
    playlistCount: Int,
    collageSource: List<Album>,
    representativeTrackOfAlbum: suspend (Long) -> Track?,
    scanning: Boolean,
    onOpenAllTracks: () -> Unit,
    onOpenAlbums: () -> Unit,
    onOpenArtists: () -> Unit,
    onOpenPlaylists: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Each card gets its own 4-cover slice, offset by card index. Wraps when
    // the library has fewer than 16 covers so no card ever shows blanks.
    fun slice(index: Int): List<Album> {
        if (collageSource.isEmpty()) return emptyList()
        return List(4) { i -> collageSource[(index * 4 + i) % collageSource.size] }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BrowseCard(
                icon = Icons.Default.MusicNote,
                title = stringResource(Res.string.home_title_all_tracks),
                subtitle = stringResource(Res.string.home_label_songs_count, trackCount.toInt()),
                albums = slice(0),
                representativeTrackOfAlbum = representativeTrackOfAlbum,
                enabled = !scanning,
                onClick = onOpenAllTracks,
                modifier = Modifier.weight(1f).fillMaxSize(),
            )
            BrowseCard(
                icon = Icons.Default.Album,
                title = stringResource(Res.string.home_label_albums),
                subtitle = stringResource(Res.string.home_label_albums_count, albumCount),
                albums = slice(1),
                representativeTrackOfAlbum = representativeTrackOfAlbum,
                enabled = !scanning,
                onClick = onOpenAlbums,
                modifier = Modifier.weight(1f).fillMaxSize(),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BrowseCard(
                icon = Icons.Default.Person,
                title = stringResource(Res.string.home_label_artists),
                subtitle = stringResource(Res.string.home_label_artists_count, artistCount),
                albums = slice(2),
                representativeTrackOfAlbum = representativeTrackOfAlbum,
                enabled = !scanning,
                onClick = onOpenArtists,
                modifier = Modifier.weight(1f).fillMaxSize(),
            )
            BrowseCard(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                title = stringResource(Res.string.home_label_playlists),
                subtitle = stringResource(Res.string.home_label_playlists_count, playlistCount),
                albums = slice(3),
                representativeTrackOfAlbum = representativeTrackOfAlbum,
                enabled = !scanning,
                onClick = onOpenPlaylists,
                modifier = Modifier.weight(1f).fillMaxSize(),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
        ),
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun RecentAlbumsShelf(
    albums: List<Album>,
    representativeTrackOfAlbum: suspend (Long) -> Track?,
    onOpenAlbum: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
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
        AlbumArtThumb(
            track = rep,
            size = 96.dp,
            cornerRadius = 12.dp,
        )
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

@Composable
private fun BrowseCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    albums: List<Album>,
    representativeTrackOfAlbum: suspend (Long) -> Track?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        // Fanned stack of mini covers, top-right. Reads as a peeking deck —
        // signals "lots more inside" without animating or rotating through.
        if (albums.isNotEmpty()) {
            CoverFan(
                albums = albums.take(3),
                representativeTrackOfAlbum = representativeTrackOfAlbum,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
            )
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

@Composable
private fun CoverFan(
    albums: List<Album>,
    representativeTrackOfAlbum: suspend (Long) -> Track?,
    modifier: Modifier = Modifier,
) {
    val coverSize = 48.dp
    // Box sized for the rightmost (top) cover; the back covers spill left/up
    // beneath via offset+rotation, so we don't need extra width here.
    Box(modifier = modifier.size(coverSize)) {
        // Back-most: rotated further left, dimmed — reads as "another layer".
        albums.getOrNull(2)?.let { album ->
            FanItem(
                album = album,
                representativeTrackOfAlbum = representativeTrackOfAlbum,
                size = coverSize,
                rotation = -14f,
                translateX = (-16).dp,
                translateY = 6.dp,
                alpha = 0.55f,
            )
        }
        albums.getOrNull(1)?.let { album ->
            FanItem(
                album = album,
                representativeTrackOfAlbum = representativeTrackOfAlbum,
                size = coverSize,
                rotation = -7f,
                translateX = (-8).dp,
                translateY = 3.dp,
                alpha = 0.8f,
            )
        }
        // Front cover, upright and fully opaque.
        albums.getOrNull(0)?.let { album ->
            FanItem(
                album = album,
                representativeTrackOfAlbum = representativeTrackOfAlbum,
                size = coverSize,
                rotation = 0f,
                translateX = 0.dp,
                translateY = 0.dp,
                alpha = 1f,
            )
        }
    }
}

@Composable
private fun FanItem(
    album: Album,
    representativeTrackOfAlbum: suspend (Long) -> Track?,
    size: Dp,
    rotation: Float,
    translateX: Dp,
    translateY: Dp,
    alpha: Float,
) {
    var track by remember(album.id) { mutableStateOf<Track?>(null) }
    LaunchedEffect(album.id) { track = representativeTrackOfAlbum(album.id) }
    val current = track ?: return
    // Render the slot ONLY when Coil resolves real artwork — loading/error
    // states draw nothing, so albums without art simply leave their slot
    // empty rather than showing a placeholder glyph. The slot behind shows
    // through, so the fan still reads as a deck whenever any covers exist.
    SubcomposeAsyncImage(
        model = current,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(size)
            .offset(x = translateX, y = translateY)
            .graphicsLayer {
                rotationZ = rotation
                this.alpha = alpha
            }
            .clip(RoundedCornerShape(6.dp)),
        loading = {},
        error = {},
        success = { SubcomposeAsyncImageContent() },
    )
}

// ─── Empty library CTA ───

@Composable
private fun EmptyLibraryContent(
    scanning: Boolean,
    onPickFolder: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            // Settings icon top-right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(Res.string.now_playing_settings),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(20.dp),
                    )
                    .padding(18.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(Res.string.home_title_start_your_music_journey),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.home_label_select_folder),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(32.dp))
            androidx.compose.material3.Button(
                onClick = onPickFolder,
                enabled = !scanning,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.fillMaxWidth(0.7f),
            ) {
                Text(
                    text = if (scanning)
                        stringResource(Res.string.home_status_scanning)
                    else
                        stringResource(Res.string.home_label_pick_music_folder),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

// ─── Previews ───

@Preview
@Composable
private fun HomePageIdlePreview() {
    PreviewTheme(darkTheme = true) {
        HomePage(
            status = SyncStatus.Idle,
            trackCount = 0,
            folderCount = 0,
            albumCount = 0, artistCount = 0, playlistCount = 0,
            recentAlbums = emptyList(),
            onPickFolder = {}, onOpenLibrary = {}, onOpenAllTracks = {},
            onOpenAlbums = {}, onOpenArtists = {},
            onOpenPlaylists = {}, onOpenSearch = {}, onOpenSettings = {},
        )
    }
}

@Preview
@Composable
private fun HomePageScanningPreview() {
    PreviewTheme(darkTheme = true) {
        HomePage(
            status = SyncStatus.Scanning("content://tree/root"),
            trackCount = 42,
            folderCount = 3,
            albumCount = 0, artistCount = 0, playlistCount = 0,
            recentAlbums = emptyList(),
            onPickFolder = {}, onOpenLibrary = {}, onOpenAllTracks = {},
            onOpenAlbums = {}, onOpenArtists = {},
            onOpenPlaylists = {}, onOpenSearch = {}, onOpenSettings = {},
        )
    }
}

@Preview
@Composable
private fun HomePageCompletedPreview() {
    PreviewTheme(darkTheme = true) {
        HomePage(
            status = SyncStatus.Completed(
                SyncReport(foldersUpserted = 6, tracksDiscovered = 87, tracksScanned = 87, tracksFailed = 0),
            ),
            trackCount = 87,
            folderCount = 6,
            albumCount = 0, artistCount = 0, playlistCount = 0,
            recentAlbums = emptyList(),
            onPickFolder = {}, onOpenLibrary = {}, onOpenAllTracks = {},
            onOpenAlbums = {}, onOpenArtists = {},
            onOpenPlaylists = {}, onOpenSearch = {}, onOpenSettings = {},
        )
    }
}

@Preview
@Composable
private fun HomePageCompletedLightPreview() {
    PreviewTheme(darkTheme = false) {
        HomePage(
            status = SyncStatus.Completed(
                SyncReport(foldersUpserted = 22, tracksDiscovered = 212, tracksScanned = 212, tracksFailed = 0),
            ),
            trackCount = 212,
            folderCount = 22,
            albumCount = 18, artistCount = 12, playlistCount = 4,
            recentAlbums = listOf(
                Album(1, "Crystal Castles (II)", 1, 2010, 14, 3_060_000),
                Album(2, "A Moon Shaped Pool", 2, 2016, 11, 3_240_000),
                Album(3, "Shepherd Moons", 3, 1991, 13, 2_880_000),
                Album(4, "Ten", 4, 1991, 11, 3_320_000),
            ),
            onPickFolder = {}, onOpenLibrary = {}, onOpenAllTracks = {},
            onOpenAlbums = {}, onOpenArtists = {},
            onOpenPlaylists = {}, onOpenSearch = {}, onOpenSettings = {},
        )
    }
}

package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.ListeningStats
import com.offlineplaya.shared.domain.model.PlayedTrack
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.StatsPeriod
import com.offlineplaya.shared.domain.model.TopAlbumStat
import com.offlineplaya.shared.domain.model.TopArtistStat
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.atoms.SectionLabel
import com.offlineplaya.shared.presentation.ui.molecules.EmptyState
import com.offlineplaya.shared.presentation.ui.molecules.RankedStatRow
import com.offlineplaya.shared.presentation.ui.molecules.TopTrackRow
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.templates.ResponsiveContent
import com.offlineplaya.shared.presentation.ui.theme.AppSpacing
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.stats_empty_subtitle
import offlineplaya.shared.generated.resources.stats_empty_title
import offlineplaya.shared.generated.resources.stats_label_distinct_tracks
import offlineplaya.shared.generated.resources.stats_label_listening_time
import offlineplaya.shared.generated.resources.stats_label_plays
import offlineplaya.shared.generated.resources.stats_period_all
import offlineplaya.shared.generated.resources.stats_period_month
import offlineplaya.shared.generated.resources.stats_period_week
import offlineplaya.shared.generated.resources.stats_title
import offlineplaya.shared.generated.resources.stats_top_albums
import offlineplaya.shared.generated.resources.stats_top_artists
import offlineplaya.shared.generated.resources.stats_top_tracks
import org.jetbrains.compose.resources.stringResource

/**
 * Listening stats over the play history: period chips (week / month / all
 * time), headline totals, then Top artists / albums / tracks. Artists and
 * albums drill into their detail pages; a track tap plays the top-tracks
 * list from that entry.
 */
@Composable
fun ListeningStatsPage(
    period: StatsPeriod,
    stats: ListeningStats,
    topArtists: PersistentList<TopArtistStat>,
    topAlbums: PersistentList<TopAlbumStat>,
    topTracks: PersistentList<PlayedTrack>,
    onPeriodChange: (StatsPeriod) -> Unit,
    onArtistClick: (Long) -> Unit,
    onAlbumClick: (Long) -> Unit,
    onPlayTopTrack: (Int) -> Unit,
    onTrackLongPress: (Track) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AppTopBar(title = stringResource(Res.string.stats_title), onBack = onBack)
        },
    ) { padding ->
        ResponsiveContent(modifier = Modifier.padding(padding)) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item(key = "period") {
                    PeriodChips(period = period, onPeriodChange = onPeriodChange)
                }
                if (stats.plays == 0L) {
                    item(key = "empty") {
                        EmptyState(
                            title = stringResource(Res.string.stats_empty_title),
                            subtitle = stringResource(Res.string.stats_empty_subtitle),
                            modifier = Modifier.padding(top = AppSpacing.xl),
                        )
                    }
                    return@LazyColumn
                }
                item(key = "totals") {
                    TotalsRow(
                        stats = stats,
                        modifier = Modifier.padding(
                            horizontal = AppSpacing.lg,
                            vertical = AppSpacing.sm,
                        ),
                    )
                }
                if (topArtists.isNotEmpty()) {
                    item(key = "artists-label") {
                        SectionLabel(
                            text = stringResource(Res.string.stats_top_artists).uppercase(),
                            modifier = Modifier.padding(top = AppSpacing.md),
                        )
                    }
                    items(topArtists.size, key = { "artist-${topArtists[it].artistId}" }) { index ->
                        val artist = topArtists[index]
                        RankedStatRow(
                            rank = index + 1,
                            title = artist.name,
                            plays = artist.plays,
                            onClick = { onArtistClick(artist.artistId) },
                        )
                    }
                }
                if (topAlbums.isNotEmpty()) {
                    item(key = "albums-label") {
                        SectionLabel(
                            text = stringResource(Res.string.stats_top_albums).uppercase(),
                            modifier = Modifier.padding(top = AppSpacing.md),
                        )
                    }
                    items(topAlbums.size, key = { "album-${topAlbums[it].albumId}" }) { index ->
                        val album = topAlbums[index]
                        RankedStatRow(
                            rank = index + 1,
                            title = album.name,
                            subtitle = album.artistName,
                            plays = album.plays,
                            onClick = { onAlbumClick(album.albumId) },
                        )
                    }
                }
                if (topTracks.isNotEmpty()) {
                    item(key = "tracks-label") {
                        SectionLabel(
                            text = stringResource(Res.string.stats_top_tracks).uppercase(),
                            modifier = Modifier.padding(top = AppSpacing.md),
                        )
                    }
                    items(topTracks.size, key = { "track-${topTracks[it].track.id}" }) { index ->
                        val played = topTracks[index]
                        TopTrackRow(
                            rank = index + 1,
                            playedTrack = played,
                            onClick = { onPlayTopTrack(index) },
                            onLongClick = { onTrackLongPress(played.track) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodChips(
    period: StatsPeriod,
    onPeriodChange: (StatsPeriod) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        StatsPeriod.entries.forEach { candidate ->
            FilterChip(
                selected = candidate == period,
                onClick = { onPeriodChange(candidate) },
                label = { Text(candidate.label()) },
            )
        }
    }
}

@Composable
private fun StatsPeriod.label(): String = when (this) {
    StatsPeriod.WEEK -> stringResource(Res.string.stats_period_week)
    StatsPeriod.MONTH -> stringResource(Res.string.stats_period_month)
    StatsPeriod.ALL -> stringResource(Res.string.stats_period_all)
}

/** Headline totals styled to match the home page's stat strip. */
@Composable
private fun TotalsRow(
    stats: ListeningStats,
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
        TotalCell(
            value = "${stats.plays}",
            label = stringResource(Res.string.stats_label_plays),
            modifier = Modifier.weight(1f),
        )
        TotalsDivider(borderColor)
        TotalCell(
            value = stats.listenedMs.formatListeningTime(),
            label = stringResource(Res.string.stats_label_listening_time),
            modifier = Modifier.weight(1f),
        )
        TotalsDivider(borderColor)
        TotalCell(
            value = "${stats.distinctTracks}",
            label = stringResource(Res.string.stats_label_distinct_tracks),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TotalCell(
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
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TotalsDivider(color: Color) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .width(1.dp)
            .height(32.dp)
            .background(color),
    )
}

internal fun Long.formatListeningTime(): String {
    val totalMinutes = this / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}

@PreviewScreenSizes
@Composable
private fun ListeningStatsPagePreview() {
    PreviewTheme {
        ListeningStatsPage(
            period = StatsPeriod.WEEK,
            stats = ListeningStats(plays = 230, distinctTracks = 74, listenedMs = 13_980_000L),
            topArtists = persistentListOf(
                TopArtistStat(1, "Pearl Jam", 58),
                TopArtistStat(2, "Crystal Castles", 31),
            ),
            topAlbums = persistentListOf(
                TopAlbumStat(1, "Ten", "Pearl Jam", 40),
                TopAlbumStat(2, "(I)", "Crystal Castles", 22),
            ),
            topTracks = persistentListOf(
                PlayedTrack(previewStatsTrack("Alive"), 12),
                PlayedTrack(previewStatsTrack("Black"), 9),
            ),
            onPeriodChange = {}, onArtistClick = {}, onAlbumClick = {},
            onPlayTopTrack = {}, onTrackLongPress = {}, onBack = {},
        )
    }
}

@PreviewScreenSizes
@Composable
private fun ListeningStatsPageEmptyPreview() {
    PreviewTheme(darkTheme = true) {
        ListeningStatsPage(
            period = StatsPeriod.ALL,
            stats = ListeningStats.Empty,
            topArtists = persistentListOf(),
            topAlbums = persistentListOf(),
            topTracks = persistentListOf(),
            onPeriodChange = {}, onArtistClick = {}, onAlbumClick = {},
            onPlayTopTrack = {}, onTrackLongPress = {}, onBack = {},
        )
    }
}

private fun previewStatsTrack(title: String) = Track(
    id = title.hashCode().toLong(),
    documentUri = "preview/$title",
    treeUri = "preview",
    relativePath = "x/$title",
    fileName = "$title.mp3",
    title = title,
    artistName = "Pearl Jam",
    albumArtistName = null,
    albumName = "Ten",
    genre = null,
    year = 1991,
    trackNumber = null,
    discNumber = null,
    durationMs = 224_000L,
    bitrate = null,
    sampleRate = null,
    channels = null,
    codec = null,
    artistId = 1L,
    albumId = 1L,
    folderId = 1L,
    scanStatus = ScanStatus.SCANNED,
)

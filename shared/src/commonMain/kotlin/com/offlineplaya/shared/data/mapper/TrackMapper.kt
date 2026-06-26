package com.offlineplaya.shared.data.mapper

import com.offlineplaya.shared.domain.model.CanonicalGenre
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.database.Track as TrackRow

internal const val UNKNOWN_ARTIST = "Unknown Artist"
internal const val UNKNOWN_ALBUM = "Unknown Album"

/**
 * Single source of truth for turning raw Track column values into a [Track]:
 * the blank → "Unknown …" coercions, the DB-enum parsing, and the Long → Int
 * narrowing. The Track-table mapper ([toDomain]) and the playlist/queue JOIN-row
 * mappers all delegate here so those copies can't silently drift apart.
 */
@Suppress("LongParameterList")
internal fun trackFromColumns(
    id: Long,
    documentUri: String,
    treeUri: String,
    relativePath: String,
    fileName: String,
    title: String?,
    artistName: String?,
    albumArtistName: String?,
    albumName: String?,
    genre: String?,
    year: Long?,
    trackNumber: Long?,
    discNumber: Long?,
    durationMs: Long?,
    bitrate: Long?,
    sampleRate: Long?,
    channels: Long?,
    codec: String?,
    artistId: Long?,
    albumId: Long?,
    folderId: Long?,
    scanStatus: String,
    canonicalGenre: String?,
): Track = Track(
    id = id,
    documentUri = documentUri,
    treeUri = treeUri,
    relativePath = relativePath,
    fileName = fileName,
    title = title?.takeIf { it.isNotBlank() } ?: fileName,
    artistName = artistName?.takeIf { it.isNotBlank() } ?: UNKNOWN_ARTIST,
    albumArtistName = albumArtistName?.takeIf { it.isNotBlank() },
    albumName = albumName?.takeIf { it.isNotBlank() } ?: UNKNOWN_ALBUM,
    genre = genre?.takeIf { it.isNotBlank() },
    year = year?.toInt(),
    trackNumber = trackNumber?.toInt(),
    discNumber = discNumber?.toInt(),
    durationMs = durationMs,
    bitrate = bitrate?.toInt(),
    sampleRate = sampleRate?.toInt(),
    channels = channels?.toInt(),
    codec = codec?.takeIf { it.isNotBlank() },
    artistId = artistId,
    albumId = albumId,
    folderId = folderId,
    scanStatus = ScanStatus.fromDbValue(scanStatus),
    canonicalGenre = canonicalGenre?.let { CanonicalGenre.fromDbValue(it) },
)

internal fun TrackRow.toDomain(): Track = trackFromColumns(
    id = id,
    documentUri = document_uri,
    treeUri = tree_uri,
    relativePath = relative_path,
    fileName = file_name,
    title = title,
    artistName = artist_name,
    albumArtistName = album_artist_name,
    albumName = album_name,
    genre = genre,
    year = year,
    trackNumber = track_number,
    discNumber = disc_number,
    durationMs = duration_ms,
    bitrate = bitrate,
    sampleRate = sample_rate,
    channels = channels,
    codec = codec,
    artistId = artist_id,
    albumId = album_id,
    folderId = folder_id,
    scanStatus = scan_status,
    canonicalGenre = canonical_genre,
)

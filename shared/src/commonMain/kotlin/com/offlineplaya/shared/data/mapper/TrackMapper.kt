package com.offlineplaya.shared.data.mapper

import com.offlineplaya.shared.domain.model.CanonicalGenre
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.database.Track as TrackRow

internal const val UNKNOWN_ARTIST = "Unknown Artist"
internal const val UNKNOWN_ALBUM = "Unknown Album"

internal fun TrackRow.toDomain(): Track = Track(
    id = id,
    documentUri = document_uri,
    treeUri = tree_uri,
    relativePath = relative_path,
    fileName = file_name,
    title = title?.takeIf { it.isNotBlank() } ?: file_name,
    artistName = artist_name?.takeIf { it.isNotBlank() } ?: UNKNOWN_ARTIST,
    albumArtistName = album_artist_name?.takeIf { it.isNotBlank() },
    albumName = album_name?.takeIf { it.isNotBlank() } ?: UNKNOWN_ALBUM,
    genre = genre?.takeIf { it.isNotBlank() },
    year = year?.toInt(),
    trackNumber = track_number?.toInt(),
    discNumber = disc_number?.toInt(),
    durationMs = duration_ms,
    bitrate = bitrate?.toInt(),
    sampleRate = sample_rate?.toInt(),
    channels = channels?.toInt(),
    codec = codec?.takeIf { it.isNotBlank() },
    artistId = artist_id,
    albumId = album_id,
    folderId = folder_id,
    scanStatus = ScanStatus.fromDbValue(scan_status),
    canonicalGenre = canonical_genre?.let { CanonicalGenre.fromDbValue(it) },
)

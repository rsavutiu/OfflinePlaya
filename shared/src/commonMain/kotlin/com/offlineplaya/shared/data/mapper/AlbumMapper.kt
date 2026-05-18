package com.offlineplaya.shared.data.mapper

import com.offlineplaya.shared.domain.model.Album
import com.offlineplaya.shared.database.Album as AlbumRow

internal fun AlbumRow.toDomain(): Album = Album(
    id = id,
    name = name,
    artistId = artist_id,
    year = year?.toInt(),
    trackCount = track_count.toInt(),
    durationMs = duration_ms,
)

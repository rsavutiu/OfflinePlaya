package com.offlineplaya.shared.data.mapper

import com.offlineplaya.shared.domain.model.Artist
import com.offlineplaya.shared.database.Artist as ArtistRow

internal fun ArtistRow.toDomain(): Artist = Artist(
    id = id,
    name = name,
    albumCount = album_count.toInt(),
    trackCount = track_count.toInt(),
)

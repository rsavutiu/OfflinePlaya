package com.offlineplaya.shared.data.mapper

import com.offlineplaya.shared.domain.model.Playlist
import com.offlineplaya.shared.database.Playlist as PlaylistRow

internal fun PlaylistRow.toDomain(): Playlist = Playlist(
    id = id,
    name = name,
    createdAt = created_at,
    updatedAt = updated_at,
)

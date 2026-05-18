package com.offlineplaya.shared.data.mapper

import com.offlineplaya.shared.domain.model.Folder
import com.offlineplaya.shared.database.Folder as FolderRow

internal fun FolderRow.toDomain(): Folder = Folder(
    id = id,
    treeUri = tree_uri,
    relativePath = relative_path,
    displayName = display_name,
    parentId = parent_id,
    trackCount = track_count.toInt(),
)

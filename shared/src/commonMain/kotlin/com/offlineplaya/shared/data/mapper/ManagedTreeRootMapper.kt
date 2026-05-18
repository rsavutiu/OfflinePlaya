package com.offlineplaya.shared.data.mapper

import com.offlineplaya.shared.domain.model.ManagedTreeRoot
import com.offlineplaya.shared.database.ManagedTreeRoot as ManagedTreeRootRow

internal fun ManagedTreeRootRow.toDomain(): ManagedTreeRoot = ManagedTreeRoot(
    id = id,
    treeUri = tree_uri,
    displayName = display_name,
    addedAt = added_at,
    lastScannedAt = last_scanned_at,
)

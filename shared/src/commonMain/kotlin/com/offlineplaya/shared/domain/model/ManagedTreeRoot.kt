package com.offlineplaya.shared.domain.model

data class ManagedTreeRoot(
    val id: Long,
    val treeUri: String,
    val displayName: String,
    val addedAt: Long,
    val lastScannedAt: Long?,
)

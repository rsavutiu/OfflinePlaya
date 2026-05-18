package com.offlineplaya.shared.domain.model

data class Folder(
    val id: Long,
    val treeUri: String,
    val relativePath: String,
    val displayName: String,
    val parentId: Long?,
    val trackCount: Int,
)

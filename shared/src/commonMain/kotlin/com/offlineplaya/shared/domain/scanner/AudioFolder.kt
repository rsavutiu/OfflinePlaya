package com.offlineplaya.shared.domain.scanner

/**
 * A folder discovered during a scan, used to materialize the Folder hierarchy.
 *
 * The use case will insert parents before children so the FK [parentRelativePath]
 * → parent `Folder.id` can be resolved.
 *
 * @property treeUri the root tree URI this folder belongs to.
 * @property relativePath POSIX-style path from the tree root.
 *   The root itself uses an empty string (`""`).
 * @property displayName the folder's display name (typically the last segment
 *   of [relativePath], or the tree's display name for the root).
 * @property parentRelativePath the parent folder's relative path, or `null`
 *   when this folder is the tree root.
 */
data class AudioFolder(
    val treeUri: String,
    val relativePath: String,
    val displayName: String,
    val parentRelativePath: String?,
)

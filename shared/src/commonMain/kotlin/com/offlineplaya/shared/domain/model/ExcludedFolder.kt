package com.offlineplaya.shared.domain.model

/**
 * A folder the user has hidden from the library. Both scanners skip anything
 * at or under (treeUri, relativePath), and excluding deletes already-indexed
 * rows — re-including triggers a rescan to bring them back.
 */
data class ExcludedFolder(
    val id: Long,
    val treeUri: String,
    val relativePath: String,
    val displayName: String,
) {
    /** True when [path] (within [treeUri]) is this folder or a descendant. */
    fun covers(treeUri: String, path: String): Boolean =
        treeUri == this.treeUri &&
            (path == relativePath || path.startsWith("$relativePath/"))
}

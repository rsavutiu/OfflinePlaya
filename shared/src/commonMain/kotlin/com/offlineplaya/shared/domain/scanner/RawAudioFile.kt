package com.offlineplaya.shared.domain.scanner

/**
 * A raw audio file discovered on disk, before metadata has been read. Produced
 * by [FolderScanner] and consumed by the library-sync use case.
 *
 * @property documentUri stable SAF Document URI for this file (used as the
 *   unique key in the Track table).
 * @property treeUri the root [android.provider.DocumentsContract] tree URI the
 *   file was discovered under.
 * @property relativePath POSIX-style path from the tree root, e.g.
 *   `"Pearl Jam/Ten/01 Once.flac"`. Always uses forward slashes.
 * @property fileName the file's display name including extension.
 * @property fileSize size in bytes (0 if unknown).
 * @property lastModified epoch millis of the file's last-modified timestamp
 *   (0 if unknown).
 */
data class RawAudioFile(
    val documentUri: String,
    val treeUri: String,
    val relativePath: String,
    val fileName: String,
    val fileSize: Long,
    val lastModified: Long,
)

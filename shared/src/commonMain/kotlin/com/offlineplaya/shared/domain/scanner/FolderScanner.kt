package com.offlineplaya.shared.domain.scanner

/**
 * Walks a SAF tree URI and emits every folder + audio file beneath it.
 *
 * Implementations live in platform source sets (`androidMain` uses
 * `DocumentsContract` directly for speed). Tests use [FakeFolderScanner].
 *
 * Contracts:
 *  - Folders MUST be returned in parent-before-child order.
 *  - The root folder MUST be present with `parentRelativePath == null` and
 *    `relativePath == ""`.
 *  - Every file's `relativePath` directory portion MUST correspond to a folder
 *    in the result set.
 *  - Audio files are detected by the implementation (extension + MIME); the
 *    domain layer does not filter.
 */
interface FolderScanner {
    suspend fun scan(treeUri: String): ScanResult
}

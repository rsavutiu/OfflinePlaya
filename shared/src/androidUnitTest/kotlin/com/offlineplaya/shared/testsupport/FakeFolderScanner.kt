package com.offlineplaya.shared.testsupport

import com.offlineplaya.shared.domain.scanner.AudioFolder
import com.offlineplaya.shared.domain.scanner.FolderScanner
import com.offlineplaya.shared.domain.scanner.RawAudioFile
import com.offlineplaya.shared.domain.scanner.ScanResult

/**
 * Scripted [FolderScanner] for tests. Build with [scan] or use [empty] for a
 * tree that has only the root folder and no files.
 */
class FakeFolderScanner(
    private val scripted: Map<String, ScanResult>,
) : FolderScanner {

    override suspend fun scan(treeUri: String): ScanResult =
        scripted[treeUri] ?: error("FakeFolderScanner: no script for $treeUri")

    companion object {
        fun empty(treeUri: String, displayName: String = "root") = FakeFolderScanner(
            mapOf(
                treeUri to ScanResult(
                    folders = listOf(
                        AudioFolder(
                            treeUri = treeUri,
                            relativePath = "",
                            displayName = displayName,
                            parentRelativePath = null,
                        ),
                    ),
                    files = emptyList(),
                ),
            )
        )

        /** DSL helper for building a single-root scan result inline. */
        fun scan(
            treeUri: String,
            folders: List<AudioFolder>,
            files: List<RawAudioFile>,
        ) = FakeFolderScanner(mapOf(treeUri to ScanResult(folders, files)))
    }
}

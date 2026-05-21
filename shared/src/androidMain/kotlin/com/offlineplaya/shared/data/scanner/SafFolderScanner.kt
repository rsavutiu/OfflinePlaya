package com.offlineplaya.shared.data.scanner

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import com.offlineplaya.shared.domain.scanner.AudioFolder
import com.offlineplaya.shared.domain.scanner.FolderScanner
import com.offlineplaya.shared.domain.scanner.RawAudioFile
import com.offlineplaya.shared.domain.scanner.ScanResult
import com.offlineplaya.shared.util.AppLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [FolderScanner] backed by SAF's [DocumentsContract] API. Uses the bulk
 * `buildChildDocumentsUriUsingTree` cursor traversal — much faster than
 * `DocumentFile.listFiles()` (the plan flags this explicitly).
 *
 * Emits folders in parent-before-child order as required by [FolderScanner].
 * Audio detection is delegated to [AudioFileTypes].
 */
internal class SafFolderScanner(
    private val context: Context,
    private val logger: AppLogger,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : FolderScanner {

    override suspend fun scan(treeUri: String): ScanResult = withContext(ioDispatcher) {
        logger.i(TAG, "Scanning SAF tree: $treeUri")
        val uri = treeUri.toUri()
        val rootDocId = try {
            DocumentsContract.getTreeDocumentId(uri)
        } catch (e: Exception) {
            logger.e(TAG, "Failed to get root document ID for $treeUri", e)
            throw e
        }

        val folders = mutableListOf<AudioFolder>()
        val files = mutableListOf<RawAudioFile>()

        val rootDisplayName = queryDisplayName(uri, rootDocId) ?: "Folder"
        logger.d(TAG, "Root folder name: $rootDisplayName")
        folders += AudioFolder(
            treeUri = treeUri,
            relativePath = "",
            displayName = rootDisplayName,
            parentRelativePath = null,
        )

        walk(
            treeUri = uri,
            treeUriString = treeUri,
            parentDocId = rootDocId,
            parentRelativePath = "",
            folders = folders,
            files = files,
        )

        val result = ScanResult(folders.toList(), files.toList())
        logger.i(TAG, "Scan complete for $treeUri: ${folders.size} folders, ${files.size} files")
        result
    }

    private fun queryDisplayName(treeUri: Uri, docId: String): String? {
        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        return context.contentResolver
            .query(docUri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    }

    /**
     * Depth-first recursion. Reads children of `parentDocId` in a single query,
     * appends folders + audio files at this level, then descends into each
     * subfolder. The cursor is fully drained and closed before recursing so
     * we don't hold a cursor open across levels.
     */
    private fun walk(
        treeUri: Uri,
        treeUriString: String,
        parentDocId: String,
        parentRelativePath: String,
        folders: MutableList<AudioFolder>,
        files: MutableList<RawAudioFile>,
    ) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)

        // Collect subfolders to recurse into after the cursor closes.
        val subfolders = mutableListOf<SubfolderRef>()

        context.contentResolver.query(
            childrenUri,
            PROJECTION,
            null,
            null,
            null,
        )?.use { cursor ->
            val idxId = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val idxName = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val idxMime = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val idxSize = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
            val idxModified = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

            while (cursor.moveToNext()) {
                val docId = cursor.getString(idxId) ?: continue
                val displayName = cursor.getString(idxName) ?: continue
                val mime = cursor.getString(idxMime)
                val size = if (cursor.isNull(idxSize)) 0L else cursor.getLong(idxSize)
                val lastModified = if (cursor.isNull(idxModified)) 0L else cursor.getLong(idxModified)

                val childRelPath = childRelativePath(parentRelativePath, displayName)

                when {
                    mime == DocumentsContract.Document.MIME_TYPE_DIR -> {
                        folders += AudioFolder(
                            treeUri = treeUriString,
                            relativePath = childRelPath,
                            displayName = displayName,
                            parentRelativePath = parentRelativePath,
                        )
                        subfolders += SubfolderRef(docId, childRelPath)
                    }

                    AudioFileTypes.isAudioFile(displayName, mime) -> {
                        val docUri = DocumentsContract
                            .buildDocumentUriUsingTree(treeUri, docId)
                            .toString()
                        files += RawAudioFile(
                            documentUri = docUri,
                            treeUri = treeUriString,
                            relativePath = childRelPath,
                            fileName = displayName,
                            fileSize = size,
                            lastModified = lastModified,
                        )
                    }
                    // else: non-audio file (image, txt …) — skip silently.
                }
            }
        }

        for (sub in subfolders) {
            walk(
                treeUri = treeUri,
                treeUriString = treeUriString,
                parentDocId = sub.docId,
                parentRelativePath = sub.relativePath,
                folders = folders,
                files = files,
            )
        }
    }

    private data class SubfolderRef(val docId: String, val relativePath: String)

    private companion object {
        const val TAG = "SafFolderScanner"

        val PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )

        fun childRelativePath(parent: String, displayName: String): String =
            if (parent.isEmpty()) displayName else "$parent/$displayName"
    }
}

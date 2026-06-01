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
 * [FolderScanner] backed entirely by SAF (Storage Access Framework). Every
 * managed root is a `content://...tree/...` URI obtained from the system
 * folder picker, which is the only path Google Play allows for a music
 * player without the MANAGE_EXTERNAL_STORAGE permission. Files outside any
 * SAF-picked tree are still visible to the user via
 * [MediaStoreDeviceAudioScanner] (read-only) but can't be written to —
 * tag-burning is intentionally scoped to SAF-picked roots only.
 */
internal class SafFolderScanner(
    private val context: Context,
    private val logger: AppLogger,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : FolderScanner {

    private companion object {
        const val TAG = "FolderScanner"

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

    override suspend fun scan(treeUri: String): ScanResult = withContext(ioDispatcher) {
        scanSaf(treeUri)
    }

    private suspend fun scanSaf(treeUri: String): ScanResult {
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
        folders += AudioFolder(
            treeUri = treeUri,
            relativePath = "",
            displayName = rootDisplayName,
            parentRelativePath = null,
        )

        walkSaf(
            treeUri = uri,
            treeUriString = treeUri,
            parentDocId = rootDocId,
            parentRelativePath = "",
            folders = folders,
            files = files,
        )

        return ScanResult(folders.toList(), files.toList())
    }

    private fun queryDisplayName(treeUri: Uri, docId: String): String? {
        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        return context.contentResolver
            .query(docUri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    }

    private fun walkSaf(
        treeUri: Uri,
        treeUriString: String,
        parentDocId: String,
        parentRelativePath: String,
        folders: MutableList<AudioFolder>,
        files: MutableList<RawAudioFile>,
    ) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val subfolders = mutableListOf<SubfolderRef>()

        try {
            context.contentResolver.query(childrenUri, PROJECTION, null, null, null)
                ?.use { cursor ->
                    val idxId =
                        cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val idxName =
                        cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val idxMime =
                        cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val idxSize =
                        cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                    val idxModified =
                        cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                    while (cursor.moveToNext()) {
                        val docId = cursor.getString(idxId) ?: continue
                        val displayName = cursor.getString(idxName) ?: continue
                        val mime = cursor.getString(idxMime)
                        val size = if (cursor.isNull(idxSize)) 0L else cursor.getLong(idxSize)
                        val lastModified =
                            if (cursor.isNull(idxModified)) 0L else cursor.getLong(idxModified)

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
                                val docUri =
                                    DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
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
                        }
                    }
                }
        } catch (e: Exception) {
            logger.e(TAG, "Failed to query $childrenUri", e)
        }

        for (sub in subfolders) {
            walkSaf(treeUri, treeUriString, sub.docId, sub.relativePath, folders, files)
        }
    }

    private data class SubfolderRef(val docId: String, val relativePath: String)
}

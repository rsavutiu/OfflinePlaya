package com.offlineplaya.shared.data.image

import android.content.Context
import android.provider.DocumentsContract
import androidx.core.net.toUri
import com.offlineplaya.shared.domain.image.FolderArtSource
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.util.AppLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SAF-backed [FolderArtSource]. Given a track's `treeUri` and
 * `relativePath`, computes the parent document id and queries its children
 * via the bulk `DocumentsContract` cursor, returning the bytes of the first
 * candidate filename it finds.
 *
 * Match order mirrors common conventions in music tooling and torrent
 * releases: `cover` → `folder` → `album` → `front` → `albumart`. Matching
 * is case-insensitive and accepts `.jpg`, `.jpeg`, `.png`, `.webp`.
 *
 * Returns null when:
 *  - the track lives under the synthetic device-audio tree (no SAF parent
 *    we can list);
 *  - no candidate filename is present;
 *  - any I/O or permission error along the way.
 */
internal class SafFolderArtSource(
    private val context: Context,
    private val logger: AppLogger,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : FolderArtSource {

    override suspend fun findInFolder(track: Track): ByteArray? = withContext(ioDispatcher) {
        if (!track.treeUri.startsWith("content://")) return@withContext null
        val parentRelative = parentRelativePath(track.relativePath)
        try {
            val treeUri = track.treeUri.toUri()
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentDocId =
                if (parentRelative.isEmpty()) rootDocId else "$rootDocId/$parentRelative"
            val childrenUri =
                DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)

            // Single cursor pass; pick the best candidate by priority + extension.
            data class Hit(val docId: String, val name: String, val priority: Int)

            var best: Hit? = null

            context.contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                ),
                null, null, null,
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val docId = cursor.getString(0) ?: continue
                    val name = cursor.getString(1) ?: continue
                    val mime = cursor.getString(2) ?: ""
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) continue
                    val priority = matchPriority(name) ?: continue
                    val current = best
                    if (current == null || priority < current.priority) {
                        best = Hit(docId, name, priority)
                        if (priority == 0) break // cover.* — can't beat it.
                    }
                }
            }

            val hit = best ?: return@withContext null
            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, hit.docId)
            val bytes = context.contentResolver.openInputStream(docUri)?.use { it.readBytes() }
            if (bytes != null) {
                logger.d(
                    TAG,
                    "Sidecar art found: ${hit.name} (${bytes.size} bytes) in $parentRelative"
                )
            }
            bytes
        } catch (t: Throwable) {
            logger.w(TAG, "Sidecar lookup failed for ${track.documentUri}: ${t.message}")
            null
        }
    }

    private fun parentRelativePath(relativePath: String): String {
        val idx = relativePath.lastIndexOf('/')
        return if (idx < 0) "" else relativePath.substring(0, idx)
    }

    private fun matchPriority(filename: String): Int? {
        val lower = filename.lowercase()
        val dot = lower.lastIndexOf('.')
        if (dot < 0) return null
        val ext = lower.substring(dot + 1)
        if (ext !in ACCEPTED_EXTS) return null
        val stem = lower.substring(0, dot).trim()
        return CANDIDATE_STEMS[stem]
    }

    private companion object {
        const val TAG = "SafFolderArtSource"
        val ACCEPTED_EXTS = setOf("jpg", "jpeg", "png", "webp")

        // Lower index wins. "cover" is the strongest convention; "albumart"
        // and variants are last-resort fallbacks Windows Media Player writes.
        val CANDIDATE_STEMS = mapOf(
            "cover" to 0,
            "folder" to 1,
            "album" to 2,
            "front" to 3,
            "albumart" to 4,
            "albumartsmall" to 5,
        )
    }
}

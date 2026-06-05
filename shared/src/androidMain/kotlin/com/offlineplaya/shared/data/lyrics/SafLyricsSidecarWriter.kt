package com.offlineplaya.shared.data.lyrics

import android.content.Context
import android.provider.DocumentsContract
import androidx.core.net.toUri
import com.offlineplaya.shared.domain.lyrics.LyricsSidecarWriter
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.util.AppLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SAF-backed [LyricsSidecarWriter]. Writes `<basename>.lrc` for synced
 * text and `<basename>.txt` for plain text into the track's containing
 * SAF folder, using the same `(authority, rootDocId/relativePath)`
 * structure that [SafSidecarLyricsSource] reads from.
 *
 * Skipped (return `false`) when:
 *  - the track has no SAF parent (the synthetic `device://audio` tree
 *    used for MediaStore-indexed audio — no folder to write into);
 *  - a sidecar with the target name already exists — overwriting a
 *    hand-edited `.lrc` would surprise the user, and the upstream
 *    sidecar source would have hit before remote anyway;
 *  - the text is blank;
 *  - the underlying [DocumentsContract.createDocument] call returns
 *    `null` (provider refused), or the write throws.
 *
 * MIME-type choice: providers may rename `.lrc` to a mime-suggested
 * extension. Passing `application/octet-stream` is the safest way to
 * keep the literal name; `.txt` goes with `text/plain` because every
 * provider preserves that one verbatim.
 */
internal class SafLyricsSidecarWriter(
    private val context: Context,
    private val logger: AppLogger,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : LyricsSidecarWriter {

    override suspend fun write(
        track: Track,
        text: String,
        isSynced: Boolean,
    ): Boolean = withContext(ioDispatcher) {
        if (text.isBlank()) return@withContext false
        if (!track.treeUri.startsWith("content://")) return@withContext false
        val audioStem = track.fileName.substringBeforeLast('.').lowercase().trim()
        if (audioStem.isEmpty()) return@withContext false

        val extension = if (isSynced) "lrc" else "txt"
        val mimeType = if (isSynced) MIME_OCTET_STREAM else MIME_TEXT_PLAIN
        val displayName = "${track.fileName.substringBeforeLast('.')}.$extension"

        val parentRelative = parentRelativePath(track.relativePath)
        val resolver = context.contentResolver

        try {
            val treeUri = track.treeUri.toUri()
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentDocId =
                if (parentRelative.isEmpty()) rootDocId else "$rootDocId/$parentRelative"
            val parentDocUri =
                DocumentsContract.buildDocumentUriUsingTree(treeUri, parentDocId)
            val childrenUri =
                DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)

            if (sidecarAlreadyExists(childrenUri, audioStem, extension)) {
                logger.d(TAG, "Sidecar $displayName already present for ${track.documentUri}; skipping")
                return@withContext false
            }

            val newDocUri = DocumentsContract.createDocument(
                resolver,
                parentDocUri,
                mimeType,
                displayName,
            )
            if (newDocUri == null) {
                logger.w(TAG, "createDocument returned null for $displayName under $parentDocUri")
                return@withContext false
            }

            resolver.openOutputStream(newDocUri, "wt")?.use { out ->
                out.write(text.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: run {
                logger.w(TAG, "openOutputStream returned null for $newDocUri")
                return@withContext false
            }

            logger.d(TAG, "Wrote sidecar $displayName for ${track.documentUri}")
            true
        } catch (t: Throwable) {
            // SAF writes can throw for many reasons (revoked permission,
            // read-only provider, disk full). The in-app cache row is
            // still valid — log + bail rather than bubble.
            logger.w(TAG, "Sidecar write failed for ${track.documentUri}: ${t.message}")
            false
        }
    }

    /**
     * Mirrors [SafSidecarLyricsSource]'s match logic — case-insensitive
     * stem comparison against `.lrc` (or `.txt`) children, so we don't
     * create a duplicate when the user already has one with mixed casing.
     */
    private fun sidecarAlreadyExists(
        childrenUri: android.net.Uri,
        audioStem: String,
        targetExtension: String,
    ): Boolean {
        return try {
            context.contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                ),
                null, null, null,
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val name = cursor.getString(0) ?: continue
                    val mime = cursor.getString(1) ?: ""
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) continue
                    val lower = name.lowercase()
                    val dot = lower.lastIndexOf('.')
                    if (dot < 0) continue
                    val stem = lower.substring(0, dot).trim()
                    val ext = lower.substring(dot + 1)
                    if (stem == audioStem && ext == targetExtension) return@use true
                }
                false
            } == true
        } catch (t: Throwable) {
            // Querying failed — conservatively say "exists" so we don't
            // pile new files on top of an unknown-state folder.
            logger.w(TAG, "Sidecar pre-check failed: ${t.message}")
            true
        }
    }

    private fun parentRelativePath(relativePath: String): String {
        val idx = relativePath.lastIndexOf('/')
        return if (idx < 0) "" else relativePath.substring(0, idx)
    }

    private companion object {
        const val TAG = "SafLyricsSidecar"
        const val MIME_OCTET_STREAM = "application/octet-stream"
        const val MIME_TEXT_PLAIN = "text/plain"
    }
}

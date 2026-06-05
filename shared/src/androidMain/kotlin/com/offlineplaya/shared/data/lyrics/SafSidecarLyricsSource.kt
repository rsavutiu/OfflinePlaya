package com.offlineplaya.shared.data.lyrics

import android.content.Context
import android.provider.DocumentsContract
import androidx.core.net.toUri
import com.offlineplaya.shared.domain.lyrics.SidecarLyricsSource
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.util.AppLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SAF-backed [SidecarLyricsSource]. Near-clone of
 * [com.offlineplaya.shared.data.image.SafFolderArtSource]: queries the track's
 * parent folder via a single `DocumentsContract` child cursor and returns the
 * text of a lyrics sidecar whose stem matches the audio file's.
 *
 * Match: `<audio-basename>.lrc` (preferred, usually synced) → `<audio-basename>.txt`.
 * Returns null for the synthetic `device://audio` tree (no SAF parent to list),
 * when no sidecar is present, or on any I/O error.
 */
internal class SafSidecarLyricsSource(
    private val context: Context,
    private val logger: AppLogger,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SidecarLyricsSource {

    override suspend fun read(track: Track): String? = withContext(ioDispatcher) {
        if (!track.treeUri.startsWith("content://")) return@withContext null
        val audioStem = track.fileName.substringBeforeLast('.').lowercase().trim()
        if (audioStem.isEmpty()) return@withContext null
        val parentRelative = parentRelativePath(track.relativePath)

        try {
            val treeUri = track.treeUri.toUri()
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentDocId =
                if (parentRelative.isEmpty()) rootDocId else "$rootDocId/$parentRelative"
            val childrenUri =
                DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)

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
                    val priority = matchPriority(name, audioStem) ?: continue
                    val current = best
                    if (current == null || priority < current.priority) {
                        best = Hit(docId, name, priority)
                        if (priority == 0) break // .lrc — can't beat it.
                    }
                }
            }

            val hit = best ?: return@withContext null
            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, hit.docId)
            val text = context.contentResolver.openInputStream(docUri)?.use { input ->
                input.readBytes().decodeToString().removePrefix("﻿")
            }
            if (text != null) {
                logger.d(TAG, "Sidecar lyrics found: ${hit.name} (${text.length} chars)")
            }
            text?.takeIf { it.isNotBlank() }
        } catch (t: Throwable) {
            logger.w(TAG, "Sidecar lyrics lookup failed for ${track.documentUri}: ${t.message}")
            null
        }
    }

    private fun parentRelativePath(relativePath: String): String {
        val idx = relativePath.lastIndexOf('/')
        return if (idx < 0) "" else relativePath.substring(0, idx)
    }

    /**
     * Lower wins. The child's stem must equal the audio file's stem
     * (case-insensitive); only `.lrc`/`.txt` extensions qualify.
     */
    private fun matchPriority(filename: String, audioStem: String): Int? {
        val lower = filename.lowercase()
        val dot = lower.lastIndexOf('.')
        if (dot < 0) return null
        val stem = lower.substring(0, dot).trim()
        if (stem != audioStem) return null
        return when (lower.substring(dot + 1)) {
            "lrc" -> 0
            "txt" -> 1
            else -> null
        }
    }

    private companion object {
        const val TAG = "SafSidecarLyrics"
    }
}

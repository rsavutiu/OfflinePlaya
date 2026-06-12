package com.offlineplaya.shared.domain.tag

import com.offlineplaya.shared.domain.model.TrackTagEdits

/**
 * Writes user-edited tags back into an audio file. The Android implementation
 * ([com.offlineplaya.shared.data.tag.JaudiotaggerTrackTagWriter]) uses the same
 * SAF temp-file dance as the art / genre writers.
 *
 * Returns [Result.failure] (rather than throwing) so callers can surface a
 * per-file error — most commonly a non-writable URI (e.g. a MediaStore-indexed
 * file the app hasn't been granted write access to).
 */
interface TrackTagWriter {
    suspend fun write(documentUri: String, edits: TrackTagEdits): Result<TagWriteStats>
}

/**
 * Post-write file stats. Rewriting tags changes the file's size and mtime on
 * disk, so the stored content fingerprint (file_name, file_size, last_modified)
 * used for SAF-vs-MediaStore dedup must be refreshed or the next device-audio
 * resync re-inserts the file as a duplicate. Either field may be null when the
 * provider doesn't expose it — callers keep the stale value in that case.
 */
data class TagWriteStats(
    val fileSize: Long? = null,
    val lastModified: Long? = null,
)

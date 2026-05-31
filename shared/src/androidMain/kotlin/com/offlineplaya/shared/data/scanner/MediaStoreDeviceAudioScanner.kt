package com.offlineplaya.shared.data.scanner

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.offlineplaya.shared.domain.scanner.AudioMetadata
import com.offlineplaya.shared.domain.scanner.DeviceAudioScanner
import com.offlineplaya.shared.domain.scanner.DeviceAudioTrack
import com.offlineplaya.shared.util.AppLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android implementation of [DeviceAudioScanner] backed by
 * [MediaStore.Audio.Media]. This is what lets the app see music in
 * `Download/`, the internal-storage root, and other places SAF refuses to
 * grant tree-URI access to on Android 11+.
 */
internal class MediaStoreDeviceAudioScanner(
    private val context: Context,
    private val logger: AppLogger,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DeviceAudioScanner {

    private companion object {
        const val TAG = "MediaStoreScanner"
        val PRIMARY_STORAGE_PREFIXES = listOf(
            "/storage/emulated/0/",
            "/sdcard/",
            "/mnt/sdcard/",
        )
    }

    override suspend fun scan(): List<DeviceAudioTrack> = withContext(ioDispatcher) {
        logger.i(TAG, "Starting MediaStore scan...")
        if (!hasPermission()) {
            logger.w(TAG, "Missing permissions for MediaStore scan")
            return@withContext emptyList()
        }

        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        // Include music and podcasts. Audiobooks added for API 29+.
        var selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 OR " +
                "${MediaStore.Audio.Media.IS_PODCAST} != 0"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            selection += " OR ${MediaStore.Audio.Media.IS_AUDIOBOOK} != 0"
        }

        val projection = baseProjection()

        val out = mutableListOf<DeviceAudioTrack>()
        runCatching {
            resolver.query(collection, projection, selection, null, null)?.use { cursor ->
                logger.d(TAG, "Query returned ${cursor.count} potential audio files")

                val idxId = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val idxDisplay = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val idxSize = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val idxModified = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val idxTitle = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val idxArtist = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val idxAlbum = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val idxAlbumArtist = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ARTIST)
                val idxYear = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val idxTrack = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val idxDuration = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val idxMime = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val idxRelative = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH) else -1
                val idxData = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idxId)
                    val displayName = cursor.getString(idxDisplay) ?: continue
                    val size = if (cursor.isNull(idxSize)) 0L else cursor.getLong(idxSize)
                    val lastModifiedSec =
                        if (cursor.isNull(idxModified)) 0L else cursor.getLong(idxModified)
                    val lastModifiedMs = lastModifiedSec * 1000L

                    val relativePath = computeRelativePath(
                        cursor = cursor,
                        idxRelative = idxRelative,
                        idxData = idxData,
                        displayName = displayName,
                    )

                    val (discNumber, trackNumber) = decodeTrackField(
                        if (cursor.isNull(idxTrack)) null else cursor.getInt(idxTrack)
                    )

                    val sourceUri = ContentUris.withAppendedId(collection, id).toString()

                    val metadata = AudioMetadata(
                        title = cursor.getString(idxTitle)?.takeIf { it.isNotBlank() },
                        artist = cursor.getString(idxArtist)?.takeIf { it.isNotBlank() },
                        albumArtist = if (idxAlbumArtist >= 0)
                            cursor.getString(idxAlbumArtist)?.takeIf { it.isNotBlank() } else null,
                        album = cursor.getString(idxAlbum)?.takeIf { it.isNotBlank() },
                        genre = null,
                        year = if (cursor.isNull(idxYear)) null else cursor.getInt(idxYear)
                            .takeIf { it > 0 },
                        trackNumber = trackNumber,
                        discNumber = discNumber,
                        durationMs = if (cursor.isNull(idxDuration)) null else cursor.getLong(
                            idxDuration
                        ),
                        bitrate = null,
                        sampleRate = null,
                        channels = null,
                        codec = cursor.getString(idxMime),
                    )

                    out += DeviceAudioTrack(
                        sourceUri = sourceUri,
                        relativePath = relativePath,
                        fileName = displayName,
                        fileSize = size,
                        lastModified = lastModifiedMs,
                        metadata = metadata,
                    )
                }
            }
        }.onFailure { t ->
            logger.e(TAG, "MediaStore query failed", t)
        }

        logger.i(TAG, "Scan complete: found ${out.size} tracks")
        out
    }

    private fun hasPermission(): Boolean {
        // If user granted MANAGE_EXTERNAL_STORAGE, we're good for any file access.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            return true
        }

        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }

    private fun computeRelativePath(
        cursor: android.database.Cursor,
        idxRelative: Int,
        idxData: Int,
        displayName: String,
    ): String {
        if (idxRelative >= 0 && !cursor.isNull(idxRelative)) {
            val rel = cursor.getString(idxRelative) ?: ""
            val cleaned = rel.trimEnd('/')
            return if (cleaned.isEmpty()) displayName else "$cleaned/$displayName"
        }
        if (idxData >= 0 && !cursor.isNull(idxData)) {
            val data = cursor.getString(idxData) ?: return displayName
            for (prefix in PRIMARY_STORAGE_PREFIXES) {
                if (data.startsWith(prefix)) return data.removePrefix(prefix)
            }
            return data.trimStart('/')
        }
        return displayName
    }

    private fun decodeTrackField(raw: Int?): Pair<Int?, Int?> {
        if (raw == null || raw <= 0) return null to null
        return if (raw >= 1000) {
            (raw / 1000) to (raw % 1000)
        } else {
            null to raw
        }
    }

    private fun baseProjection(): Array<String> {
        val cols = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATA,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            cols += MediaStore.Audio.Media.RELATIVE_PATH
        }
        cols += MediaStore.Audio.Media.ALBUM_ARTIST
        return cols.toTypedArray()
    }
}

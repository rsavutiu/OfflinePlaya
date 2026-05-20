package com.offlineplaya.shared.data.scanner

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.offlineplaya.shared.domain.scanner.AudioMetadata
import com.offlineplaya.shared.domain.scanner.DeviceAudioScanner
import com.offlineplaya.shared.domain.scanner.DeviceAudioTrack
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android implementation of [DeviceAudioScanner] backed by
 * [MediaStore.Audio.Media]. This is what lets the app see music in
 * `Download/`, the internal-storage root, and other places SAF refuses to
 * grant tree-URI access to on Android 11+.
 *
 * Permission gating: returns an empty list if the audio permission isn't
 * held, rather than throwing. The caller (sync pipeline) treats a no-op
 * scan exactly the same as "no device audio found", which is the right
 * behaviour when the user hasn't yet granted the permission.
 *
 * Path handling:
 *  - API 29+: [MediaStore.MediaColumns.RELATIVE_PATH] gives us a clean
 *    `Download/Subfolder/` prefix that joins with `DISPLAY_NAME` to form
 *    the relative path.
 *  - API 24–28: `RELATIVE_PATH` doesn't exist; we derive it from `DATA`
 *    by stripping the primary-storage prefix. Best-effort — if the file
 *    lives on an SD card the relative path will just be the absolute
 *    filesystem path, which still groups sensibly enough.
 */
internal class MediaStoreDeviceAudioScanner(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DeviceAudioScanner {

    override suspend fun scan(): List<DeviceAudioTrack> = withContext(ioDispatcher) {
        if (!hasPermission()) return@withContext emptyList()

        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        // IS_MUSIC keeps notification sounds, ringtones, and alarms out of the
        // library. Users adding genuine music to Ringtones/ can re-tag it
        // themselves — that's the boundary MediaStore itself uses.
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} = 1"

        val projection = baseProjection()

        val out = mutableListOf<DeviceAudioTrack>()
        resolver.query(collection, projection, selection, null, null)?.use { cursor ->
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
                // DATE_MODIFIED is in seconds, not millis.
                val lastModifiedSec = if (cursor.isNull(idxModified)) 0L else cursor.getLong(idxModified)
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
                    genre = null, // genre lookup requires a separate MediaStore join — skipped for now
                    year = if (cursor.isNull(idxYear)) null else cursor.getInt(idxYear).takeIf { it > 0 },
                    trackNumber = trackNumber,
                    discNumber = discNumber,
                    durationMs = if (cursor.isNull(idxDuration)) null else cursor.getLong(idxDuration),
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
        out
    }

    private fun hasPermission(): Boolean {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Build the relative path the rest of the app uses to group tracks into
     * folders. On Q+ this is straightforward; on older API levels we strip
     * the primary-storage prefix off `DATA`.
     */
    private fun computeRelativePath(
        cursor: android.database.Cursor,
        idxRelative: Int,
        idxData: Int,
        displayName: String,
    ): String {
        if (idxRelative >= 0 && !cursor.isNull(idxRelative)) {
            val rel = cursor.getString(idxRelative) ?: ""
            // MediaStore returns trailing-slash-terminated paths like "Music/Albums/".
            val cleaned = rel.trimEnd('/')
            return if (cleaned.isEmpty()) displayName else "$cleaned/$displayName"
        }
        if (idxData >= 0 && !cursor.isNull(idxData)) {
            val data = cursor.getString(idxData) ?: return displayName
            // Strip the well-known primary storage prefix. If the file is on
            // an SD card or elsewhere, fall through and use the absolute path
            // as the relative path — it'll group oddly but won't crash.
            for (prefix in PRIMARY_STORAGE_PREFIXES) {
                if (data.startsWith(prefix)) return data.removePrefix(prefix)
            }
            return data.trimStart('/')
        }
        return displayName
    }

    /**
     * MediaStore's [MediaStore.Audio.Media.TRACK] column historically encodes
     * `disc * 1000 + track`, so a value like 2005 means disc 2, track 5.
     * Values < 1000 just mean track N with no disc info.
     */
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
        // ALBUM_ARTIST has been a documented column since API 30, but most
        // OEMs populate it from API 24 onwards. getColumnIndex returns -1
        // when absent, which we handle at read time.
        cols += MediaStore.Audio.Media.ALBUM_ARTIST
        return cols.toTypedArray()
    }

    private companion object {
        val PRIMARY_STORAGE_PREFIXES = listOf(
            "/storage/emulated/0/",
            "/sdcard/",
            "/mnt/sdcard/",
        )
    }
}

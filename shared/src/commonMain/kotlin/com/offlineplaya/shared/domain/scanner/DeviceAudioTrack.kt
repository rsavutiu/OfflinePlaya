package com.offlineplaya.shared.domain.scanner

/**
 * A single audio file as seen by the platform's "I already know about this"
 * index — MediaStore on Android, the user's iTunes library on macOS, an
 * `xdg-user-dir` walk on Linux. Surfaced by [DeviceAudioScanner].
 *
 * Unlike [RawAudioFile] (which describes a SAF tree entry whose metadata
 * still has to be read), a [DeviceAudioTrack] carries the metadata inline
 * — the platform index has already done that work, so we don't need to
 * round-trip through [MetadataReader] for these.
 *
 * @property sourceUri Stable URI for playback (`content://media/external/audio/media/{id}`
 *   on Android). Used as the Track table's `document_uri`, so this also acts
 *   as the unique key when the same file is re-scanned.
 * @property relativePath POSIX-style path under the device's audio root, e.g.
 *   `"Download/My Song.mp3"`. Drives folder synthesis so the UI can group
 *   these tracks by directory the same way it does SAF tracks.
 * @property fileName Display name including extension.
 */
data class DeviceAudioTrack(
    val sourceUri: String,
    val relativePath: String,
    val fileName: String,
    val fileSize: Long,
    val lastModified: Long,
    val metadata: AudioMetadata,
)

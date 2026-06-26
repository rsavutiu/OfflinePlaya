package com.offlineplaya.android.e2e

import com.offlineplaya.shared.domain.scanner.AudioFolder
import com.offlineplaya.shared.domain.scanner.AudioMetadata
import com.offlineplaya.shared.domain.scanner.DeviceAudioScanner
import com.offlineplaya.shared.domain.scanner.DeviceAudioTrack
import com.offlineplaya.shared.domain.scanner.FolderScanner
import com.offlineplaya.shared.domain.scanner.MetadataReader
import com.offlineplaya.shared.domain.scanner.ScanResult

/**
 * Fixture library for E2E tests, surfaced through the [DeviceAudioScanner] path
 * (no managed SAF root needed). The sync coordinator folds these in under the
 * synthetic `mediastore://audio` tree, so a normal `resyncAll()` populates the
 * library deterministically with no real MediaStore or SAF access.
 *
 * Returning the *same* list on every scan is what makes the seed safe against
 * the sync's reconcile pass — re-scanning keeps the rows instead of wiping them.
 */
object E2ELibrary {

    /** Stable, human-readable titles the tests assert on. */
    val tracks: List<DeviceAudioTrack> = listOf(
        deviceTrack(id = 1, title = "Alpha Anthem", artist = "The Testers", album = "Fixtures"),
        deviceTrack(id = 2, title = "Beta Ballad", artist = "The Testers", album = "Fixtures"),
        deviceTrack(id = 3, title = "Gamma Groove", artist = "The Testers", album = "Fixtures"),
    )

    private fun deviceTrack(id: Int, title: String, artist: String, album: String) = DeviceAudioTrack(
        sourceUri = "content://media/external/audio/media/$id",
        relativePath = "Music/$title.mp3",
        fileName = "$title.mp3",
        fileSize = 4_000_000L + id,
        lastModified = 1_700_000_000L + id,
        metadata = AudioMetadata(
            title = title,
            artist = artist,
            albumArtist = artist,
            album = album,
            genre = "Rock",
            year = 2020,
            trackNumber = id,
            discNumber = 1,
            durationMs = 200_000L,
            bitrate = 320_000,
            sampleRate = 44_100,
            channels = 2,
            codec = "mp3",
        ),
    )
}

/** [DeviceAudioScanner] that always returns [E2ELibrary.tracks]. */
class FakeE2EDeviceAudioScanner : DeviceAudioScanner {
    override suspend fun scan(): List<DeviceAudioTrack> = E2ELibrary.tracks
}

/**
 * [FolderScanner] that reports an empty tree for any URI. The E2E library comes
 * in via the device-audio path, so no SAF folder is ever walked — but the
 * coordinator still needs a binding.
 */
class FakeE2EFolderScanner : FolderScanner {
    override suspend fun scan(treeUri: String): ScanResult = ScanResult(
        folders = listOf(
            AudioFolder(
                treeUri = treeUri,
                relativePath = "",
                displayName = "root",
                parentRelativePath = null,
            ),
        ),
        files = emptyList(),
    )
}

/**
 * [MetadataReader] returning [AudioMetadata.Empty]. Device-audio tracks carry
 * their metadata inline, so the sync pipeline never reads through here for the
 * fixtures — this exists only to satisfy the use case's constructor.
 */
class FakeE2EMetadataReader : MetadataReader {
    override suspend fun read(documentUri: String): AudioMetadata = AudioMetadata.Empty
}

package com.offlineplaya.shared.domain.usecase

import com.offlineplaya.shared.domain.model.CanonicalGenre
import com.offlineplaya.shared.data.repository.SqlAlbumRepository
import com.offlineplaya.shared.data.repository.SqlArtistRepository
import com.offlineplaya.shared.data.repository.SqlExcludedFolderRepository
import com.offlineplaya.shared.data.repository.SqlFolderRepository
import com.offlineplaya.shared.data.repository.SqlManagedTreeRootRepository
import com.offlineplaya.shared.data.repository.SqlTrackRepository
import com.offlineplaya.shared.database.OfflinePlayaDatabase
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.scanner.AudioFolder
import com.offlineplaya.shared.domain.scanner.AudioMetadata
import com.offlineplaya.shared.domain.scanner.DeviceAudioScanner
import com.offlineplaya.shared.domain.scanner.DeviceAudioTrack
import com.offlineplaya.shared.domain.scanner.RawAudioFile
import com.offlineplaya.shared.testsupport.FakeDeviceAudioScanner
import com.offlineplaya.shared.testsupport.FakeFolderScanner
import com.offlineplaya.shared.testsupport.FakeMetadataReader
import com.offlineplaya.shared.testsupport.createInMemoryDatabase
import com.offlineplaya.shared.util.TestLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LibrarySyncUseCaseTest {

    private data class Fixture(
        val db: OfflinePlayaDatabase,
        val managedRoots: SqlManagedTreeRootRepository,
        val folders: SqlFolderRepository,
        val artists: SqlArtistRepository,
        val albums: SqlAlbumRepository,
        val tracks: SqlTrackRepository,
        val excludedFolders: SqlExcludedFolderRepository,
    )

    private fun fixture(): Fixture {
        val db = createInMemoryDatabase()
        return Fixture(
            db = db,
            managedRoots = SqlManagedTreeRootRepository(db, Dispatchers.Unconfined),
            folders = SqlFolderRepository(db, TestLogger(), Dispatchers.Unconfined),
            artists = SqlArtistRepository(db, TestLogger(), Dispatchers.Unconfined),
            albums = SqlAlbumRepository(db, TestLogger(), Dispatchers.Unconfined),
            tracks = SqlTrackRepository(db, TestLogger(), Dispatchers.Unconfined),
            excludedFolders = SqlExcludedFolderRepository(db, TestLogger(), Dispatchers.Unconfined),
        )
    }

    private fun useCase(
        f: Fixture,
        scanner: FakeFolderScanner,
        reader: FakeMetadataReader,
        deviceAudio: FakeDeviceAudioScanner = FakeDeviceAudioScanner(),
    ) = LibrarySyncUseCase(
        managedRoots = f.managedRoots,
        folders = f.folders,
        artists = f.artists,
        albums = f.albums,
        tracks = f.tracks,
        scanner = scanner,
        metadataReader = reader,
        deviceAudio = deviceAudio,
        excludedFolders = f.excludedFolders,
        logger = TestLogger(),
    )

    @Test
    fun `empty tree inserts only the root folder and reports zero tracks`() = runTest {
        val f = fixture()
        val uri = "content://tree/root"
        val scanner = FakeFolderScanner.empty(uri, displayName = "Music")

        val report = useCase(f, scanner, FakeMetadataReader()).syncOne(uri)

        assertEquals(1, report.foldersUpserted)
        assertEquals(0, report.tracksDiscovered)
        assertEquals(0, report.tracksScanned)
        assertEquals(0, report.tracksFailed)
        assertEquals(0L, f.tracks.count())
        assertEquals(1, f.folders.observeRoots().first().size)
    }

    @Test
    fun `single track in root populates artist album folder and updates metadata`() = runTest {
        val f = fixture()
        val uri = "content://tree/root"
        val docUri = "content://tree/root/song.flac"
        val scanner = FakeFolderScanner.scan(
            treeUri = uri,
            folders = listOf(
                AudioFolder(treeUri = uri, relativePath = "", displayName = "Music", parentRelativePath = null),
            ),
            files = listOf(
                RawAudioFile(
                    documentUri = docUri,
                    treeUri = uri,
                    relativePath = "song.flac",
                    fileName = "song.flac",
                    fileSize = 5_000_000L,
                    lastModified = 1_700_000_000L,
                ),
            ),
        )
        val reader = FakeMetadataReader(
            scripted = mapOf(
                docUri to AudioMetadata(
                    title = "Once", artist = "Pearl Jam", albumArtist = null,
                    album = "Ten", genre = "Rock", year = 1991,
                    trackNumber = 1, discNumber = 1, durationMs = 224_000L,
                    bitrate = 1_000_000, sampleRate = 44_100, channels = 2,
                    codec = "flac",
                ),
            ),
        )

        val report = useCase(f, scanner, reader).syncOne(uri)

        assertEquals(1, report.tracksDiscovered)
        assertEquals(1, report.tracksScanned)
        assertEquals(0, report.tracksFailed)

        val track = f.tracks.findByDocumentUri(docUri)
        assertNotNull(track)
        assertEquals(ScanStatus.SCANNED, track.scanStatus)
        assertEquals("Once", track.title)
        assertEquals("Pearl Jam", track.artistName)
        assertEquals("Ten", track.albumName)
        assertEquals(1991, track.year)
        assertNotNull(track.artistId)
        assertNotNull(track.albumId)
        assertNotNull(track.folderId)

        val artist = f.artists.findByName("Pearl Jam")
        assertNotNull(artist)
        assertEquals(1, artist.trackCount, "artist counts refreshed")
        assertEquals(1, artist.albumCount)

        val album = f.albums.findByNameAndArtist("Ten", artist.id)
        assertNotNull(album)
        assertEquals(1, album.trackCount, "album aggregates refreshed")
        assertEquals(224_000L, album.durationMs)
    }

    @Test
    fun `multi-folder tree inserts folders parent-first and assigns tracks to leaf folder`() = runTest {
        val f = fixture()
        val uri = "content://tree/root"
        val docA = "$uri/Artist%20A/Album%201/01.mp3"
        val docB = "$uri/Artist%20A/Album%201/02.mp3"
        val docC = "$uri/Artist%20B/Album%202/01.mp3"

        val scanner = FakeFolderScanner.scan(
            treeUri = uri,
            folders = listOf(
                AudioFolder(uri, "", "root", null),
                AudioFolder(uri, "Artist A", "Artist A", ""),
                AudioFolder(uri, "Artist A/Album 1", "Album 1", "Artist A"),
                AudioFolder(uri, "Artist B", "Artist B", ""),
                AudioFolder(uri, "Artist B/Album 2", "Album 2", "Artist B"),
            ),
            files = listOf(
                RawAudioFile(docA, uri, "Artist A/Album 1/01.mp3", "01.mp3", 0, 0),
                RawAudioFile(docB, uri, "Artist A/Album 1/02.mp3", "02.mp3", 0, 0),
                RawAudioFile(docC, uri, "Artist B/Album 2/01.mp3", "01.mp3", 0, 0),
            ),
        )
        val reader = FakeMetadataReader(
            scripted = mapOf(
                docA to md(artist = "Artist A", album = "Album 1", title = "T1"),
                docB to md(artist = "Artist A", album = "Album 1", title = "T2"),
                docC to md(artist = "Artist B", album = "Album 2", title = "T3"),
            ),
        )

        val report = useCase(f, scanner, reader).syncOne(uri)

        assertEquals(5, report.foldersUpserted)
        assertEquals(3, report.tracksScanned)

        // Folder parent ids resolved correctly
        val artistAFolder = f.folders.findByPath(uri, "Artist A")
        val albumFolder = f.folders.findByPath(uri, "Artist A/Album 1")
        assertNotNull(artistAFolder)
        assertNotNull(albumFolder)
        assertEquals(artistAFolder.id, albumFolder.parentId)

        // Tracks live under the leaf album folder
        val trackA = f.tracks.findByDocumentUri(docA)
        assertEquals(albumFolder.id, trackA?.folderId)

        // Artist A has 2 tracks across 1 album; Artist B has 1 across 1
        val artistA = f.artists.findByName("Artist A")!!
        val artistB = f.artists.findByName("Artist B")!!
        assertEquals(2, artistA.trackCount)
        assertEquals(1, artistA.albumCount)
        assertEquals(1, artistB.trackCount)
    }

    @Test
    fun `metadata read failure marks track ERROR without inserting artist or album`() = runTest {
        val f = fixture()
        val uri = "content://tree/root"
        val docOk = "$uri/ok.mp3"
        val docBad = "$uri/broken.mp3"
        val scanner = FakeFolderScanner.scan(
            treeUri = uri,
            folders = listOf(AudioFolder(uri, "", "root", null)),
            files = listOf(
                RawAudioFile(docOk, uri, "ok.mp3", "ok.mp3", 0, 0),
                RawAudioFile(docBad, uri, "broken.mp3", "broken.mp3", 0, 0),
            ),
        )
        val reader = FakeMetadataReader(
            scripted = mapOf(docOk to md(artist = "A", album = "Al", title = "T")),
            failingUris = setOf(docBad),
        )

        val report = useCase(f, scanner, reader).syncOne(uri)

        assertEquals(2, report.tracksDiscovered)
        assertEquals(1, report.tracksScanned)
        assertEquals(1, report.tracksFailed)

        assertEquals(ScanStatus.ERROR, f.tracks.findByDocumentUri(docBad)?.scanStatus)
        assertEquals(ScanStatus.SCANNED, f.tracks.findByDocumentUri(docOk)?.scanStatus)

        // No phantom artist/album from the failed read
        assertEquals(1, f.artists.observeAll().first().size)
    }

    @Test
    fun `albumArtist is preferred over artist when both present`() = runTest {
        val f = fixture()
        val uri = "content://tree/root"
        val doc = "$uri/song.mp3"
        val scanner = FakeFolderScanner.scan(
            treeUri = uri,
            folders = listOf(AudioFolder(uri, "", "root", null)),
            files = listOf(RawAudioFile(doc, uri, "song.mp3", "song.mp3", 0, 0)),
        )
        val reader = FakeMetadataReader(
            scripted = mapOf(
                doc to md(
                    artist = "Featured Guest",
                    albumArtist = "Main Act",
                    album = "Compilation",
                    title = "Track",
                ),
            ),
        )

        useCase(f, scanner, reader).syncOne(uri)

        assertNotNull(f.artists.findByName("Main Act"))
        assertNull(
            f.artists.findByName("Featured Guest"),
            "track-level artist must not create a phantom Artist row when albumArtist is present",
        )
    }

    @Test
    fun `syncAll iterates every managed root and reports a sum`() = runTest {
        val f = fixture()
        val a = "content://tree/A"
        val b = "content://tree/B"
        f.managedRoots.add(a, "Drive A")
        f.managedRoots.add(b, "Drive B")

        val scanner = FakeFolderScanner(
            mapOf(
                a to com.offlineplaya.shared.domain.scanner.ScanResult(
                    folders = listOf(AudioFolder(a, "", "A", null)),
                    files = listOf(RawAudioFile("$a/x.mp3", a, "x.mp3", "x.mp3", 0, 0)),
                ),
                b to com.offlineplaya.shared.domain.scanner.ScanResult(
                    folders = listOf(AudioFolder(b, "", "B", null)),
                    files = emptyList(),
                ),
            ),
        )
        val reader = FakeMetadataReader(
            scripted = mapOf("$a/x.mp3" to md(artist = "A", album = "Al", title = "X")),
        )

        val report = useCase(f, scanner, reader).syncAll()

        assertEquals(2, report.foldersUpserted)
        assertEquals(1, report.tracksDiscovered)
        assertEquals(1, report.tracksScanned)
        assertTrue(
            f.managedRoots.findByUri(a)!!.lastScannedAt != null,
            "managed root A must be marked as scanned",
        )
        assertTrue(
            f.managedRoots.findByUri(b)!!.lastScannedAt != null,
            "managed root B must be marked as scanned",
        )
    }

    @Test
    fun `SAF-then-device dedup keeps SAF row when content key matches`() = runTest {
        val f = fixture()
        val safUri = "content://tree/safroot"
        val safDoc = "$safUri/song.mp3"
        val deviceDoc = "${DeviceAudioScanner.ROOT_URI}/Download/song.mp3"
        val name = "song.mp3"
        val size = 4_000_000L
        val mtime = 1_700_000_000L

        val scanner = FakeFolderScanner.scan(
            treeUri = safUri,
            folders = listOf(AudioFolder(safUri, "", "SAF", null)),
            files = listOf(RawAudioFile(safDoc, safUri, "song.mp3", name, size, mtime)),
        )
        val reader = FakeMetadataReader(
            scripted = mapOf(
                safDoc to md(artist = "A", album = "Al", title = "Song"),
                deviceDoc to md(artist = "A", album = "Al", title = "Song"),
            ),
        )
        val device = FakeDeviceAudioScanner(
            tracks = listOf(
                DeviceAudioTrack(
                    sourceUri = deviceDoc,
                    relativePath = "Download/song.mp3",
                    fileName = name,
                    fileSize = size,
                    lastModified = mtime,
                    metadata = AudioMetadata.Empty.copy(title = "Song", artist = "A", album = "Al"),
                ),
            ),
        )
        f.managedRoots.add(safUri, "SAF")

        useCase(f, scanner, reader, device).syncAll()

        assertEquals(1L, f.tracks.count(), "duplicate physical file should not appear twice")
        val survivor = f.tracks.findByDocumentUri(safDoc)
        assertNotNull(survivor, "SAF row must be the survivor")
        assertNull(f.tracks.findByDocumentUri(deviceDoc), "device-audio row must be skipped")
    }

    @Test
    fun `device-then-SAF dedup replaces device row with SAF row`() = runTest {
        val f = fixture()
        val safUri = "content://tree/safroot"
        val safDoc = "$safUri/song.mp3"
        val deviceDoc = "${DeviceAudioScanner.ROOT_URI}/Download/song.mp3"
        val name = "song.mp3"
        val size = 4_000_000L
        val mtime = 1_700_000_000L

        // Pre-seed the DB with the device-audio row by running device scan first.
        val device = FakeDeviceAudioScanner(
            tracks = listOf(
                DeviceAudioTrack(
                    sourceUri = deviceDoc,
                    relativePath = "Download/song.mp3",
                    fileName = name,
                    fileSize = size,
                    lastModified = mtime,
                    metadata = AudioMetadata.Empty.copy(title = "Song", artist = "A", album = "Al"),
                ),
            ),
        )
        useCase(f, FakeFolderScanner(scripted = emptyMap()), FakeMetadataReader(), device)
            .syncDeviceAudio()
        assertEquals(1L, f.tracks.count(), "device row should be present after first pass")

        // Now run a SAF scan that finds the same physical file.
        val scanner = FakeFolderScanner.scan(
            treeUri = safUri,
            folders = listOf(AudioFolder(safUri, "", "SAF", null)),
            files = listOf(RawAudioFile(safDoc, safUri, "song.mp3", name, size, mtime)),
        )
        val reader = FakeMetadataReader(
            scripted = mapOf(safDoc to md(artist = "A", album = "Al", title = "Song")),
        )
        useCase(f, scanner, reader, device).syncOne(safUri)

        assertEquals(1L, f.tracks.count(), "device row must be evicted when SAF covers the same file")
        assertNotNull(f.tracks.findByDocumentUri(safDoc))
        assertNull(f.tracks.findByDocumentUri(deviceDoc))
    }

    @Test
    fun `device-then-SAF dedup survives sub-second mtime precision mismatch`() = runTest {
        // Real-device scenario: MediaStore DATE_MODIFIED is whole seconds
        // (always ...000 after the *1000 conversion) while SAF's
        // COLUMN_LAST_MODIFIED carries sub-second millis. Same file, same
        // second, different raw values — this is the Downloads duplicate bug.
        val f = fixture()
        val safUri = "content://tree/safroot"
        val safDoc = "$safUri/song.mp3"
        val deviceDoc = "${DeviceAudioScanner.ROOT_URI}/Download/song.mp3"
        val name = "song.mp3"
        val size = 4_000_000L
        val deviceMtime = 1_700_000_000_000L // MediaStore: whole-second millis
        val safMtime = 1_700_000_000_123L    // SAF: same second, real millis

        val device = FakeDeviceAudioScanner(
            tracks = listOf(
                DeviceAudioTrack(
                    sourceUri = deviceDoc,
                    relativePath = "Download/song.mp3",
                    fileName = name,
                    fileSize = size,
                    lastModified = deviceMtime,
                    metadata = AudioMetadata.Empty.copy(title = "Song", artist = "A", album = "Al"),
                ),
            ),
        )
        useCase(f, FakeFolderScanner(scripted = emptyMap()), FakeMetadataReader(), device)
            .syncDeviceAudio()
        assertEquals(1L, f.tracks.count(), "device row should be present after first pass")

        val scanner = FakeFolderScanner.scan(
            treeUri = safUri,
            folders = listOf(AudioFolder(safUri, "", "SAF", null)),
            files = listOf(RawAudioFile(safDoc, safUri, "song.mp3", name, size, safMtime)),
        )
        val reader = FakeMetadataReader(
            scripted = mapOf(safDoc to md(artist = "A", album = "Al", title = "Song")),
        )
        useCase(f, scanner, reader, device).syncOne(safUri)

        assertEquals(1L, f.tracks.count(), "sub-second mtime mismatch must still dedup")
        assertNotNull(f.tracks.findByDocumentUri(safDoc))
        assertNull(f.tracks.findByDocumentUri(deviceDoc))
    }

    @Test
    fun `SAF-then-device dedup survives sub-second mtime precision mismatch`() = runTest {
        val f = fixture()
        val safUri = "content://tree/safroot"
        val safDoc = "$safUri/song.mp3"
        val deviceDoc = "${DeviceAudioScanner.ROOT_URI}/Download/song.mp3"
        val name = "song.mp3"
        val size = 4_000_000L

        val scanner = FakeFolderScanner.scan(
            treeUri = safUri,
            folders = listOf(AudioFolder(safUri, "", "SAF", null)),
            files = listOf(
                RawAudioFile(safDoc, safUri, "song.mp3", name, size, lastModified = 1_700_000_000_123L),
            ),
        )
        val reader = FakeMetadataReader(
            scripted = mapOf(safDoc to md(artist = "A", album = "Al", title = "Song")),
        )
        val device = FakeDeviceAudioScanner(
            tracks = listOf(
                DeviceAudioTrack(
                    sourceUri = deviceDoc,
                    relativePath = "Download/song.mp3",
                    fileName = name,
                    fileSize = size,
                    lastModified = 1_700_000_000_000L, // same second, no sub-second part
                    metadata = AudioMetadata.Empty.copy(title = "Song", artist = "A", album = "Al"),
                ),
            ),
        )
        f.managedRoots.add(safUri, "SAF")

        useCase(f, scanner, reader, device).syncAll()

        assertEquals(1L, f.tracks.count(), "sub-second mtime mismatch must still dedup")
        assertNotNull(f.tracks.findByDocumentUri(safDoc))
        assertNull(f.tracks.findByDocumentUri(deviceDoc))
    }

    @Test
    fun `near-match with different lastModified is treated as a distinct file`() = runTest {
        val f = fixture()
        val safUri = "content://tree/safroot"
        val safDoc = "$safUri/song.mp3"
        val deviceDoc = "${DeviceAudioScanner.ROOT_URI}/Download/song.mp3"
        val name = "song.mp3"
        val size = 4_000_000L

        val scanner = FakeFolderScanner.scan(
            treeUri = safUri,
            folders = listOf(AudioFolder(safUri, "", "SAF", null)),
            files = listOf(RawAudioFile(safDoc, safUri, "song.mp3", name, size, lastModified = 1_700_000_000L)),
        )
        val reader = FakeMetadataReader(
            scripted = mapOf(safDoc to md(artist = "A", album = "Al", title = "Song")),
        )
        val device = FakeDeviceAudioScanner(
            tracks = listOf(
                DeviceAudioTrack(
                    sourceUri = deviceDoc,
                    relativePath = "Download/song.mp3",
                    fileName = name,
                    fileSize = size,
                    lastModified = 1_800_000_000L, // different mtime
                    metadata = AudioMetadata.Empty.copy(title = "Song", artist = "A", album = "Al"),
                ),
            ),
        )
        f.managedRoots.add(safUri, "SAF")

        useCase(f, scanner, reader, device).syncAll()

        assertEquals(2L, f.tracks.count(), "different mtime means different physical file — keep both")
    }

    @Test
    fun `SAF scan skips files under an excluded folder`() = runTest {
        val f = fixture()
        val uri = "content://tree/root"
        f.excludedFolders.add(uri, "Voice Notes", "Voice Notes")

        val scanner = FakeFolderScanner.scan(
            treeUri = uri,
            folders = listOf(
                AudioFolder(uri, "", "Music", null),
                AudioFolder(uri, "Voice Notes", "Voice Notes", ""),
            ),
            files = listOf(
                RawAudioFile("$uri/song.mp3", uri, "song.mp3", "song.mp3", 1L, 1L),
                RawAudioFile("$uri/vn.mp3", uri, "Voice Notes/vn.mp3", "vn.mp3", 2L, 2L),
            ),
        )
        val reader = FakeMetadataReader(
            scripted = mapOf("$uri/song.mp3" to md(artist = "A", album = "Al", title = "Song")),
        )

        val report = useCase(f, scanner, reader).syncOne(uri)

        assertEquals(1, report.tracksDiscovered, "excluded file must not count as discovered")
        assertEquals(1L, f.tracks.count())
        assertNull(f.tracks.findByDocumentUri("$uri/vn.mp3"))
        assertNull(
            f.folders.findByPath(uri, "Voice Notes"),
            "excluded folder must not be materialized",
        )
    }

    @Test
    fun `device scan skips tracks under an excluded folder`() = runTest {
        val f = fixture()
        f.excludedFolders.add(DeviceAudioScanner.ROOT_URI, "WhatsApp/Audio", "Audio")

        val device = FakeDeviceAudioScanner(
            tracks = listOf(
                DeviceAudioTrack(
                    sourceUri = "${DeviceAudioScanner.ROOT_URI}/Music/keep.mp3",
                    relativePath = "Music/keep.mp3",
                    fileName = "keep.mp3", fileSize = 1L, lastModified = 1L,
                    metadata = AudioMetadata.Empty.copy(title = "Keep", artist = "A", album = "Al"),
                ),
                DeviceAudioTrack(
                    sourceUri = "${DeviceAudioScanner.ROOT_URI}/WhatsApp/Audio/note.opus",
                    relativePath = "WhatsApp/Audio/note.opus",
                    fileName = "note.opus", fileSize = 2L, lastModified = 2L,
                    metadata = AudioMetadata.Empty,
                ),
            ),
        )

        useCase(f, FakeFolderScanner(scripted = emptyMap()), FakeMetadataReader(), device)
            .syncDeviceAudio()

        assertEquals(1L, f.tracks.count())
        assertNull(f.tracks.findByDocumentUri("${DeviceAudioScanner.ROOT_URI}/WhatsApp/Audio/note.opus"))
    }

    @Test
    fun `re-scanning unchanged device tracks does not reprocess them`() = runTest {
        val f = fixture()
        val deviceDoc = "${DeviceAudioScanner.ROOT_URI}/Download/song.mp3"
        val device = FakeDeviceAudioScanner(
            tracks = listOf(
                DeviceAudioTrack(
                    sourceUri = deviceDoc,
                    relativePath = "Download/song.mp3",
                    fileName = "song.mp3",
                    fileSize = 4_000_000L,
                    lastModified = 1_700_000_000L,
                    metadata = AudioMetadata.Empty.copy(title = "Song", artist = "A", album = "Al"),
                ),
            ),
        )
        val uc = useCase(f, FakeFolderScanner(scripted = emptyMap()), FakeMetadataReader(), device)

        // First pass: the URI is new, so it gets inserted and classified.
        val first = uc.syncDeviceAudio()
        assertEquals(1, first.tracksScanned, "first pass scans the newly-discovered track")
        assertEquals(1L, f.tracks.count())
        assertEquals(
            ScanStatus.SCANNED,
            f.tracks.findByDocumentUri(deviceDoc)?.scanStatus,
            "track must be SCANNED after the first pass",
        )

        // Second pass over the identical device index — this is what happens on
        // every app foreground. The row is already SCANNED, so it must be
        // short-circuited: zero re-scanned, zero failed, no duplicate row.
        // Before the short-circuit this reported tracksScanned == 1 every time,
        // re-upserting the artist/album and rewriting the track on each resume.
        val second = uc.syncDeviceAudio()
        assertEquals(0, second.tracksScanned, "an unchanged, already-scanned track must not be re-scanned")
        assertEquals(0, second.tracksFailed, "an unchanged track must not be miscounted as failed")
        assertEquals(1L, f.tracks.count(), "re-scan must not create a duplicate row")
    }

    @Test
    fun `re-scanning device audio still retries a previously errored track`() = runTest {
        val f = fixture()
        val deviceDoc = "${DeviceAudioScanner.ROOT_URI}/Download/song.mp3"
        val track = DeviceAudioTrack(
            sourceUri = deviceDoc,
            relativePath = "Download/song.mp3",
            fileName = "song.mp3",
            fileSize = 4_000_000L,
            lastModified = 1_700_000_000L,
            metadata = AudioMetadata.Empty.copy(title = "Song", artist = "A", album = "Al"),
        )
        val device = FakeDeviceAudioScanner(tracks = listOf(track))
        val uc = useCase(f, FakeFolderScanner(scripted = emptyMap()), FakeMetadataReader(), device)

        // Scan once, then force the row into the ERROR state to simulate a
        // first pass that failed to read tags. A re-scan SHOULD pick it back up
        // (the short-circuit only skips SCANNED rows, never ERROR/PENDING).
        uc.syncDeviceAudio()
        val id = f.tracks.findByDocumentUri(deviceDoc)!!.id
        f.tracks.markError(id)
        assertEquals(ScanStatus.ERROR, f.tracks.findByDocumentUri(deviceDoc)?.scanStatus)

        val retry = uc.syncDeviceAudio()
        assertEquals(1, retry.tracksScanned, "an ERROR row must be retried on the next device scan")
        assertEquals(
            ScanStatus.SCANNED,
            f.tracks.findByDocumentUri(deviceDoc)?.scanStatus,
            "the retried track must end up SCANNED",
        )
    }

    @Test
    fun `scan populates canonical_genre on each scanned track`() = runTest {
        val f = fixture()
        val uri = "content://tree/root"
        val docRock = "$uri/r.mp3"
        val docJazz = "$uri/j.mp3"
        val docUnknown = "$uri/u.mp3"
        val scanner = FakeFolderScanner.scan(
            treeUri = uri,
            folders = listOf(AudioFolder(uri, "", "root", null)),
            files = listOf(
                RawAudioFile(docRock, uri, "r.mp3", "r.mp3", 0, 0),
                RawAudioFile(docJazz, uri, "j.mp3", "j.mp3", 0, 0),
                RawAudioFile(docUnknown, uri, "u.mp3", "u.mp3", 0, 0),
            ),
        )
        val reader = FakeMetadataReader(
            scripted = mapOf(
                docRock to md(artist = "A", album = "Al", title = "R").copy(genre = "Alternative Rock"),
                docJazz to md(artist = "A", album = "Al", title = "J").copy(genre = "Bebop"),
                docUnknown to md(artist = "A", album = "Al", title = "U"),
            ),
        )
        useCase(f, scanner, reader).syncOne(uri)
        assertEquals(CanonicalGenre.ROCK, f.tracks.findByDocumentUri(docRock)?.canonicalGenre)
        assertEquals(CanonicalGenre.JAZZ, f.tracks.findByDocumentUri(docJazz)?.canonicalGenre)
        assertEquals(CanonicalGenre.DEFAULT, f.tracks.findByDocumentUri(docUnknown)?.canonicalGenre)
    }

    @Test
    fun `syncAll backfills canonical_genre on legacy rows`() = runTest {
        val f = fixture()
        val uri = "content://tree/root"
        val doc = "$uri/legacy.mp3"
        // First scan populates the track normally (canonical_genre = ROCK).
        val scanner = FakeFolderScanner.scan(
            treeUri = uri,
            folders = listOf(AudioFolder(uri, "", "root", null)),
            files = listOf(RawAudioFile(doc, uri, "legacy.mp3", "legacy.mp3", 0, 0)),
        )
        val reader = FakeMetadataReader(
            scripted = mapOf(doc to md(artist = "A", album = "Al", title = "L").copy(genre = "Indie Rock")),
        )
        f.managedRoots.add(uri, "Root")
        useCase(f, scanner, reader).syncAll()
        val initial = f.tracks.findByDocumentUri(doc)
        assertNotNull(initial)
        assertEquals(CanonicalGenre.ROCK, initial.canonicalGenre)

        // Simulate the pre-EQ-feature state: stamp canonical_genre back to null.
        f.db.trackQueries.setCanonicalGenre(null, initial.id)
        assertEquals(null, f.tracks.findByDocumentUri(doc)?.canonicalGenre)

        // Next syncAll should observe the missing row and backfill it.
        useCase(f, FakeFolderScanner(scripted = emptyMap()), FakeMetadataReader()).syncAll()
        assertEquals(CanonicalGenre.ROCK, f.tracks.findByDocumentUri(doc)?.canonicalGenre)
    }

    @Test
    fun `multi-artist folder with no albumArtist collapses into one Various Artists album`() = runTest {
        val f = fixture()
        val uri = "content://tree/root"
        val scanner = FakeFolderScanner.scan(
            treeUri = uri,
            folders = listOf(
                AudioFolder(uri, "", "root", null),
                AudioFolder(uri, "comp", "comp", ""),
            ),
            files = listOf("a", "b", "c").map { n ->
                RawAudioFile("$uri/comp/$n.mp3", uri, "comp/$n.mp3", "$n.mp3", 0, 0)
            },
        )
        val reader = FakeMetadataReader(
            scripted = mapOf(
                "$uri/comp/a.mp3" to md(title = "S1", artist = "Artist A", album = "Now 100"),
                "$uri/comp/b.mp3" to md(title = "S2", artist = "Artist B", album = "Now 100"),
                "$uri/comp/c.mp3" to md(title = "S3", artist = "Artist C", album = "Now 100"),
            ),
        )

        useCase(f, scanner, reader).syncOne(uri)

        // The whole folder is ONE album, filed under Various Artists.
        val va = f.artists.findByName("Various Artists")
        assertNotNull(va, "a multi-artist album with no albumArtist must file under Various Artists")
        val albums = f.albums.observeByArtist(va.id).first()
        assertEquals(1, albums.size, "the compilation must be a single album, not one per artist")
        assertEquals("Now 100", albums.first().name)
        assertEquals(3, albums.first().trackCount)

        // Each track still shows its OWN artist name for display.
        val tracks = f.tracks.observeByAlbum(albums.first().id).first()
        assertEquals(
            setOf("Artist A", "Artist B", "Artist C"),
            tracks.map { it.artistName }.toSet(),
            "per-track artist names must be preserved even though the album is Various Artists",
        )
    }

    @Test
    fun `same album name by different artists in different folders stays separate`() = runTest {
        val f = fixture()
        val uri = "content://tree/root"
        val scanner = FakeFolderScanner.scan(
            treeUri = uri,
            folders = listOf(
                AudioFolder(uri, "", "root", null),
                AudioFolder(uri, "a", "a", ""),
                AudioFolder(uri, "b", "b", ""),
            ),
            files = listOf(
                RawAudioFile("$uri/a/x.mp3", uri, "a/x.mp3", "x.mp3", 0, 0),
                RawAudioFile("$uri/b/y.mp3", uri, "b/y.mp3", "y.mp3", 0, 0),
            ),
        )
        val reader = FakeMetadataReader(
            scripted = mapOf(
                "$uri/a/x.mp3" to md(title = "X", artist = "Alice", album = "Greatest Hits"),
                "$uri/b/y.mp3" to md(title = "Y", artist = "Bob", album = "Greatest Hits"),
            ),
        )

        useCase(f, scanner, reader).syncOne(uri)

        // Two single-artist albums that merely share a name must NOT be fused
        // into a Various Artists compilation — folder scoping keeps them apart.
        assertNull(
            f.artists.findByName("Various Artists"),
            "distinct single-artist albums must not be collapsed into a compilation",
        )
        val alice = f.artists.findByName("Alice")
        val bob = f.artists.findByName("Bob")
        assertNotNull(alice)
        assertNotNull(bob)
        assertEquals(1, f.albums.observeByArtist(alice.id).first().size)
        assertEquals(1, f.albums.observeByArtist(bob.id).first().size)
    }

    @Test
    fun `single-artist album with guest-featured tracks is not a compilation`() = runTest {
        val f = fixture()
        val uri = "content://tree/root"
        // 4 tracks, no albumArtist tag: three plain "Phil Collins" and one
        // "Phil Collins feat. Philip Bailey". The dominant artist owns the
        // album, so it must NOT be flagged as a Various Artists compilation.
        val scanner = FakeFolderScanner.scan(
            treeUri = uri,
            folders = listOf(
                AudioFolder(uri, "", "root", null),
                AudioFolder(uri, "nojacket", "nojacket", ""),
            ),
            files = listOf("1", "2", "3", "4").map { n ->
                RawAudioFile("$uri/nojacket/$n.mp3", uri, "nojacket/$n.mp3", "$n.mp3", 0, 0)
            },
        )
        val reader = FakeMetadataReader(
            scripted = mapOf(
                "$uri/nojacket/1.mp3" to md(title = "Sussudio", artist = "Phil Collins", album = "No Jacket Required"),
                "$uri/nojacket/2.mp3" to md(title = "Only You", artist = "Phil Collins", album = "No Jacket Required"),
                "$uri/nojacket/3.mp3" to md(title = "One More Night", artist = "Phil Collins", album = "No Jacket Required"),
                "$uri/nojacket/4.mp3" to md(
                    title = "Who Said I Would",
                    artist = "Phil Collins feat. Philip Bailey",
                    album = "No Jacket Required",
                ),
            ),
        )

        useCase(f, scanner, reader).syncOne(uri)

        assertNull(
            f.artists.findByName("Various Artists"),
            "a single-artist album with one guest track must not become a compilation",
        )
        val phil = f.artists.findByName("Phil Collins")
        assertNotNull(phil, "the album must be filed under its dominant artist")
        val albums = f.albums.observeByArtist(phil.id).first()
        assertEquals(1, albums.size)
        assertEquals("No Jacket Required", albums.first().name)
        assertEquals(4, albums.first().trackCount)
    }

    private fun md(
        title: String? = null,
        artist: String? = null,
        albumArtist: String? = null,
        album: String? = null,
        year: Int? = null,
    ) = AudioMetadata(
        title = title, artist = artist, albumArtist = albumArtist, album = album,
        genre = null, year = year, trackNumber = null, discNumber = null,
        durationMs = null, bitrate = null, sampleRate = null, channels = null,
        codec = null,
    )
}

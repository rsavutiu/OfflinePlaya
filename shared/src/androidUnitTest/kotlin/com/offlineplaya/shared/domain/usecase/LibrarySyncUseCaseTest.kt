package com.offlineplaya.shared.domain.usecase

import com.offlineplaya.shared.domain.model.CanonicalGenre
import com.offlineplaya.shared.data.repository.SqlAlbumRepository
import com.offlineplaya.shared.data.repository.SqlArtistRepository
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

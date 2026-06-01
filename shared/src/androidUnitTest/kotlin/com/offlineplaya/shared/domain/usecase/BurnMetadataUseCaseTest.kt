package com.offlineplaya.shared.domain.usecase

import com.offlineplaya.shared.data.repository.SqlAlbumRepository
import com.offlineplaya.shared.data.repository.SqlArtistRepository
import com.offlineplaya.shared.data.repository.SqlFolderRepository
import com.offlineplaya.shared.data.repository.SqlManagedTreeRootRepository
import com.offlineplaya.shared.data.repository.SqlTrackRepository
import com.offlineplaya.shared.domain.genre.GenreTagWriter
import com.offlineplaya.shared.domain.genre.RemoteGenreSource
import com.offlineplaya.shared.domain.image.AlbumArtWriter
import com.offlineplaya.shared.domain.image.FolderArtSource
import com.offlineplaya.shared.domain.image.RemoteArtSource
import com.offlineplaya.shared.domain.model.CanonicalGenre
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.scanner.DeviceAudioScanner
import com.offlineplaya.shared.testsupport.createInMemoryDatabase
import com.offlineplaya.shared.util.TestLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Behaviour tests for [BurnMetadataUseCase]. The use case is the brain of
 * the burn-metadata feature — it walks the library, decides what to fetch
 * from where, fans album processing out across coroutines, and reports
 * progress as it goes. These tests pin down the contracts a user cares
 * about:
 *
 *  - Writes counted accurately under success and failure paths.
 *  - Tracks with unknown artist or album are silently skipped, not counted
 *    as failures.
 *  - Final processed = total so the progress bar lands at 100 %.
 *  - device://audio tracks (synthetic MediaStore entries) are never written.
 *  - treeUriFilter scopes the pass to a single managed root.
 *  - Sidecar art beats remote when both exist.
 *  - Albums process concurrently (parallel fan-out across coroutines).
 *  - Genre write failures don't persist the canonical-genre side-effect.
 *
 * Tracks are inserted into a real in-memory SQLDelight database via the
 * same Sql* repositories the production app uses, so we exercise the real
 * `observeAll()` flow and the real `setRawAndCanonicalGenre` write path.
 * The network + filesystem collaborators are stubbed with hand-written
 * fakes so failures and timings are deterministic.
 */
class BurnMetadataUseCaseTest {

    // ── Fakes for the network + filesystem collaborators ──────────────────

    private class FakeArtSource(
        /** (artist, album) → JPEG bytes (null = "no match found"). */
        private val map: Map<Pair<String, String>, ByteArray?> = emptyMap(),
        /** Optional pre-resolve hook so tests can observe concurrent calls. */
        private val onResolve: (suspend (String, String) -> Unit)? = null,
    ) : RemoteArtSource {
        val calls = mutableListOf<Pair<String, String>>()
        override suspend fun resolve(artist: String, album: String): ByteArray? {
            onResolve?.invoke(artist, album)
            calls += artist to album
            return map[artist to album]
        }

        override suspend fun resolveArtistImage(artist: String): String? = null
    }

    private class FakeFolderArtSource(
        private val perTrack: Map<String, ByteArray?> = emptyMap(),
    ) : FolderArtSource {
        override suspend fun findInFolder(track: Track): ByteArray? = perTrack[track.documentUri]
    }

    private class FakeArtWriter(
        /** Tracks the writer should report as already having art (use case skips them). */
        private val alreadyHas: Set<String> = emptySet(),
        /** Tracks where the write call should report failure. */
        private val failWrites: Set<String> = emptySet(),
    ) : AlbumArtWriter {
        val writes = mutableListOf<Pair<String, ByteArray>>()
        override suspend fun hasEmbeddedArt(documentUri: String): Boolean =
            documentUri in alreadyHas

        override suspend fun write(documentUri: String, jpegBytes: ByteArray): Result<Unit> {
            writes += documentUri to jpegBytes
            return if (documentUri in failWrites) Result.failure(RuntimeException("write failed"))
            else Result.success(Unit)
        }
    }

    private class FakeGenreSource(
        private val map: Map<Pair<String, String>, String?> = emptyMap(),
    ) : RemoteGenreSource {
        val calls = mutableListOf<Pair<String, String>>()
        override suspend fun resolveGenre(artist: String, album: String): String? {
            calls += artist to album
            return map[artist to album]
        }
    }

    private class FakeGenreWriter(
        private val failWrites: Set<String> = emptySet(),
    ) : GenreTagWriter {
        val writes = mutableListOf<Pair<String, String>>()
        override suspend fun hasGenreTag(documentUri: String): Boolean = false
        override suspend fun writeGenre(documentUri: String, genre: String): Result<Unit> {
            writes += documentUri to genre
            return if (documentUri in failWrites) Result.failure(RuntimeException("genre write failed"))
            else Result.success(Unit)
        }
    }

    // ── Database harness ─────────────────────────────────────────────────

    /**
     * Builds a fresh in-memory database, inserts each [TrackSeed], and
     * returns a fully-wired [BurnMetadataUseCase] pointing at the same
     * track repository — so the use case observes real rows and writes
     * back through real SQL.
     */
    private suspend fun harness(
        seeds: List<TrackSeed>,
        artSource: RemoteArtSource = FakeArtSource(),
        folderArtSource: FolderArtSource = FakeFolderArtSource(),
        artWriter: AlbumArtWriter = FakeArtWriter(),
        genreSource: RemoteGenreSource = FakeGenreSource(),
        genreWriter: GenreTagWriter = FakeGenreWriter(),
    ): Harness {
        val db = createInMemoryDatabase()
        val tracksRepo = SqlTrackRepository(db, TestLogger(), Dispatchers.Unconfined)
        val artistsRepo = SqlArtistRepository(db, TestLogger(), Dispatchers.Unconfined)
        val albumsRepo = SqlAlbumRepository(db, TestLogger(), Dispatchers.Unconfined)
        val foldersRepo = SqlFolderRepository(db, TestLogger(), Dispatchers.Unconfined)
        // ManagedRoots not used by the burn flow but satisfies FK consistency
        // for any future code paths.
        SqlManagedTreeRootRepository(db, Dispatchers.Unconfined)

        // Insert seeds: one folder per seed (good enough — burn doesn't care),
        // then the track row with metadata populated as if a sync had run.
        for (seed in seeds) {
            val artistId = artistsRepo.upsert(seed.artist)
            val albumId = albumsRepo.upsert(seed.album, artistId, year = null)
            val folderId = foldersRepo.upsert(
                treeUri = seed.treeUri,
                relativePath = "",
                displayName = "Folder",
                parentId = null,
            )
            val trackId = tracksRepo.insertFile(
                documentUri = seed.documentUri,
                treeUri = seed.treeUri,
                relativePath = "${seed.artist}/${seed.album}/${seed.title}.flac",
                fileName = "${seed.title}.flac",
                fileSize = 1_000_000L,
                lastModified = 1L,
                folderId = folderId,
            )
            val existing = tracksRepo.findById(trackId)!!
            tracksRepo.updateMetadata(
                existing.copy(
                    title = seed.title,
                    artistName = seed.artist,
                    albumName = seed.album,
                    genre = seed.genre,
                ),
            )
            tracksRepo.updateForeignKeys(trackId, artistId, albumId)
        }

        val useCase = BurnMetadataUseCase(
            tracks = tracksRepo,
            artSource = artSource,
            folderArtSource = folderArtSource,
            artWriter = artWriter,
            genreSource = genreSource,
            genreWriter = genreWriter,
            logger = TestLogger(),
        )
        return Harness(useCase, tracksRepo)
    }

    private data class TrackSeed(
        val documentUri: String,
        val treeUri: String = "content://tree/music",
        val artist: String = "Pearl Jam",
        val album: String = "Ten",
        val title: String,
        val genre: String? = null,
    )

    private class Harness(
        val useCase: BurnMetadataUseCase,
        val tracksRepo: SqlTrackRepository,
    )

    private fun seed(
        id: Int, artist: String = "Pearl Jam", album: String = "Ten",
        title: String = "Track $id", genre: String? = null,
        treeUri: String = "content://tree/music"
    ) = TrackSeed(
        documentUri = "$treeUri/track-$id.flac",
        treeUri = treeUri,
        artist = artist,
        album = album,
        title = title,
        genre = genre,
    )

    // ── Tests ────────────────────────────────────────────────────────────

    @Test
    fun `empty library reports zero processed, zero failed`() = runTest {
        val h = harness(seeds = emptyList())

        val report = h.useCase(onProgress = {})

        assertIs<EmbedReport.Completed>(report)
        assertEquals(0, report.processed)
        assertEquals(0, report.embedded)
        assertEquals(0, report.failed)
    }

    @Test
    fun `unknown artist or album tracks are skipped but counted toward processed total`() =
        runTest {
            val artWriter = FakeArtWriter()
            val h = harness(
                seeds = listOf(
                    seed(1, artist = "Unknown Artist", album = "Some Album"),
                    seed(2, artist = "Pearl Jam", album = "Unknown Album"),
                    seed(3, artist = "Unknown Artist", album = "Unknown Album"),
                ),
                artWriter = artWriter,
            )

            val report = h.useCase(onProgress = {}) as EmbedReport.Completed

            // Final processed must equal total so the UI progress bar lands at 100 %.
            assertEquals(3, report.processed)
            assertEquals(0, report.embedded)
            assertEquals(0, report.failed)
            assertTrue(artWriter.writes.isEmpty(), "no writes expected for Unknown tracks")
        }

    @Test
    fun `art is written once per track and album lookup is deduped`() = runTest {
        val artBytes = byteArrayOf(1, 2, 3, 4)
        val artSource = FakeArtSource(map = mapOf(("Pearl Jam" to "Ten") to artBytes))
        val artWriter = FakeArtWriter()
        val h = harness(
            seeds = listOf(seed(1), seed(2), seed(3)),
            artSource = artSource,
            artWriter = artWriter,
        )

        val report = h.useCase(onProgress = {}) as EmbedReport.Completed

        assertEquals(3, report.processed)
        assertEquals(3, report.embedded)
        assertEquals(0, report.failed)
        // Same album → one MusicBrainz round-trip, three file writes.
        assertEquals(1, artSource.calls.size, "remote art lookup must dedupe across album")
        assertEquals(3, artWriter.writes.size)
        artWriter.writes.forEach { (_, bytes) ->
            assertTrue(bytes.contentEquals(artBytes), "writer received the resolved bytes verbatim")
        }
    }

    @Test
    fun `sidecar art beats remote when folder source returns bytes`() = runTest {
        val sidecar = byteArrayOf(42)
        val remote = byteArrayOf(99)
        val seeds = listOf(seed(1))
        val folderArt = FakeFolderArtSource(mapOf(seeds[0].documentUri to sidecar))
        val remoteArt = FakeArtSource(map = mapOf(("Pearl Jam" to "Ten") to remote))
        val artWriter = FakeArtWriter()
        val h = harness(
            seeds = seeds,
            artSource = remoteArt,
            folderArtSource = folderArt,
            artWriter = artWriter,
        )

        h.useCase(onProgress = {})

        assertEquals(0, remoteArt.calls.size, "sidecar found — remote must not be called")
        assertEquals(1, artWriter.writes.size)
        assertTrue(artWriter.writes[0].second.contentEquals(sidecar))
    }

    @Test
    fun `genre lookup writes tag and persists canonical classification`() = runTest {
        val seeds = listOf(seed(1, genre = null), seed(2, genre = null))
        val genreSource = FakeGenreSource(mapOf(("Pearl Jam" to "Ten") to "Rock"))
        val genreWriter = FakeGenreWriter()
        val h = harness(
            seeds = seeds,
            artWriter = FakeArtWriter(alreadyHas = seeds.map { it.documentUri }.toSet()),
            genreSource = genreSource,
            genreWriter = genreWriter,
        )

        val report = h.useCase(onProgress = {}) as EmbedReport.Completed

        assertEquals(2, report.embedded)
        assertEquals(2, genreWriter.writes.size)
        // Canonical classification stored on every track that got the tag.
        val updated = h.tracksRepo.findPending(Int.MAX_VALUE) // empty — they're scanned
        // Verify by re-reading each row:
        for (seed in seeds) {
            val row = h.tracksRepo.findByDocumentUri(seed.documentUri)!!
            assertEquals("Rock", row.genre)
            assertEquals(CanonicalGenre.ROCK, row.canonicalGenre)
        }
    }

    @Test
    fun `art write failure is counted as failed without success side effects`() = runTest {
        val seeds = listOf(seed(1), seed(2))
        val artWriter = FakeArtWriter(failWrites = setOf(seeds[0].documentUri))
        val h = harness(
            seeds = seeds,
            artSource = FakeArtSource(map = mapOf(("Pearl Jam" to "Ten") to byteArrayOf(1, 2))),
            artWriter = artWriter,
        )

        val report = h.useCase(onProgress = {}) as EmbedReport.Completed

        assertEquals(2, report.processed)
        assertEquals(1, report.embedded)
        assertEquals(1, report.failed)
    }

    @Test
    fun `genre write failure does not persist canonical classification`() = runTest {
        val seeds = listOf(seed(1, genre = null))
        val h = harness(
            seeds = seeds,
            artWriter = FakeArtWriter(alreadyHas = setOf(seeds[0].documentUri)),
            genreSource = FakeGenreSource(mapOf(("Pearl Jam" to "Ten") to "Rock")),
            genreWriter = FakeGenreWriter(failWrites = setOf(seeds[0].documentUri)),
        )

        val report = h.useCase(onProgress = {}) as EmbedReport.Completed

        assertEquals(0, report.embedded)
        assertEquals(1, report.failed)
        // Critical invariant: don't lie to the rest of the app about a tag
        // that didn't actually land on disk.
        val row = h.tracksRepo.findByDocumentUri(seeds[0].documentUri)!!
        assertEquals(null, row.canonicalGenre, "no canonical genre stored when write fails")
    }

    @Test
    fun `device-audio tracks are excluded from writes by default`() = runTest {
        val safSeed = seed(1, treeUri = "content://tree/music")
        val deviceSeed = seed(2, treeUri = DeviceAudioScanner.ROOT_URI)
        val artWriter = FakeArtWriter()
        val h = harness(
            seeds = listOf(safSeed, deviceSeed),
            artSource = FakeArtSource(map = mapOf(("Pearl Jam" to "Ten") to byteArrayOf(1))),
            artWriter = artWriter,
        )

        h.useCase(onProgress = {})

        // Only the SAF track is touched — the synthetic device-audio entry
        // is intentionally read-only.
        assertEquals(1, artWriter.writes.size)
        assertEquals(safSeed.documentUri, artWriter.writes[0].first)
    }

    @Test
    fun `treeUriFilter scopes the pass to a single managed root`() = runTest {
        val rootA = "content://tree/root-a"
        val rootB = "content://tree/root-b"
        val seeds = listOf(
            seed(1, treeUri = rootA),
            seed(2, treeUri = rootA),
            seed(3, treeUri = rootB),
        )
        val artWriter = FakeArtWriter()
        val h = harness(
            seeds = seeds,
            artSource = FakeArtSource(map = mapOf(("Pearl Jam" to "Ten") to byteArrayOf(1))),
            artWriter = artWriter,
        )

        h.useCase(onProgress = {}, treeUriFilter = rootB)

        assertEquals(1, artWriter.writes.size, "only tracks under rootB should be written")
        assertEquals(seeds[2].documentUri, artWriter.writes[0].first)
    }

    @Test
    fun `progress callback ends with processed equal to total`() = runTest {
        val seenReports = mutableListOf<EmbedReport.Running>()
        val h = harness(
            seeds = listOf(seed(1), seed(2), seed(3)),
            artSource = FakeArtSource(map = mapOf(("Pearl Jam" to "Ten") to byteArrayOf(1))),
            artWriter = FakeArtWriter(),
        )

        h.useCase(onProgress = { seenReports += it })

        assertTrue(seenReports.isNotEmpty(), "use case must emit at least one progress update")
        val last = seenReports.last()
        assertEquals(3, last.total)
        assertEquals(3, last.processed, "final progress event must land exactly on total")
    }

    @Test
    fun `albums process concurrently — multiple fetches in flight at once`() = runTest {
        val overlap = OverlapTracker()
        val slowArt = FakeArtSource(
            map = mapOf(
                ("Pearl Jam" to "Ten") to byteArrayOf(1),
                ("Pearl Jam" to "Vs.") to byteArrayOf(2),
                ("Pearl Jam" to "Vitalogy") to byteArrayOf(3),
                ("Pearl Jam" to "No Code") to byteArrayOf(4),
            ),
            onResolve = { _, _ ->
                overlap.enter()
                try {
                    delay(50)
                } finally {
                    overlap.exit()
                }
            },
        )
        val h = harness(
            seeds = listOf(
                seed(1, album = "Ten"),
                seed(2, album = "Vs."),
                seed(3, album = "Vitalogy"),
                seed(4, album = "No Code"),
            ),
            artSource = slowArt,
            artWriter = FakeArtWriter(),
        )

        h.useCase(onProgress = {})

        assertTrue(
            overlap.maxOverlap >= 2,
            "expected ≥2 album fetches in flight at once; saw peak ${overlap.maxOverlap}",
        )
    }

    /**
     * Tracks the peak number of concurrent enter()/exit() pairs. Used by
     * the concurrency test above to assert that fan-out actually happens.
     */
    private class OverlapTracker {
        private val mutex = Mutex()
        private var current = 0
        var maxOverlap: Int = 0
            private set

        suspend fun enter() = mutex.withLock {
            current += 1
            if (current > maxOverlap) maxOverlap = current
        }

        suspend fun exit() = mutex.withLock { current -= 1 }
    }
}

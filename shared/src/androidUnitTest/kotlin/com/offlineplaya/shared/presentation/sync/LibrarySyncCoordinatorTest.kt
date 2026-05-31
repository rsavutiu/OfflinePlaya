package com.offlineplaya.shared.presentation.sync

import com.offlineplaya.shared.data.repository.SqlAlbumRepository
import com.offlineplaya.shared.data.repository.SqlArtistRepository
import com.offlineplaya.shared.data.repository.SqlFolderRepository
import com.offlineplaya.shared.data.repository.SqlManagedTreeRootRepository
import com.offlineplaya.shared.data.repository.SqlTrackRepository
import com.offlineplaya.shared.domain.scanner.AudioFolder
import com.offlineplaya.shared.domain.scanner.AudioMetadata
import com.offlineplaya.shared.domain.scanner.RawAudioFile
import com.offlineplaya.shared.domain.usecase.LibrarySyncUseCase
import com.offlineplaya.shared.testsupport.FakeDeviceAudioScanner
import com.offlineplaya.shared.testsupport.FakeFolderScanner
import com.offlineplaya.shared.testsupport.FakeMetadataReader
import com.offlineplaya.shared.testsupport.createInMemoryDatabase
import com.offlineplaya.shared.util.TestLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class LibrarySyncCoordinatorTest {

    private class Harness(
        val coordinator: LibrarySyncCoordinator,
        val managedRoots: SqlManagedTreeRootRepository,
    )

    private fun harness(
        testScope: CoroutineScope,
        scanner: FakeFolderScanner,
        reader: FakeMetadataReader,
    ): Harness {
        val db = createInMemoryDatabase()
        val managedRoots = SqlManagedTreeRootRepository(db, Dispatchers.Unconfined)
        val folders = SqlFolderRepository(db, TestLogger(), Dispatchers.Unconfined)
        val artists = SqlArtistRepository(db, TestLogger(), Dispatchers.Unconfined)
        val albums = SqlAlbumRepository(db, TestLogger(), Dispatchers.Unconfined)
        val tracks = SqlTrackRepository(db, TestLogger(), Dispatchers.Unconfined)
        val useCase = LibrarySyncUseCase(
            managedRoots, folders, artists, albums, tracks, scanner, reader,
            FakeDeviceAudioScanner(),
            TestLogger(),
        )
        return Harness(
            LibrarySyncCoordinator(
                syncUseCase = useCase,
                managedRoots = managedRoots,
                tracks = tracks,
                folders = folders,
                artists = artists,
                albums = albums,
                scope = testScope,
            ),
            managedRoots,
        )
    }

    @Test
    fun `addAndSync transitions to Completed on success`() = runTest {
        val uri = "content://tree/root"
        val docUri = "$uri/song.flac"
        val scanner = FakeFolderScanner.scan(
            treeUri = uri,
            folders = listOf(AudioFolder(uri, "", "Music", null)),
            files = listOf(RawAudioFile(docUri, uri, "song.flac", "song.flac", 0L, 0L)),
        )
        val reader = FakeMetadataReader(
            scripted = mapOf(
                docUri to AudioMetadata.Empty.copy(
                    title = "Once", artist = "Pearl Jam", album = "Ten",
                ),
            ),
        )
        val scope = CoroutineScope(coroutineContext + UnconfinedTestDispatcher(testScheduler))
        val h = harness(scope, scanner, reader)

        assertEquals(SyncStatus.Idle, h.coordinator.status.value)

        h.coordinator.addAndSync(uri, "Music").join()

        val final = h.coordinator.status.value
        assertIs<SyncStatus.Completed>(final)
        assertEquals(1, final.report.tracksScanned)
        assertEquals(1, final.report.foldersUpserted)
    }

    @Test
    fun `addAndSync registers the tree as a managed root and marks it scanned`() = runTest {
        val uri = "content://tree/newroot"
        val scope = CoroutineScope(coroutineContext + UnconfinedTestDispatcher(testScheduler))
        val h = harness(
            testScope = scope,
            scanner = FakeFolderScanner.empty(uri, "NewRoot"),
            reader = FakeMetadataReader(),
        )

        h.coordinator.addAndSync(uri, "NewRoot").join()

        val stored = h.managedRoots.findByUri(uri)
        assertNotNull(stored)
        assertEquals("NewRoot", stored.displayName)
        assertNotNull(stored.lastScannedAt)
    }

    @Test
    fun `addAndSync emits AlreadyAdded when the URI is already a managed root`() = runTest {
        val uri = "content://tree/dupe"
        val scope = CoroutineScope(coroutineContext + UnconfinedTestDispatcher(testScheduler))
        val h = harness(
            testScope = scope,
            scanner = FakeFolderScanner.empty(uri, "Music"),
            reader = FakeMetadataReader(),
        )

        h.coordinator.addAndSync(uri, "Music").join()
        h.coordinator.addAndSync(uri, "Music").join()

        val final = h.coordinator.status.value
        assertIs<SyncStatus.AlreadyAdded>(final)
        assertEquals(uri, final.treeUri)
        assertEquals("Music", final.displayName)
    }

    @Test
    fun `addAndSync rejects a subfolder of an already-added root`() = runTest {
        val parent = "content://com.android.externalstorage.documents/tree/primary%3AMusic"
        val child = "$parent%2FAlbums"
        val scope = CoroutineScope(coroutineContext + UnconfinedTestDispatcher(testScheduler))
        val h = harness(
            testScope = scope,
            scanner = FakeFolderScanner.empty(parent, "Music"),
            reader = FakeMetadataReader(),
        )

        h.coordinator.addAndSync(parent, "Music").join()
        h.coordinator.addAndSync(child, "Albums").join()

        val final = h.coordinator.status.value
        assertIs<SyncStatus.AlreadyAdded>(final)
        assertEquals(parent, final.treeUri)
        assertEquals("Music", final.displayName)
    }

    @Test
    fun `addAndSync rejects a parent of an already-added root`() = runTest {
        val parent = "content://com.android.externalstorage.documents/tree/primary%3AMusic"
        val child = "$parent%2FAlbums"
        val scope = CoroutineScope(coroutineContext + UnconfinedTestDispatcher(testScheduler))
        val h = harness(
            testScope = scope,
            scanner = FakeFolderScanner.empty(child, "Albums"),
            reader = FakeMetadataReader(),
        )

        h.coordinator.addAndSync(child, "Albums").join()
        h.coordinator.addAndSync(parent, "Music").join()

        val final = h.coordinator.status.value
        assertIs<SyncStatus.AlreadyAdded>(final)
        assertEquals(child, final.treeUri)
        assertEquals("Albums", final.displayName)
    }

    @Test
    fun `addAndSync accepts a sibling that shares a prefix but not a path boundary`() = runTest {
        // "Music" vs "MusicVideos" — the latter is NOT under the former.
        val music = "content://com.android.externalstorage.documents/tree/primary%3AMusic"
        val videos = "content://com.android.externalstorage.documents/tree/primary%3AMusicVideos"
        val scope = CoroutineScope(coroutineContext + UnconfinedTestDispatcher(testScheduler))
        val scanner = FakeFolderScanner(
            scripted = mapOf(
                music to com.offlineplaya.shared.domain.scanner.ScanResult(
                    folders = listOf(AudioFolder(music, "", "Music", null)),
                    files = emptyList(),
                ),
                videos to com.offlineplaya.shared.domain.scanner.ScanResult(
                    folders = listOf(AudioFolder(videos, "", "Videos", null)),
                    files = emptyList(),
                ),
            ),
        )
        val h = harness(testScope = scope, scanner = scanner, reader = FakeMetadataReader())

        h.coordinator.addAndSync(music, "Music").join()
        h.coordinator.addAndSync(videos, "Videos").join()

        val final = h.coordinator.status.value
        assertIs<SyncStatus.Completed>(final)
    }

    @Test
    fun `addAndSync rejects a file URI subfolder of an already-added file URI root`() = runTest {
        val parent = "file:///storage/emulated/0/Music"
        val child = "file:///storage/emulated/0/Music/Albums"
        val scope = CoroutineScope(coroutineContext + UnconfinedTestDispatcher(testScheduler))
        val h = harness(
            testScope = scope,
            scanner = FakeFolderScanner.empty(parent, "Music"),
            reader = FakeMetadataReader(),
        )

        h.coordinator.addAndSync(parent, "Music").join()
        h.coordinator.addAndSync(child, "Albums").join()

        val final = h.coordinator.status.value
        assertIs<SyncStatus.AlreadyAdded>(final)
        assertEquals(parent, final.treeUri)
    }

    @Test
    fun `addAndSync rejects a file URI root that overlaps an existing SAF primary root`() =
        runTest {
            // SAF primary:Music and file:///storage/emulated/0/Music point at the same
            // physical folder — without normalisation each scan would insert a fresh set
            // of Track rows with different document_uri values, duplicating every album.
            val safPrimary = "content://com.android.externalstorage.documents/tree/primary%3AMusic"
            val fileRoot = "file:///storage/emulated/0/Music"
            val scope = CoroutineScope(coroutineContext + UnconfinedTestDispatcher(testScheduler))
            val h = harness(
                testScope = scope,
                scanner = FakeFolderScanner.empty(safPrimary, "Music"),
                reader = FakeMetadataReader(),
            )

            h.coordinator.addAndSync(safPrimary, "Music").join()
            h.coordinator.addAndSync(fileRoot, "Music").join()

            val final = h.coordinator.status.value
            assertIs<SyncStatus.AlreadyAdded>(final)
            assertEquals(safPrimary, final.treeUri)
        }

    @Test
    fun `addAndSync accepts a file URI sibling that shares a prefix but not a path boundary`() =
        runTest {
            val music = "file:///storage/emulated/0/Music"
            val videos = "file:///storage/emulated/0/MusicVideos"
            val scope = CoroutineScope(coroutineContext + UnconfinedTestDispatcher(testScheduler))
            val scanner = FakeFolderScanner(
                scripted = mapOf(
                    music to com.offlineplaya.shared.domain.scanner.ScanResult(
                        folders = listOf(AudioFolder(music, "", "Music", null)),
                        files = emptyList(),
                    ),
                    videos to com.offlineplaya.shared.domain.scanner.ScanResult(
                        folders = listOf(AudioFolder(videos, "", "MusicVideos", null)),
                        files = emptyList(),
                    ),
                ),
            )
            val h = harness(testScope = scope, scanner = scanner, reader = FakeMetadataReader())

            h.coordinator.addAndSync(music, "Music").join()
            h.coordinator.addAndSync(videos, "MusicVideos").join()

        val final = h.coordinator.status.value
        assertIs<SyncStatus.Completed>(final)
    }

    @Test
    fun `removeManagedRoot cascades to tracks and folders for that tree`() = runTest {
        val uri = "content://tree/removable"
        val docUri = "$uri/track.mp3"
        val scanner = FakeFolderScanner.scan(
            treeUri = uri,
            folders = listOf(AudioFolder(uri, "", "Removable", null)),
            files = listOf(RawAudioFile(docUri, uri, "track.mp3", "track.mp3", 100L, 200L)),
        )
        val reader = FakeMetadataReader(
            scripted = mapOf(
                docUri to AudioMetadata.Empty.copy(title = "T", artist = "A", album = "B"),
            ),
        )
        val scope = CoroutineScope(coroutineContext + UnconfinedTestDispatcher(testScheduler))
        val db = createInMemoryDatabase()
        val managedRoots = SqlManagedTreeRootRepository(db, Dispatchers.Unconfined)
        val folders = SqlFolderRepository(db, TestLogger(), Dispatchers.Unconfined)
        val artists = SqlArtistRepository(db, TestLogger(), Dispatchers.Unconfined)
        val albums = SqlAlbumRepository(db, TestLogger(), Dispatchers.Unconfined)
        val tracks = SqlTrackRepository(db, TestLogger(), Dispatchers.Unconfined)
        val useCase = LibrarySyncUseCase(
            managedRoots, folders, artists, albums, tracks, scanner, reader,
            FakeDeviceAudioScanner(), TestLogger(),
        )
        val coordinator = LibrarySyncCoordinator(
            useCase, managedRoots, tracks, folders, artists, albums, scope,
        )

        coordinator.addAndSync(uri, "Removable").join()
        assertEquals(1, tracks.count())
        assertNotNull(tracks.findByDocumentUri(docUri))

        coordinator.removeManagedRoot(uri).join()

        assertEquals(0, tracks.count())
        assertEquals(null, managedRoots.findByUri(uri))
    }

    @Test
    fun `unscripted URI lands in Completed-empty state without crashing the scope`() = runTest {
        // The use case swallows scanner failures and returns SyncReport.Empty;
        // we just need to verify the coordinator scope survives.
        val scope = CoroutineScope(coroutineContext + UnconfinedTestDispatcher(testScheduler))
        val h = harness(
            testScope = scope,
            scanner = FakeFolderScanner(scripted = emptyMap()),
            reader = FakeMetadataReader(),
        )

        h.coordinator.addAndSync("content://nope", "Nope").join()

        val final = h.coordinator.status.value
        assertIs<SyncStatus.Completed>(final)
        assertEquals(0, final.report.tracksDiscovered)
    }
}

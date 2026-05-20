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
        val folders = SqlFolderRepository(db, Dispatchers.Unconfined)
        val artists = SqlArtistRepository(db, Dispatchers.Unconfined)
        val albums = SqlAlbumRepository(db, Dispatchers.Unconfined)
        val tracks = SqlTrackRepository(db, Dispatchers.Unconfined)
        val useCase = LibrarySyncUseCase(
            managedRoots, folders, artists, albums, tracks, scanner, reader,
            FakeDeviceAudioScanner(),
        )
        return Harness(LibrarySyncCoordinator(useCase, managedRoots, testScope), managedRoots)
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
    fun `unscripted URI lands in Failed state without crashing the scope`() = runTest {
        val scope = CoroutineScope(coroutineContext + UnconfinedTestDispatcher(testScheduler))
        val h = harness(
            testScope = scope,
            scanner = FakeFolderScanner(scripted = emptyMap()),
            reader = FakeMetadataReader(),
        )

        h.coordinator.addAndSync("content://nope", "Nope").join()

        assertIs<SyncStatus.Failed>(h.coordinator.status.value)
    }
}

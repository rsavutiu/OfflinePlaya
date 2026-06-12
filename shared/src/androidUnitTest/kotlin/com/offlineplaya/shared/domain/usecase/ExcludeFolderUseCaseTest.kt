package com.offlineplaya.shared.domain.usecase

import com.offlineplaya.shared.data.repository.SqlAlbumRepository
import com.offlineplaya.shared.data.repository.SqlArtistRepository
import com.offlineplaya.shared.data.repository.SqlExcludedFolderRepository
import com.offlineplaya.shared.data.repository.SqlFolderRepository
import com.offlineplaya.shared.data.repository.SqlManagedTreeRootRepository
import com.offlineplaya.shared.data.repository.SqlTrackRepository
import com.offlineplaya.shared.domain.scanner.AudioFolder
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

class ExcludeFolderUseCaseTest {

    private class Fixture {
        val db = createInMemoryDatabase()
        val managedRoots = SqlManagedTreeRootRepository(db, Dispatchers.Unconfined)
        val folders = SqlFolderRepository(db, TestLogger(), Dispatchers.Unconfined)
        val artists = SqlArtistRepository(db, TestLogger(), Dispatchers.Unconfined)
        val albums = SqlAlbumRepository(db, TestLogger(), Dispatchers.Unconfined)
        val tracks = SqlTrackRepository(db, TestLogger(), Dispatchers.Unconfined)
        val excluded = SqlExcludedFolderRepository(db, TestLogger(), Dispatchers.Unconfined)
        val useCase = ExcludeFolderUseCase(excluded, tracks, folders, artists, albums, TestLogger())

        val treeUri = "content://tree/root"
        val scanner = FakeFolderScanner.scan(
            treeUri = treeUri,
            folders = listOf(
                AudioFolder(treeUri, "", "Music", null),
                AudioFolder(treeUri, "Junk", "Junk", ""),
            ),
            files = listOf(
                RawAudioFile("$treeUri/keep.mp3", treeUri, "keep.mp3", "keep.mp3", 1L, 1L),
                RawAudioFile("$treeUri/junk.mp3", treeUri, "Junk/junk.mp3", "junk.mp3", 2L, 2L),
            ),
        )
        val reader = FakeMetadataReader(
            scripted = mapOf(
                "$treeUri/keep.mp3" to com.offlineplaya.shared.domain.scanner.AudioMetadata.Empty
                    .copy(title = "Keep", artist = "Keeper", album = "KeepAlbum"),
                "$treeUri/junk.mp3" to com.offlineplaya.shared.domain.scanner.AudioMetadata.Empty
                    .copy(title = "Junk", artist = "Junker", album = "JunkAlbum"),
            ),
        )
        val sync = LibrarySyncUseCase(
            managedRoots, folders, artists, albums, tracks, scanner, reader,
            FakeDeviceAudioScanner(), excluded, TestLogger(),
        )
    }

    @Test
    fun `excluding a folder drops its tracks and a rescan does not resurrect them`() = runTest {
        val f = Fixture()
        f.sync.syncOne(f.treeUri)
        assertEquals(2L, f.tracks.count())

        val junkFolder = f.folders.findByPath(f.treeUri, "Junk")
        assertNotNull(junkFolder)
        f.useCase.exclude(junkFolder)

        // Indexed rows under the folder are gone; the rest survives.
        assertEquals(1L, f.tracks.count())
        assertNull(f.tracks.findByDocumentUri("${f.treeUri}/junk.mp3"))
        assertNull(f.folders.findByPath(f.treeUri, "Junk"))
        // The artist/album that only existed under the folder are cleaned up.
        assertNull(f.artists.findByName("Junker"))

        // A full rescan must not bring the excluded subtree back.
        f.sync.syncOne(f.treeUri)
        assertEquals(1L, f.tracks.count())
        assertNull(f.tracks.findByDocumentUri("${f.treeUri}/junk.mp3"))
    }

    @Test
    fun `include removes the exclusion so the next scan re-indexes the folder`() = runTest {
        val f = Fixture()
        f.sync.syncOne(f.treeUri)
        val junkFolder = f.folders.findByPath(f.treeUri, "Junk")!!
        f.useCase.exclude(junkFolder)
        assertEquals(1L, f.tracks.count())

        val exclusion = f.excluded.observeAll().first().single()
        f.useCase.include(exclusion.id)
        assertEquals(0, f.excluded.getAll().size)

        f.sync.syncOne(f.treeUri)
        assertEquals(2L, f.tracks.count(), "re-included folder must be re-indexed")
        assertNotNull(f.tracks.findByDocumentUri("${f.treeUri}/junk.mp3"))
    }
}

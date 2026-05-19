package com.offlineplaya.shared.data.repository

import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.testsupport.createInMemoryDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SqlTrackRepositoryTest {

    private fun newRepository() = SqlTrackRepository(createInMemoryDatabase(), Dispatchers.Unconfined)

    @Test
    fun `empty repository reports zero count and empty observeAll`() = runTest {
        val repo = newRepository()
        assertEquals(0L, repo.count())
        assertEquals(emptyList(), repo.observeAll().first())
    }

    @Test
    fun `insertFile assigns a unique id and persists fields`() = runTest {
        val repo = newRepository()
        val id = repo.insertFile(
            documentUri = "content://uri/abc",
            treeUri = "content://tree",
            relativePath = "Music/song.mp3",
            fileName = "song.mp3",
            fileSize = 4_000_000L,
            lastModified = 1_700_000_000L,
            folderId = null,
        )
        val found = repo.findById(id)
        assertNotNull(found)
        assertEquals("content://uri/abc", found.documentUri)
        assertEquals("song.mp3", found.fileName)
        assertEquals(ScanStatus.PENDING, found.scanStatus)
        assertEquals("song.mp3", found.title, "title falls back to file name when tag is absent")
    }

    @Test
    fun `insertFile is idempotent on document_uri`() = runTest {
        val repo = newRepository()
        repo.insertFile("u1", "t", "p", "f", 0, 0, null)
        repo.insertFile("u1", "t", "p", "f", 0, 0, null)
        assertEquals(1L, repo.count())
    }

    @Test
    fun `findByDocumentUri returns null when missing and the track when present`() = runTest {
        val repo = newRepository()
        assertNull(repo.findByDocumentUri("nope"))
        repo.insertFile("present", "t", "p", "f", 0, 0, null)
        assertEquals("present", repo.findByDocumentUri("present")?.documentUri)
    }

    @Test
    fun `updateMetadata transitions status from PENDING to SCANNED`() = runTest {
        val repo = newRepository()
        val id = repo.insertFile("u", "t", "p", "song.mp3", 0, 0, null)
        val pending = repo.findById(id)!!
        assertEquals(ScanStatus.PENDING, pending.scanStatus)

        repo.updateMetadata(
            pending.copy(
                title = "Real Title",
                artistName = "Real Artist",
                albumName = "Real Album",
                year = 1999,
                trackNumber = 3,
                durationMs = 180_000L,
                codec = "mp3",
            )
        )

        val scanned = repo.findById(id)!!
        assertEquals(ScanStatus.SCANNED, scanned.scanStatus)
        assertEquals("Real Title", scanned.title)
        assertEquals(1999, scanned.year)
        assertEquals(3, scanned.trackNumber)
        assertEquals(180_000L, scanned.durationMs)
    }

    @Test
    fun `countByStatus reflects the mix of PENDING SCANNED and ERROR tracks`() = runTest {
        val repo = newRepository()
        val a = repo.insertFile("a", "t", "p", "a.mp3", 0, 0, null)
        val b = repo.insertFile("b", "t", "p", "b.mp3", 0, 0, null)
        val c = repo.insertFile("c", "t", "p", "c.mp3", 0, 0, null)

        repo.updateMetadata(repo.findById(a)!!.copy(title = "A"))
        repo.markError(c)

        assertEquals(1L, repo.countByStatus(ScanStatus.PENDING))
        assertEquals(1L, repo.countByStatus(ScanStatus.SCANNED))
        assertEquals(1L, repo.countByStatus(ScanStatus.ERROR))
        // Reference b to keep it pending and avoid an "unused" warning.
        assertEquals(ScanStatus.PENDING, repo.findById(b)!!.scanStatus)
    }

    @Test
    fun `findPending only returns rows whose status is PENDING`() = runTest {
        val repo = newRepository()
        repo.insertFile("a", "t", "p", "a.mp3", 0, 0, null)
        val b = repo.insertFile("b", "t", "p", "b.mp3", 0, 0, null)
        repo.updateMetadata(repo.findById(b)!!.copy(title = "B"))

        val pending = repo.findPending(limit = 10)
        assertEquals(1, pending.size)
        assertEquals("a", pending.single().documentUri)
    }

    @Test
    fun `updateForeignKeys writes artist and album ids without touching metadata`() = runTest {
        val db = createInMemoryDatabase()
        val repo = SqlTrackRepository(db, Dispatchers.Unconfined)
        val artistRepo = SqlArtistRepository(db, Dispatchers.Unconfined)
        val albumRepo = SqlAlbumRepository(db, Dispatchers.Unconfined)

        val id = repo.insertFile("u", "t", "p", "f.mp3", 0, 0, null)
        val artistId = artistRepo.upsert("Test Artist")
        val albumId = albumRepo.upsert("Test Album", artistId, year = 2024)

        repo.updateForeignKeys(id, artistId = artistId, albumId = albumId)

        val updated = repo.findById(id)!!
        assertEquals(artistId, updated.artistId)
        assertEquals(albumId, updated.albumId)
    }

    @Test
    fun `deleteAll removes every row`() = runTest {
        val repo = newRepository()
        repo.insertFile("a", "t", "p", "a.mp3", 0, 0, null)
        repo.insertFile("b", "t", "p", "b.mp3", 0, 0, null)
        assertEquals(2L, repo.count())
        repo.deleteAll()
        assertEquals(0L, repo.count())
    }

    @Test
    fun `search matches title artist or album case-insensitively and only on SCANNED tracks`() = runTest {
        val repo = newRepository()
        val a = repo.insertFile("u-a", "t", "p", "a.mp3", 0, 0, null)
        val b = repo.insertFile("u-b", "t", "p", "b.mp3", 0, 0, null)
        val c = repo.insertFile("u-c", "t", "p", "c.mp3", 0, 0, null)
        val d = repo.insertFile("u-d", "t", "p", "d.mp3", 0, 0, null)
        // a: matches title; b: matches artist; c: matches album; d: pending — excluded
        repo.updateMetadata(repo.findById(a)!!.copy(title = "Tangerine Dream", artistName = "Tangerine", albumName = "X"))
        repo.updateMetadata(repo.findById(b)!!.copy(title = "X", artistName = "TANGERINE", albumName = "Y"))
        repo.updateMetadata(repo.findById(c)!!.copy(title = "X", artistName = "Y", albumName = "tangerine Z"))
        // d stays pending — search must skip it

        val hits = repo.search("tangerine")
        assertEquals(3, hits.size, "should match scanned rows in title/artist/album, ignore pending")
        // d's documentUri is not in the hit set
        assertEquals(false, hits.any { it.documentUri == "u-d" })
    }

    @Test
    fun `search returns empty list for blank input`() = runTest {
        val repo = newRepository()
        val id = repo.insertFile("u", "t", "p", "a.mp3", 0, 0, null)
        repo.updateMetadata(repo.findById(id)!!.copy(title = "Anything"))

        assertEquals(emptyList(), repo.search(""))
        assertEquals(emptyList(), repo.search("   "))
    }

    @Test
    fun `search respects the limit parameter`() = runTest {
        val repo = newRepository()
        repeat(5) { i ->
            val id = repo.insertFile("u-$i", "t", "p", "$i.mp3", 0, 0, null)
            repo.updateMetadata(repo.findById(id)!!.copy(title = "Match $i"))
        }
        assertEquals(2, repo.search("Match", limit = 2).size)
    }
}

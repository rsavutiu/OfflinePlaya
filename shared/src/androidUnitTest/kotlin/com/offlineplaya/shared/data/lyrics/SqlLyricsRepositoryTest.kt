package com.offlineplaya.shared.data.lyrics

import com.offlineplaya.shared.domain.lyrics.EmbeddedLyricsSource
import com.offlineplaya.shared.domain.lyrics.Lyrics
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.lyrics.SidecarLyricsSource
import com.offlineplaya.shared.testsupport.createInMemoryDatabase
import com.offlineplaya.shared.util.TestLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SqlLyricsRepositoryTest {

    private class FakeEmbedded(var text: String? = null, var calls: Int = 0) : EmbeddedLyricsSource {
        override suspend fun read(track: Track): String? { calls++; return text }
    }

    private class FakeSidecar(var text: String? = null, var calls: Int = 0) : SidecarLyricsSource {
        override suspend fun read(track: Track): String? { calls++; return text }
    }

    private fun newRepo(embedded: FakeEmbedded, sidecar: FakeSidecar) =
        SqlLyricsRepository(
            db = createInMemoryDatabase(),
            embedded = embedded,
            sidecar = sidecar,
            logger = TestLogger(),
            now = { 0L },
            ioDispatcher = Dispatchers.Unconfined,
        )

    private fun track(uri: String = "content://track/1") = Track(
        id = 1, documentUri = uri, treeUri = "content://tree", relativePath = "a/b.mp3",
        fileName = "b.mp3", title = "T", artistName = "A", albumArtistName = null,
        albumName = "Al", genre = null, year = null, trackNumber = null, discNumber = null,
        durationMs = 200_000L, bitrate = null, sampleRate = null, channels = null, codec = null,
        artistId = null, albumId = null, folderId = null, scanStatus = ScanStatus.SCANNED,
    )

    @Test
    fun `no source has lyrics returns None`() = runTest {
        val repo = newRepo(FakeEmbedded(null), FakeSidecar(null))
        assertEquals(Lyrics.None, repo.lyricsFor(track()))
    }

    @Test
    fun `embedded plain text resolves to Plain`() = runTest {
        val repo = newRepo(FakeEmbedded("la la la\nsecond line"), FakeSidecar(null))
        val result = repo.lyricsFor(track())
        assertEquals(Lyrics.Plain("la la la\nsecond line"), result)
    }

    @Test
    fun `sidecar lrc resolves to Synced`() = runTest {
        val sidecar = FakeSidecar("[00:01.00]one\n[00:02.00]two")
        val repo = newRepo(FakeEmbedded(null), sidecar)
        val result = repo.lyricsFor(track())
        assertTrue(result is Lyrics.Synced)
        assertEquals(2, (result as Lyrics.Synced).lines.size)
    }

    @Test
    fun `embedded wins over sidecar`() = runTest {
        val embedded = FakeEmbedded("embedded text")
        val sidecar = FakeSidecar("[00:01.00]sidecar text")
        val repo = newRepo(embedded, sidecar)
        assertEquals(Lyrics.Plain("embedded text"), repo.lyricsFor(track()))
        assertEquals(0, sidecar.calls, "sidecar must not be consulted once embedded hits")
    }

    @Test
    fun `result is cached so sources are not re-read on the next call`() = runTest {
        val embedded = FakeEmbedded("[00:01.00]cached")
        val sidecar = FakeSidecar(null)
        val repo = newRepo(embedded, sidecar)

        val first = repo.lyricsFor(track())
        assertTrue(first is Lyrics.Synced)
        assertEquals(1, embedded.calls)

        // Second call: served from the Lyrics table, sources untouched.
        val second = repo.lyricsFor(track())
        assertTrue(second is Lyrics.Synced)
        assertEquals(1, embedded.calls, "embedded source should not be read again")
    }
}

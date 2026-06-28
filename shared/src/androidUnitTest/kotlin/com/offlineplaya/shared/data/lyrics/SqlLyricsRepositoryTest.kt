package com.offlineplaya.shared.data.lyrics

import com.offlineplaya.shared.domain.lyrics.EmbeddedLyricsSource
import com.offlineplaya.shared.domain.lyrics.Lyrics
import com.offlineplaya.shared.domain.lyrics.LyricsSidecarWriter
import com.offlineplaya.shared.domain.lyrics.RemoteLyricsSource
import com.offlineplaya.shared.domain.lyrics.SidecarLyricsSource
import com.offlineplaya.shared.domain.model.ArtworkPreferences
import com.offlineplaya.shared.domain.model.EqPreferences
import com.offlineplaya.shared.domain.model.LyricsPreferences
import com.offlineplaya.shared.domain.model.PlaybackPreferences
import com.offlineplaya.shared.domain.model.ReviewPromptState
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.ThemePreferences
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.repository.SettingsRepository
import com.offlineplaya.shared.testsupport.createInMemoryDatabase
import com.offlineplaya.shared.util.TestLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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

    private class FakeRemote(var text: String? = null, var calls: Int = 0) : RemoteLyricsSource {
        override suspend fun resolve(track: Track): String? { calls++; return text }
    }

    private class FakeSidecarWriter(
        var returns: Boolean = true,
    ) : LyricsSidecarWriter {
        data class Call(val text: String, val isSynced: Boolean)
        val calls = mutableListOf<Call>()
        override suspend fun write(track: Track, text: String, isSynced: Boolean): Boolean {
            calls += Call(text, isSynced)
            return returns
        }
    }

    /**
     * Settings stub backing only the lyrics-preference accessors used by
     * the repository. The other typed setters are unreachable in this test.
     */
    private class FakeSettings(
        var downloadRemoteLyrics: Boolean = true,
        var saveLyricsAsSidecar: Boolean = false,
    ) : SettingsRepository {
        override fun observeLyricsPreferences(): Flow<LyricsPreferences> =
            flowOf(LyricsPreferences(downloadRemoteLyrics, saveLyricsAsSidecar))
        override suspend fun getLyricsPreferences(): LyricsPreferences =
            LyricsPreferences(downloadRemoteLyrics, saveLyricsAsSidecar)
        override suspend fun setLyricsPreferences(preferences: LyricsPreferences) {
            downloadRemoteLyrics = preferences.downloadRemoteLyrics
            saveLyricsAsSidecar = preferences.saveLyricsAsSidecar
        }

        // Unused in lyrics resolution — throw so a regression that wires
        // the wrong path is loud.
        override fun observeThemePreferences(): Flow<ThemePreferences> = error("unused")
        override suspend fun getThemePreferences(): ThemePreferences = error("unused")
        override suspend fun setThemePreferences(preferences: ThemePreferences) = error("unused")
        override fun observeArtworkPreferences(): Flow<ArtworkPreferences> = error("unused")
        override suspend fun getArtworkPreferences(): ArtworkPreferences = error("unused")
        override suspend fun setArtworkPreferences(preferences: ArtworkPreferences) = error("unused")
        override fun observeEqPreferences(): Flow<EqPreferences> = error("unused")
        override suspend fun getEqPreferences(): EqPreferences = error("unused")
        override suspend fun setEqPreferences(preferences: EqPreferences) = error("unused")
        override fun observePlaybackPreferences(): Flow<PlaybackPreferences> = error("unused")
        override suspend fun getPlaybackPreferences(): PlaybackPreferences = error("unused")
        override suspend fun setPlaybackPreferences(preferences: PlaybackPreferences) = error("unused")
        override suspend fun getLastSeedColor(): Int? = null
        override suspend fun setLastSeedColor(argb: Int?) = Unit
        override suspend fun getReviewPromptState(): ReviewPromptState = ReviewPromptState.Default
        override suspend fun setReviewPromptState(state: ReviewPromptState) = Unit
    }

    private fun newRepo(
        embedded: FakeEmbedded = FakeEmbedded(),
        sidecar: FakeSidecar = FakeSidecar(),
        remote: FakeRemote? = FakeRemote(),
        sidecarWriter: FakeSidecarWriter? = FakeSidecarWriter(),
        settings: FakeSettings = FakeSettings(),
    ) = SqlLyricsRepository(
        db = createInMemoryDatabase(),
        embedded = embedded,
        sidecar = sidecar,
        remote = remote,
        sidecarWriter = sidecarWriter,
        settings = settings,
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
        val repo = newRepo()
        assertEquals(Lyrics.None, repo.lyricsFor(track()))
    }

    @Test
    fun `embedded plain text resolves to Plain`() = runTest {
        val repo = newRepo(embedded = FakeEmbedded("la la la\nsecond line"))
        val result = repo.lyricsFor(track())
        assertEquals(Lyrics.Plain("la la la\nsecond line"), result)
    }

    @Test
    fun `sidecar lrc resolves to Synced`() = runTest {
        val repo = newRepo(sidecar = FakeSidecar("[00:01.00]one\n[00:02.00]two"))
        val result = repo.lyricsFor(track())
        assertTrue(result is Lyrics.Synced)
        assertEquals(2, result.lines.size)
    }

    @Test
    fun `embedded wins over sidecar and remote`() = runTest {
        val embedded = FakeEmbedded("embedded text")
        val sidecar = FakeSidecar("[00:01.00]sidecar text")
        val remote = FakeRemote("[00:01.00]remote text")
        val repo = newRepo(embedded, sidecar, remote)
        assertEquals(Lyrics.Plain("embedded text"), repo.lyricsFor(track()))
        assertEquals(0, sidecar.calls, "sidecar must not be consulted once embedded hits")
        assertEquals(0, remote.calls, "remote must not be consulted once embedded hits")
    }

    @Test
    fun `sidecar wins over remote`() = runTest {
        val sidecar = FakeSidecar("[00:01.00]sidecar text")
        val remote = FakeRemote("[00:01.00]remote text")
        val repo = newRepo(sidecar = sidecar, remote = remote)
        val result = repo.lyricsFor(track())
        assertTrue(result is Lyrics.Synced)
        assertEquals(0, remote.calls, "remote must not be consulted once sidecar hits")
    }

    @Test
    fun `remote is consulted when local sources miss and toggle is on`() = runTest {
        val remote = FakeRemote("[00:01.00]remote line")
        val repo = newRepo(remote = remote, settings = FakeSettings(downloadRemoteLyrics = true))
        val result = repo.lyricsFor(track())
        assertTrue(result is Lyrics.Synced)
        assertEquals(1, remote.calls)
    }

    @Test
    fun `remote is skipped when toggle is off`() = runTest {
        val remote = FakeRemote("[00:01.00]remote line")
        val repo = newRepo(remote = remote, settings = FakeSettings(downloadRemoteLyrics = false))
        val result = repo.lyricsFor(track())
        assertEquals(Lyrics.None, result)
        assertEquals(0, remote.calls, "remote must not be hit when the toggle is off")
    }

    @Test
    fun `null remote source is treated as off`() = runTest {
        val repo = newRepo(remote = null)
        assertEquals(Lyrics.None, repo.lyricsFor(track()))
    }

    @Test
    fun `result is cached so sources are not re-read on the next call`() = runTest {
        val embedded = FakeEmbedded("[00:01.00]cached")
        val repo = newRepo(embedded = embedded)

        val first = repo.lyricsFor(track())
        assertTrue(first is Lyrics.Synced)
        assertEquals(1, embedded.calls)

        // Second call: served from the Lyrics table, sources untouched.
        val second = repo.lyricsFor(track())
        assertTrue(second is Lyrics.Synced)
        assertEquals(1, embedded.calls, "embedded source should not be read again")
    }

    @Test
    fun `remote hit triggers sidecar write when save toggle is on`() = runTest {
        val writer = FakeSidecarWriter()
        val repo = newRepo(
            remote = FakeRemote("[00:01.00]synced"),
            sidecarWriter = writer,
            settings = FakeSettings(
                downloadRemoteLyrics = true,
                saveLyricsAsSidecar = true,
            ),
        )
        repo.lyricsFor(track())
        assertEquals(1, writer.calls.size)
        assertEquals(true, writer.calls.first().isSynced)
        assertEquals("[00:01.00]synced", writer.calls.first().text)
    }

    @Test
    fun `plain remote hit writes sidecar with isSynced=false`() = runTest {
        val writer = FakeSidecarWriter()
        val repo = newRepo(
            remote = FakeRemote("just plain lines\nno timestamps"),
            sidecarWriter = writer,
            settings = FakeSettings(
                downloadRemoteLyrics = true,
                saveLyricsAsSidecar = true,
            ),
        )
        repo.lyricsFor(track())
        assertEquals(1, writer.calls.size)
        assertEquals(false, writer.calls.first().isSynced)
    }

    @Test
    fun `sidecar write is skipped when save toggle is off`() = runTest {
        val writer = FakeSidecarWriter()
        val repo = newRepo(
            remote = FakeRemote("[00:01.00]synced"),
            sidecarWriter = writer,
            settings = FakeSettings(
                downloadRemoteLyrics = true,
                saveLyricsAsSidecar = false,
            ),
        )
        repo.lyricsFor(track())
        assertTrue(writer.calls.isEmpty(), "writer must not be called when save toggle is off")
    }

    @Test
    fun `sidecar write is skipped for local hits regardless of save toggle`() = runTest {
        val writer = FakeSidecarWriter()
        val repo = newRepo(
            embedded = FakeEmbedded("embedded text"),
            sidecarWriter = writer,
            settings = FakeSettings(
                downloadRemoteLyrics = true,
                saveLyricsAsSidecar = true,
            ),
        )
        repo.lyricsFor(track())
        assertTrue(writer.calls.isEmpty(), "embedded hits are already durable in the audio file")
    }

    @Test
    fun `null sidecar writer is tolerated`() = runTest {
        val repo = newRepo(
            remote = FakeRemote("[00:01.00]synced"),
            sidecarWriter = null,
            settings = FakeSettings(
                downloadRemoteLyrics = true,
                saveLyricsAsSidecar = true,
            ),
        )
        // Should not throw — repo treats missing writer the same as toggle off.
        val result = repo.lyricsFor(track())
        assertTrue(result is Lyrics.Synced)
    }

    @Test
    fun `remote hit is persisted to the cache`() = runTest {
        val remote = FakeRemote("[00:01.00]remote line")
        val repo = newRepo(remote = remote)

        repo.lyricsFor(track())
        assertEquals(1, remote.calls)

        // Second call: cached, so the remote shouldn't be called again.
        repo.lyricsFor(track())
        assertEquals(1, remote.calls, "remote should be hit once and cached thereafter")
    }
}

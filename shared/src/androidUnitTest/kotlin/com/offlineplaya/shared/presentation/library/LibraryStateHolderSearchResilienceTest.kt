package com.offlineplaya.shared.presentation.library

import com.offlineplaya.shared.data.repository.SqlAlbumRepository
import com.offlineplaya.shared.data.repository.SqlArtistRepository
import com.offlineplaya.shared.data.repository.SqlFolderRepository
import com.offlineplaya.shared.data.repository.SqlRecentAlbumRepository
import com.offlineplaya.shared.data.repository.SqlTrackRepository
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.repository.TrackRepository
import com.offlineplaya.shared.testsupport.createInMemoryDatabase
import com.offlineplaya.shared.util.TestLogger
import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression guard for the 2026-06-26 search-resilience fix (commit 8028a29):
 * `TrackRepository.search()` runs unguarded inside `flatMapLatest`, so a thrown
 * SQL/repository error used to propagate to `stateIn` and kill `searchResults`
 * for the whole session. The `.catch` on the inner search flow must degrade a
 * failing query to empty results while keeping the outer flow alive for the
 * next query.
 *
 * Headless `LibraryStateHolder` test (no device): wraps a real repo so one query
 * throws, then asserts the flow survives and a later query still returns results.
 */
class LibraryStateHolderSearchResilienceTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `a throwing search degrades to empty and the flow survives for the next query`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher)
        try {
            val db = createInMemoryDatabase()
            val logger = TestLogger()
            val realTracks = SqlTrackRepository(db, logger, dispatcher)

            // Delegate everything to the real repo, but make any query containing
            // "boom" throw — and any other query return a canned hit, proving the
            // collector recovered (not just that the throw was swallowed).
            val tracks = object : TrackRepository by realTracks {
                override suspend fun search(query: String, limit: Int): List<Track> {
                    if (query.contains("boom")) throw RuntimeException("search blew up")
                    return listOf(track(1, "Alpha Anthem"))
                }
            }

            val holder = LibraryStateHolder(
                artists = SqlArtistRepository(db, logger, dispatcher),
                albums = SqlAlbumRepository(db, logger, dispatcher),
                folders = SqlFolderRepository(db, logger, dispatcher),
                tracks = tracks,
                recentAlbumsRepo = SqlRecentAlbumRepository(db, logger, dispatcher),
                scope = scope,
            )

            // searchResults is WhileSubscribed, so it only runs with a collector.
            val seen = mutableListOf<PersistentList<Track>>()
            val collector = scope.launch { holder.searchResults.collect { seen += it } }

            // A query that throws inside search() must degrade to empty, not crash.
            holder.setSearchQuery("boom")
            advanceTimeBy(300) // past the 200ms debounce
            runCurrent()
            assertEquals(
                emptyList<Track>(),
                holder.searchResults.value,
                "a throwing search must degrade to empty results",
            )

            // A later valid query must still resolve — proving the flow is alive.
            holder.setSearchQuery("alpha")
            advanceTimeBy(300)
            runCurrent()
            assertEquals(
                "Alpha Anthem",
                holder.searchResults.value.singleOrNull()?.title,
                "search must keep working after an earlier query threw",
            )
            assertTrue(collector.isActive, "collector should still be active")

            collector.cancel()
        } finally {
            scope.cancel()
        }
    }

    private fun track(id: Long, title: String) = Track(
        id = id, documentUri = "u/$id", treeUri = "t", relativePath = "$title.mp3",
        fileName = "$title.mp3", title = title, artistName = "A", albumArtistName = null,
        albumName = "Al", genre = null, year = null, trackNumber = null, discNumber = null,
        durationMs = 200_000L, bitrate = null, sampleRate = null, channels = null, codec = null,
        artistId = null, albumId = null, folderId = null, scanStatus = ScanStatus.SCANNED,
    )
}

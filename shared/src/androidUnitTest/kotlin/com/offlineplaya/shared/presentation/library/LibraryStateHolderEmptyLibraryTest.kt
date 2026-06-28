package com.offlineplaya.shared.presentation.library

import com.offlineplaya.shared.data.repository.SqlAlbumRepository
import com.offlineplaya.shared.data.repository.SqlArtistRepository
import com.offlineplaya.shared.data.repository.SqlFolderRepository
import com.offlineplaya.shared.data.repository.SqlRecentAlbumRepository
import com.offlineplaya.shared.data.repository.SqlTrackRepository
import com.offlineplaya.shared.testsupport.createInMemoryDatabase
import com.offlineplaya.shared.util.TestLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Guards the Home onboarding-guide trigger ([LibraryStateHolder.isLibraryEmpty]).
 *
 * The flag must report empty *only* when the library is genuinely empty, and
 * must report non-empty as soon as a track exists — it backs a full-screen
 * "you have no music" guide, so a false positive on a populated library would
 * be a jarring, visible bug. (The complementary "don't flash while the count is
 * still loading" guarantee comes from seeding the flow at `false`, verifiable in
 * the `stateIn(..., false)` declaration.)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryStateHolderEmptyLibraryTest {

    @Test
    fun `reports empty for an empty library and non-empty once a track exists`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher)
        try {
            val db = createInMemoryDatabase()
            val logger = TestLogger()
            val tracks = SqlTrackRepository(db, logger, dispatcher)

            val holder = LibraryStateHolder(
                artists = SqlArtistRepository(db, logger, dispatcher),
                albums = SqlAlbumRepository(db, logger, dispatcher),
                folders = SqlFolderRepository(db, logger, dispatcher),
                tracks = tracks,
                recentAlbumsRepo = SqlRecentAlbumRepository(db, logger, dispatcher),
                scope = scope,
            )

            // Empty DB → the guide should show.
            runCurrent()
            assertTrue(
                holder.isLibraryEmpty.value,
                "an empty library must report empty so the onboarding guide shows",
            )

            // One indexed track → the guide must disappear.
            tracks.insertFile(
                documentUri = "u/1",
                treeUri = "t",
                relativePath = "Song.mp3",
                fileName = "Song.mp3",
                fileSize = 1_000L,
                lastModified = 0L,
                folderId = null,
            )
            runCurrent()
            assertFalse(
                holder.isLibraryEmpty.value,
                "a library with a track must not report empty",
            )
        } finally {
            scope.cancel()
        }
    }
}

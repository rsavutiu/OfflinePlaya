package com.offlineplaya.shared.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Boundary behaviour of [ExcludedFolder.covers] — the in-memory check that
 * [com.offlineplaya.shared.domain.usecase.LibrarySyncUseCase] uses to skip
 * excluded subtrees on every scan.
 */
class ExcludedFolderTest {

    private fun excluded(treeUri: String = "tree-a", path: String = "Music/Live") =
        ExcludedFolder(id = 1, treeUri = treeUri, relativePath = path, displayName = "Live")

    @Test
    fun `covers the folder itself (exact path match)`() {
        assertTrue(excluded().covers("tree-a", "Music/Live"))
    }

    @Test
    fun `covers a descendant file or folder`() {
        assertTrue(excluded().covers("tree-a", "Music/Live/track.mp3"))
        assertTrue(excluded().covers("tree-a", "Music/Live/Bootlegs/x.flac"))
    }

    @Test
    fun `does not cover a sibling that merely shares the name prefix`() {
        // "Music/Live2" must not be swallowed by excluding "Music/Live" — the
        // match is boundary-safe on the '/' separator, not raw startsWith.
        assertFalse(excluded().covers("tree-a", "Music/Live2"))
        assertFalse(excluded().covers("tree-a", "Music/Live2/track.mp3"))
    }

    @Test
    fun `does not cover an ancestor or unrelated path`() {
        assertFalse(excluded().covers("tree-a", "Music"))
        assertFalse(excluded().covers("tree-a", "Podcasts/ep.mp3"))
    }

    @Test
    fun `does not cross authorities even when the path matches`() {
        assertFalse(excluded().covers("tree-b", "Music/Live/track.mp3"))
    }

    @Test
    fun `exact match and descendant are the only true cases`() {
        val f = excluded()
        assertEquals(
            listOf(true, true, false, false),
            listOf(
                f.covers("tree-a", "Music/Live"),
                f.covers("tree-a", "Music/Live/a.mp3"),
                f.covers("tree-a", "Music/Liver"),
                f.covers("tree-a", "Musi"),
            ),
        )
    }
}

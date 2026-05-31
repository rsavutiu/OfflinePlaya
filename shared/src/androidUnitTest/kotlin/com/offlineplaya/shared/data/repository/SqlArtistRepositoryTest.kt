package com.offlineplaya.shared.data.repository

import com.offlineplaya.shared.testsupport.createInMemoryDatabase
import com.offlineplaya.shared.util.TestLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SqlArtistRepositoryTest {

    private fun newRepository() =
        SqlArtistRepository(createInMemoryDatabase(), TestLogger(), Dispatchers.Unconfined)

    @Test
    fun `upsert returns the same id for a duplicate name`() = runTest {
        val repo = newRepository()
        val first = repo.upsert("Radiohead")
        val second = repo.upsert("Radiohead")
        assertEquals(first, second)
        assertEquals(1, repo.observeAll().first().size)
    }

    @Test
    fun `findByName returns the inserted artist`() = runTest {
        val repo = newRepository()
        repo.upsert("Aphex Twin")
        val found = repo.findByName("Aphex Twin")
        assertNotNull(found)
        assertEquals("Aphex Twin", found.name)
    }

    @Test
    fun `findById returns null for an unknown id`() = runTest {
        assertNull(newRepository().findById(99))
    }

    @Test
    fun `observeAll emits artists sorted case-insensitively by name`() = runTest {
        val repo = newRepository()
        repo.upsert("aphex twin")
        repo.upsert("Radiohead")
        repo.upsert("Boards of Canada")
        val names = repo.observeAll().first().map { it.name }
        assertEquals(listOf("aphex twin", "Boards of Canada", "Radiohead"), names)
    }
}

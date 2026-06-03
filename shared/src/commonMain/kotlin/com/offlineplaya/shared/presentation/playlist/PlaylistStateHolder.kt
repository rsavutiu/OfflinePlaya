package com.offlineplaya.shared.presentation.playlist

import com.offlineplaya.shared.domain.model.Playlist
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.repository.PlaylistRepository
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI-facing facade for playlist CRUD. Exposes a hot list of all playlists
 * and forwards mutations onto the [PlaylistRepository] via the supplied
 * [scope] so the UI doesn't have to manage coroutines itself.
 */
class PlaylistStateHolder(
    private val playlists: PlaylistRepository,
    private val scope: CoroutineScope,
) {

    val allPlaylists: StateFlow<PersistentList<Playlist>> = playlists.observeAll()
        .map { it.toPersistentList() }
        .stateIn(scope, SharingStarted.Eagerly, persistentListOf())

    fun tracksIn(playlistId: Long): Flow<PersistentList<Track>> =
        playlists.observeTracks(playlistId).map { it.toPersistentList() }

    suspend fun findPlaylist(id: Long): Playlist? = playlists.findById(id)

    fun create(name: String) {
        if (name.isBlank()) return
        scope.launch { playlists.create(name.trim()) }
    }

    /**
     * Create a playlist and immediately add [trackId] to it. Used by the
     * long-press "Add to new playlist" flow so the user isn't bounced to the
     * Playlists page just to seed a playlist with one track. The repo's
     * [PlaylistRepository.create] returns the new id, so this stays a single
     * launched job — create-then-add — rather than two fire-and-forget calls.
     */
    fun createAndAddTrack(name: String, trackId: Long) {
        if (name.isBlank()) return
        scope.launch {
            val id = playlists.create(name.trim())
            playlists.addTrack(id, trackId)
        }
    }

    fun rename(id: Long, name: String) {
        if (name.isBlank()) return
        scope.launch { playlists.rename(id, name.trim()) }
    }

    fun delete(id: Long) {
        scope.launch { playlists.delete(id) }
    }

    fun addTrack(playlistId: Long, trackId: Long) {
        scope.launch { playlists.addTrack(playlistId, trackId) }
    }

    fun removeEntry(playlistTrackId: Long) {
        scope.launch { playlists.removeEntry(playlistTrackId) }
    }
}

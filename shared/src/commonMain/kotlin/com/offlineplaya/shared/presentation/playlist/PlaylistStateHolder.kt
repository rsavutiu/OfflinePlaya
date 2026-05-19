package com.offlineplaya.shared.presentation.playlist

import com.offlineplaya.shared.domain.model.Playlist
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.repository.PlaylistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    val allPlaylists: StateFlow<List<Playlist>> = playlists.observeAll()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun tracksIn(playlistId: Long): Flow<List<Track>> = playlists.observeTracks(playlistId)

    suspend fun findPlaylist(id: Long): Playlist? = playlists.findById(id)

    fun create(name: String) {
        if (name.isBlank()) return
        scope.launch { playlists.create(name.trim()) }
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

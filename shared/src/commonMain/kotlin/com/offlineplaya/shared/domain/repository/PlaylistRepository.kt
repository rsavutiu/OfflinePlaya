package com.offlineplaya.shared.domain.repository

import com.offlineplaya.shared.domain.model.Playlist
import com.offlineplaya.shared.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun observeAll(): Flow<List<Playlist>>
    fun observeTracks(playlistId: Long): Flow<List<Track>>
    suspend fun findById(id: Long): Playlist?
    suspend fun create(name: String): Long
    suspend fun rename(id: Long, name: String)
    suspend fun delete(id: Long)
    suspend fun addTrack(playlistId: Long, trackId: Long)
    suspend fun removeEntry(playlistTrackId: Long)
    suspend fun reorder(playlistTrackId: Long, newPosition: Int)
}

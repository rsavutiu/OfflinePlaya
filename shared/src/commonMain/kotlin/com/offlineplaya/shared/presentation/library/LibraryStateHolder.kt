package com.offlineplaya.shared.presentation.library

import com.offlineplaya.shared.domain.model.Album
import com.offlineplaya.shared.domain.model.Artist
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.repository.AlbumRepository
import com.offlineplaya.shared.domain.repository.ArtistRepository
import com.offlineplaya.shared.domain.repository.TrackRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * UI-facing facade over the library repositories. Exposes hot lists for the
 * top-level views and on-demand getters for drill-down pages so each detail
 * page can fetch its scoped data without juggling repo handles itself.
 *
 * Kept thin — no use-case logic here, just glue.
 */
class LibraryStateHolder(
    private val artists: ArtistRepository,
    private val albums: AlbumRepository,
    private val tracks: TrackRepository,
    private val scope: CoroutineScope,
) {

    /** All artists, alphabetical. Always-on hot list. */
    val allArtists: StateFlow<List<Artist>> = artists.observeAll()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    /**
     * Total track count derived from [TrackRepository.observeAll]. Re-emits
     * whenever the underlying table changes, so the home page Library button
     * appears as soon as the first scan completes.
     */
    val totalTrackCount: StateFlow<Long> = tracks.observeAll()
        .map { it.size.toLong() }
        .stateIn(scope, SharingStarted.Eagerly, 0L)

    suspend fun findArtist(id: Long): Artist? = artists.findById(id)
    suspend fun findAlbum(id: Long): Album? = albums.findById(id)

    fun albumsByArtist(artistId: Long): Flow<List<Album>> = albums.observeByArtist(artistId)
    fun tracksByAlbum(albumId: Long): Flow<List<Track>> = tracks.observeByAlbum(albumId)

    /** Look up the artist name without holding a reference to the Artist row. */
    suspend fun artistNameOrNull(id: Long?): String? = id?.let { artists.findById(it)?.name }
}

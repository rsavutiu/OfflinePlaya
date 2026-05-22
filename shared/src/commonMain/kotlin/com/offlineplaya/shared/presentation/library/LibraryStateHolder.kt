package com.offlineplaya.shared.presentation.library

import com.offlineplaya.shared.domain.model.Album
import com.offlineplaya.shared.domain.model.Artist
import com.offlineplaya.shared.domain.model.Folder
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.repository.AlbumRepository
import com.offlineplaya.shared.domain.repository.ArtistRepository
import com.offlineplaya.shared.domain.repository.FolderRepository
import com.offlineplaya.shared.domain.repository.TrackRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
    private val folders: FolderRepository,
    private val tracks: TrackRepository,
    private val scope: CoroutineScope,
) {

    /** All artists, alphabetical. Always-on hot list. */
    val allArtists: StateFlow<List<Artist>> = artists.observeAll()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** All root folders (one per managed tree). Always-on hot list. */
    val rootFolders: StateFlow<List<Folder>> = folders.observeRoots()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** Every track in the library, sorted by title. Lazy — collected only when subscribed. */
    val allTracks: StateFlow<List<Track>> = tracks.observeAll()
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000L), emptyList())

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
    suspend fun findFolder(id: Long): Folder? = folders.findById(id)

    /** One representative track per album, used by album-row thumbnails to
     *  feed the Coil art fetcher. Returns null while the album has no
     *  tracks yet (e.g. mid-scan). */
    suspend fun representativeTrackOfAlbum(albumId: Long): Track? =
        tracks.findFirstByAlbum(albumId)

    fun albumsByArtist(artistId: Long): Flow<List<Album>> = albums.observeByArtist(artistId)
    fun tracksByAlbum(albumId: Long): Flow<List<Track>> = tracks.observeByAlbum(albumId)
    fun tracksByArtist(artistId: Long): Flow<List<Track>> = tracks.observeByArtist(artistId)
    fun childFolders(parentId: Long): Flow<List<Folder>> = folders.observeChildren(parentId)
    fun tracksInFolder(folderId: Long): Flow<List<Track>> = tracks.observeByFolder(folderId)

    /**
     * Up to [limit] tracks for [folderId], one per distinct album, used to
     * build the folder-row collage thumbnail. Derived off the existing
     * folder-tracks flow so it picks up new scans automatically.
     */
    fun previewTracksInFolder(folderId: Long, limit: Int = 4): Flow<List<Track>> =
        tracks.observeByFolder(folderId).map { all ->
            val seen = HashSet<Long?>()
            val out = ArrayList<Track>(limit)
            for (t in all) {
                if (out.size >= limit) break
                if (seen.add(t.albumId)) out += t
            }
            out
        }

    /** Look up the artist name without holding a reference to the Artist row. */
    suspend fun artistNameOrNull(id: Long?): String? = id?.let { artists.findById(it)?.name }

    // --- search ---

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Hot debounced search results. Empty when the trimmed query is shorter
     * than two characters; otherwise reflects the latest [TrackRepository.search]
     * pass, cancelling any earlier in-flight search when the query changes.
     */
    @OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<Track>> = _searchQuery
        .debounce(200L)
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.length < 2) flowOf(emptyList()) else flow { emit(tracks.search(query)) }
        }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000L), emptyList())
}

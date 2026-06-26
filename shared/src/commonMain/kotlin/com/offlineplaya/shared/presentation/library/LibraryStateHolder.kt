package com.offlineplaya.shared.presentation.library

import com.offlineplaya.shared.domain.model.Album
import com.offlineplaya.shared.domain.model.Artist
import com.offlineplaya.shared.domain.model.Folder
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.repository.AlbumRepository
import com.offlineplaya.shared.domain.repository.ArtistRepository
import com.offlineplaya.shared.domain.repository.FolderRepository
import com.offlineplaya.shared.domain.repository.RecentAlbumRepository
import com.offlineplaya.shared.domain.repository.TrackRepository
import com.offlineplaya.shared.presentation.library.LibraryStateHolder.Companion.RECENT_ALBUMS_LIMIT
import com.offlineplaya.shared.util.currentTimeMillis
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    private val recentAlbumsRepo: RecentAlbumRepository,
    private val scope: CoroutineScope,
) {

    private companion object {
        // Plenty of headroom for the home grid's 16 fan slots without
        // bloating the recency table. Old entries fall off naturally as
        // the user plays new things — there's no explicit eviction
        // policy because the LIMIT clause clips the read path.
        const val RECENT_ALBUMS_LIMIT = 32
    }

    // Repositories return plain `List<T>`; we convert to `PersistentList`
    // at the state-holder boundary so every downstream Composable sees a
    // Compose-stable collection (the Compose compiler treats `List` as
    // unstable because mutable subtypes exist; `PersistentList` is
    // `@Immutable` and skips recomposition when the reference is the same).
    /** All artists, alphabetical. Always-on hot list. */
    val allArtists: StateFlow<PersistentList<Artist>> = artists.observeAll()
        .map { it.toPersistentList() }
        .stateIn(scope, SharingStarted.Eagerly, persistentListOf())

    /** All albums, alphabetical. Used by the home page recent-albums shelf. */
    val allAlbums: StateFlow<PersistentList<Album>> = albums.observeAll()
        .map { it.toPersistentList() }
        .stateIn(scope, SharingStarted.Eagerly, persistentListOf())

    /** All root folders (one per managed tree). Always-on hot list. */
    val rootFolders: StateFlow<PersistentList<Folder>> = folders.observeRoots()
        .map { it.toPersistentList() }
        .stateIn(scope, SharingStarted.Eagerly, persistentListOf())

    /** Every track in the library, sorted by title. Lazy — collected only when subscribed. */
    val allTracks: StateFlow<PersistentList<Track>> = tracks.observeAll()
        .map { it.toPersistentList() }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000L), persistentListOf())

    /**
     * "Recently played" album shelf, persisted across launches via the
     * `RecentAlbum` table. Empty on first launch — fills up as the user
     * plays tracks (see [recordAlbumUse] on the [App] play-trigger
     * lambda). Front of the list is most recent. Capped at
     * [RECENT_ALBUMS_LIMIT]; the home grid only ever displays the first
     * 16 anyway.
     */
    val recentAlbums: StateFlow<PersistentList<Album>> =
        recentAlbumsRepo.observeRecent(RECENT_ALBUMS_LIMIT)
            .map { it.toPersistentList() }
            .stateIn(scope, SharingStarted.Eagerly, persistentListOf())

    /**
     * Record an album as "just played". No-op when [albumId] is null —
     * the player path passes through Track rows whose `albumId` may not
     * have been resolved yet (mid-scan), and we'd rather drop those
     * recordings on the floor than insert nulls.
     */
    fun recordAlbumUse(albumId: Long?) {
        if (albumId == null) return
        scope.launch { recentAlbumsRepo.recordUse(albumId, currentTimeMillis()) }
    }

    /**
     * Total track count derived from [TrackRepository.observeAll]. Re-emits
     * whenever the underlying table changes, so the home page Library button
     * appears as soon as the first scan completes.
     */
    val totalTrackCount: StateFlow<Long> = tracks.observeCount()
        .stateIn(scope, SharingStarted.Eagerly, 0L)

    suspend fun findArtist(id: Long): Artist? = artists.findById(id)
    suspend fun findAlbum(id: Long): Album? = albums.findById(id)
    suspend fun findFolder(id: Long): Folder? = folders.findById(id)

    /** One representative track per album, used by album-row thumbnails to
     *  feed the Coil art fetcher. Returns null while the album has no
     *  tracks yet (e.g. mid-scan). */
    suspend fun representativeTrackOfAlbum(albumId: Long): Track? =
        tracks.findFirstByAlbum(albumId)

    fun albumsByArtist(artistId: Long): Flow<PersistentList<Album>> =
        albums.observeByArtist(artistId).map { it.toPersistentList() }

    fun tracksByAlbum(albumId: Long): Flow<PersistentList<Track>> =
        tracks.observeByAlbum(albumId).map { it.toPersistentList() }

    fun tracksByArtist(artistId: Long): Flow<PersistentList<Track>> =
        tracks.observeByArtist(artistId).map { it.toPersistentList() }

    fun childFolders(parentId: Long): Flow<PersistentList<Folder>> =
        folders.observeChildren(parentId).map { it.toPersistentList() }

    fun tracksInFolder(folderId: Long): Flow<PersistentList<Track>> =
        tracks.observeByFolder(folderId).map { it.toPersistentList() }

    /**
     * Every scanned track at or under [folder] (subfolders included), in
     * path order — the queue for the folder-row play button.
     */
    suspend fun tracksUnderFolder(folder: Folder): List<Track> =
        tracks.findByPathPrefix(folder.treeUri, folder.relativePath)

    /**
     * Up to [limit] tracks for [folderId], one per distinct album, used to
     * build the folder-row collage thumbnail. Derived off the existing
     * folder-tracks flow so it picks up new scans automatically.
     */
    fun previewTracksInFolder(folderId: Long, limit: Int = 4): Flow<PersistentList<Track>> =
        tracks.observeByFolder(folderId).map { all ->
            val seen = HashSet<Long?>()
            val out = ArrayList<Track>(limit)
            for (t in all) {
                if (out.size >= limit) break
                if (seen.add(t.albumId)) out += t
            }
            out.toPersistentList()
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
    val searchResults: StateFlow<PersistentList<Track>> = _searchQuery
        .debounce(200L)
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.length < 2) flowOf(persistentListOf())
            // .catch is on the inner flow so a failed search degrades to empty
            // results for *this* query only — without it, an exception would
            // propagate to stateIn and kill search for the whole session.
            // (catch already rethrows CancellationException.)
            else flow { emit(tracks.search(query).toPersistentList()) }
                .catch { emit(persistentListOf()) }
        }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000L), persistentListOf())
}

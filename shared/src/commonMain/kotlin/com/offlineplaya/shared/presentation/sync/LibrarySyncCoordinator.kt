package com.offlineplaya.shared.presentation.sync

import com.offlineplaya.shared.domain.model.ManagedTreeRoot
import com.offlineplaya.shared.domain.repository.AlbumRepository
import com.offlineplaya.shared.domain.repository.ArtistRepository
import com.offlineplaya.shared.domain.repository.FolderRepository
import com.offlineplaya.shared.domain.repository.ManagedTreeRootRepository
import com.offlineplaya.shared.domain.repository.TrackRepository
import com.offlineplaya.shared.domain.usecase.LibrarySyncUseCase
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Glue between the UI layer and the [LibrarySyncUseCase]. Holds a single
 * [StateFlow] of [SyncStatus] so the home page can observe progress, and
 * funnels picker results into [ManagedTreeRootRepository] + the use case.
 *
 * Lives in `commonMain` so any future iOS/Desktop UI can reuse it as-is.
 * The platform layer supplies the URI (via SAF on Android) and a coroutine
 * [scope] tied to the application lifetime.
 */
class LibrarySyncCoordinator(
    private val syncUseCase: LibrarySyncUseCase,
    private val managedRoots: ManagedTreeRootRepository,
    private val tracks: TrackRepository,
    private val folders: FolderRepository,
    private val artists: ArtistRepository,
    private val albums: AlbumRepository,
    private val scope: CoroutineScope,
) {
    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    /**
     * Every tree URI the user has granted us access to. Cold Flow — callers
     * collectAsState(initial = emptyList()) when they want to render the
     * list. Kept cold so the coordinator scope doesn't accumulate sentinel
     * coroutines that outlive the test fixtures.
     */
    val managedRootsFlow: Flow<PersistentList<ManagedTreeRoot>> =
        managedRoots.observeAll().map { it.toPersistentList() }

    /**
     * Register a newly-picked tree URI as a managed root and immediately sync
     * it. The launched [Job] is returned so tests (and future UI cancellation
     * logic) can await completion; production callers can ignore it.
     */
    fun addAndSync(treeUri: String, displayName: String): Job = scope.launch {
        try {
            val conflict = findOverlappingRoot(treeUri, managedRoots.getAll())
            if (conflict != null) {
                _status.value = SyncStatus.AlreadyAdded(conflict.treeUri, conflict.displayName)
                return@launch
            }
            _status.value = SyncStatus.Scanning(treeUri)
            managedRoots.add(treeUri, displayName)
            val report = syncUseCase.syncOne(treeUri)
            _status.value = SyncStatus.Completed(report)
        } catch (t: Throwable) {
            _status.value = SyncStatus.Failed(t.message ?: "Unknown error")
        }
    }

    /** Trigger a re-scan of every already-registered managed root. */
    fun resyncAll(): Job = scope.launch {
        try {
            _status.value = SyncStatus.Scanning(treeUri = "<all>")
            val report = syncUseCase.syncAll()
            _status.value = SyncStatus.Completed(report)
        } catch (t: Throwable) {
            _status.value = SyncStatus.Failed(t.message ?: "Unknown error")
        }
    }

    /**
     * Re-scan only if nothing is in flight. Used by the auto-rescan triggers
     * (MediaStore observer, app foreground) so a burst of filesystem events
     * doesn't stack scans on top of each other — if the user just added 50
     * tracks from a torrent client, the observer fires many times in seconds;
     * we want one scan, not fifty.
     */
    fun resyncIfIdle(): Job? {
        if (_status.value is SyncStatus.Scanning) return null
        return resyncAll()
    }

    /**
     * Re-scan every managed root, but only if at least one is registered.
     * Used at app launch so empty installs don't briefly flip the status
     * indicator into Scanning / Completed for no reason.
     */
    fun resyncAllIfHasRoots(): Job = scope.launch {
        if (managedRoots.getAll().isEmpty()) return@launch
        try {
            _status.value = SyncStatus.Scanning(treeUri = "<all>")
            val report = syncUseCase.syncAll()
            _status.value = SyncStatus.Completed(report)
        } catch (t: Throwable) {
            _status.value = SyncStatus.Failed(t.message ?: "Unknown error")
        }
    }

    /**
     * Return any managed root that conflicts with [candidateUri]: exact match,
     * an ancestor that already covers [candidateUri], or a descendant that
     * [candidateUri] would itself cover. Returns null when [candidateUri] is
     * a fresh, non-overlapping tree.
     *
     * Overlap is computed on the *decoded* document-id path with `/` as the
     * separator, scoped to a single authority. Two URIs from different
     * authorities (different DocumentsProviders) can never overlap — even if
     * their decoded paths happen to coincide, they refer to different storage.
     */
    internal fun findOverlappingRoot(
        candidateUri: String,
        existing: List<ManagedTreeRoot>,
    ): ManagedTreeRoot? {
        // Exact-string match is a duplicate even when the URI doesn't follow
        // the SAF tree shape — used by non-SAF callers and by tests with
        // synthetic URIs.
        existing.firstOrNull { it.treeUri == candidateUri }?.let { return it }

        val candidate = parseRoot(candidateUri) ?: return null
        for (root in existing) {
            val r = parseRoot(root.treeUri) ?: continue
            if (candidate.scheme != r.scheme) continue
            if (candidate.authority != r.authority) continue
            if (candidate.path == r.path) return root
            if (candidate.path.startsWith(r.path + "/")) return root
            if (r.path.startsWith(candidate.path + "/")) return root
        }
        return null
    }

    /**
     * Forget a managed tree URI and drop everything that came from it: the
     * Track rows, the Folder rows, and the ManagedTreeRoot record itself.
     * Artist/Album rows stay (they may be referenced by other roots) but
     * their aggregate counts are refreshed so empty ones display as zero
     * instead of stale.
     */
    fun removeManagedRoot(treeUri: String): Job = scope.launch {
        // Snapshot affected artists/albums BEFORE deleting tracks — the FK
        // values are gone the moment the rows are dropped.
        val touched = tracks.selectAggregatesByTreeUri(treeUri)
        val touchedArtists = touched.mapNotNull { it.artistId }.toSet()
        val touchedAlbums = touched.mapNotNull { it.albumId }.toSet()

        tracks.deleteByTreeUri(treeUri)
        folders.deleteByTreeUri(treeUri)
        managedRoots.remove(treeUri)

        touchedAlbums.forEach { albums.refreshAggregates(it) }
        touchedArtists.forEach { artists.refreshCounts(it) }
    }
}

internal data class ParsedRoot(val scheme: String, val authority: String, val path: String)

private const val PRIMARY_VOLUME_FS_PATH = "/storage/emulated/0"

/**
 * Decompose a managed-root URI into a `(scheme, authority, path)` triple that
 * can be prefix-compared for overlap detection. Handles two shapes:
 *
 *  - `content://<authority>/tree/<encoded-doc-id>` (SAF) — path is the
 *    percent-decoded document id.
 *  - `file:///absolute/path` (direct filesystem, used by the in-app folder
 *    browser when the user has granted MANAGE_EXTERNAL_STORAGE).
 *
 * As a special case, SAF tree URIs against the external-storage provider's
 * primary volume (`primary:Foo`) are normalised to their filesystem
 * counterpart (`/storage/emulated/0/Foo`) under a synthetic `"fs"` scheme.
 * Without that mapping, a SAF root and a file:// root pointing at the same
 * physical folder would not collide, and re-scanning via the alternate picker
 * would duplicate every track. Cross-authority and SD-card SAF URIs fall back
 * to their raw scheme/authority and only overlap with same-scheme roots.
 *
 * Returns null for URIs that match neither shape — those just fall through to
 * "no overlap detected" so a malformed URI never blocks the happy path.
 */
internal fun parseRoot(uri: String): ParsedRoot? {
    val schemeIdx = uri.indexOf("://")
    if (schemeIdx < 0) return null
    val scheme = uri.substring(0, schemeIdx)
    val afterScheme = uri.substring(schemeIdx + 3)

    if (scheme == "file") {
        val rawPath = if (afterScheme.startsWith("/")) afterScheme else "/$afterScheme"
        return ParsedRoot("fs", authority = "", path = trimTrailingSlash(rawPath))
    }

    val authorityEnd = afterScheme.indexOf('/')
    if (authorityEnd <= 0) return null
    val authority = afterScheme.substring(0, authorityEnd)
    val rest = afterScheme.substring(authorityEnd + 1)
    val treeMarker = "tree/"
    if (!rest.startsWith(treeMarker)) return null
    val afterTree = rest.substring(treeMarker.length)
    val docIdEnd = afterTree.indexOf('/').let { if (it < 0) afterTree.length else it }
    val docId = percentDecode(afterTree.substring(0, docIdEnd))

    if (authority == "com.android.externalstorage.documents" && docId.startsWith("primary:")) {
        val sub = docId.removePrefix("primary:").trim('/')
        val path = if (sub.isEmpty()) PRIMARY_VOLUME_FS_PATH else "$PRIMARY_VOLUME_FS_PATH/$sub"
        return ParsedRoot("fs", authority = "", path = trimTrailingSlash(path))
    }
    return ParsedRoot(scheme, authority, docId)
}

private fun trimTrailingSlash(path: String): String =
    if (path.length > 1 && path.endsWith('/')) path.trimEnd('/') else path

/**
 * Tiny URL-decoder for the document-id portion of a SAF tree URI. Handles
 * `%XX` hex escapes and leaves everything else alone. We don't pull in a
 * URI library for this since commonMain has no platform URL parser.
 */
internal fun percentDecode(input: String): String {
    if ('%' !in input) return input
    val out = StringBuilder(input.length)
    var i = 0
    while (i < input.length) {
        val c = input[i]
        if (c == '%' && i + 2 < input.length) {
            val hex = input.substring(i + 1, i + 3)
            val byte = hex.toIntOrNull(16)
            if (byte != null) {
                out.append(byte.toChar())
                i += 3
                continue
            }
        }
        out.append(c)
        i++
    }
    return out.toString()
}

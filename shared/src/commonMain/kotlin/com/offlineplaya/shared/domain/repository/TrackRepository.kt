package com.offlineplaya.shared.domain.repository

import com.offlineplaya.shared.domain.model.CanonicalGenre
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface TrackRepository {
    fun observeAll(): Flow<List<Track>>
    fun observeByAlbum(albumId: Long): Flow<List<Track>>
    fun observeByArtist(artistId: Long): Flow<List<Track>>
    fun observeByFolder(folderId: Long): Flow<List<Track>>

    /**
     * Hot count of every track in the library. Backed by `SELECT COUNT(*)`
     * so cold-start cost is constant in the row count — used by the home
     * page total instead of materializing every row through [observeAll].
     */
    fun observeCount(): Flow<Long>

    suspend fun count(): Long
    suspend fun countByStatus(status: ScanStatus): Long
    suspend fun findById(id: Long): Track?
    suspend fun findByDocumentUri(uri: String): Track?
    suspend fun findPending(limit: Int): List<Track>

    /**
     * Look for a track whose physical-file fingerprint matches but lives under
     * a different `tree_uri` than [excludeTreeUri]. Used by [syncDeviceAudio]
     * to skip files already covered by a SAF-managed root.
     */
    suspend fun findByContentKeyExcludingTree(
        fileName: String,
        fileSize: Long,
        lastModified: Long,
        excludeTreeUri: String,
    ): Track?

    /**
     * Inverse lookup: every track in [inTreeUri] (typically `device://audio`)
     * that matches the given fingerprint. Used by SAF scans to promote
     * themselves over an existing device-audio row for the same physical file.
     */
    suspend fun findIdsByContentKeyInTree(
        fileName: String,
        fileSize: Long,
        lastModified: Long,
        inTreeUri: String,
    ): List<Long>

    suspend fun deleteById(id: Long)

    /**
     * Pull (track id, artistId, albumId) for every row in [treeUri] before
     * deletion, so callers can refresh aggregate counts on the artists and
     * albums those tracks belonged to.
     */
    suspend fun selectAggregatesByTreeUri(treeUri: String): List<TrackAggregateRef>

    suspend fun deleteByTreeUri(treeUri: String)

    /**
     * One representative track for [albumId] — used as the source for album-art
     * thumbnails in list rows. Returns the first track in album/disc/title
     * order, or `null` if the album has no tracks yet.
     */
    suspend fun findFirstByAlbum(albumId: Long): Track?

    /**
     * Fuzzy search across title / artist / album. The query string is
     * substring-matched (case-insensitive); only `scanned` tracks are
     * returned. Results are capped at [limit] for UI predictability.
     */
    suspend fun search(query: String, limit: Int = 200): List<Track>

    suspend fun insertFile(
        documentUri: String,
        treeUri: String,
        relativePath: String,
        fileName: String,
        fileSize: Long,
        lastModified: Long,
        folderId: Long?,
    ): Long

    suspend fun updateMetadata(track: Track)
    suspend fun updateForeignKeys(id: Long, artistId: Long?, albumId: Long?)
    suspend fun markError(id: Long)

    /**
     * One chunk of tracks whose `canonical_genre` is still null. Used by the
     * lazy backfill that runs once after upgrading to the EQ-enabled build,
     * to populate the column for rows scanned by older app versions.
     */
    suspend fun selectMissingCanonicalGenre(limit: Int): List<TrackGenreRow>

    suspend fun setCanonicalGenre(id: Long, genre: CanonicalGenre)

    suspend fun deleteAll()
}

/** Minimal projection for backfilling [com.offlineplaya.shared.domain.model.CanonicalGenre]. */
data class TrackGenreRow(
    val trackId: Long,
    val rawGenre: String?,
)

/** Minimal projection used by cascading-delete bookkeeping. */
data class TrackAggregateRef(
    val trackId: Long,
    val artistId: Long?,
    val albumId: Long?,
)

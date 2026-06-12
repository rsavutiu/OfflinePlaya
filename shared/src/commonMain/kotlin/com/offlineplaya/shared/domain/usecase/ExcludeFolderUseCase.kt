package com.offlineplaya.shared.domain.usecase

import com.offlineplaya.shared.domain.model.Folder
import com.offlineplaya.shared.domain.repository.AlbumRepository
import com.offlineplaya.shared.domain.repository.ArtistRepository
import com.offlineplaya.shared.domain.repository.ExcludedFolderRepository
import com.offlineplaya.shared.domain.repository.FolderRepository
import com.offlineplaya.shared.domain.repository.TrackRepository
import com.offlineplaya.shared.util.AppLogger

/**
 * Hide a folder from the library: record the exclusion (so future scans skip
 * it — see [LibrarySyncUseCase]) and drop everything already indexed under it,
 * then clean up the artists/albums left empty by the removal. The files on
 * disk are untouched.
 *
 * [include] reverses the record; the caller triggers a rescan to re-index the
 * folder's contents.
 */
class ExcludeFolderUseCase(
    private val excluded: ExcludedFolderRepository,
    private val tracks: TrackRepository,
    private val folders: FolderRepository,
    private val artists: ArtistRepository,
    private val albums: AlbumRepository,
    private val logger: AppLogger,
) {
    suspend fun exclude(folder: Folder) {
        logger.i(TAG, "Excluding '${folder.relativePath}' in ${folder.treeUri}")
        excluded.add(folder.treeUri, folder.relativePath, folder.displayName)

        // Snapshot ancestors and affected artist/album FKs BEFORE deleting —
        // both are unreachable once the rows are gone.
        val ancestors = ancestorIds(folder)
        val touched = tracks.selectAggregatesByPathPrefix(folder.treeUri, folder.relativePath)

        tracks.deleteByPathPrefix(folder.treeUri, folder.relativePath)
        folders.deleteByPathPrefix(folder.treeUri, folder.relativePath)

        touched.mapNotNull { it.albumId }.toSet().forEach { albums.refreshAggregates(it) }
        touched.mapNotNull { it.artistId }.toSet().forEach { artists.refreshCounts(it) }
        albums.deleteEmpty()
        artists.deleteOrphans()
        ancestors.forEach { folders.refreshTrackCount(it) }
        logger.i(TAG, "Excluded '${folder.relativePath}': removed ${touched.size} track(s)")
    }

    /** Forget an exclusion. Caller rescans to re-index the folder. */
    suspend fun include(excludedFolderId: Long) {
        excluded.remove(excludedFolderId)
    }

    private suspend fun ancestorIds(folder: Folder): List<Long> {
        val ids = mutableListOf<Long>()
        var parentId = folder.parentId
        while (parentId != null) {
            ids += parentId
            parentId = folders.findById(parentId)?.parentId
        }
        return ids
    }

    private companion object {
        const val TAG = "ExcludeFolderUseCase"
    }
}

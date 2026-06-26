package com.offlineplaya.shared.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.offlineplaya.shared.data.mapper.trackFromColumns
import com.offlineplaya.shared.database.OfflinePlayaDatabase
import com.offlineplaya.shared.domain.model.PlaybackSnapshot
import com.offlineplaya.shared.domain.model.RepeatMode
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.repository.QueueRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

internal class SqlQueueRepository(
    private val db: OfflinePlayaDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : QueueRepository {

    private val queries get() = db.queueItemQueries

    override fun observeQueue(): Flow<List<Track>> =
        queries.selectAll(::mapQueueRow).asFlow().mapToList(ioDispatcher)

    override suspend fun count(): Long = withContext(ioDispatcher) {
        queries.count().executeAsOne()
    }

    override suspend fun enqueue(trackId: Long, source: String) = withContext(ioDispatcher) {
        queries.transaction {
            val nextPosition = (queries.maxPosition().executeAsOne().maxPosition ?: -1L) + 1
            queries.insert(trackId, nextPosition, source)
        }
    }

    override suspend fun clear() = withContext(ioDispatcher) {
        queries.clear()
    }

    override suspend fun replaceAll(trackIds: List<Long>) = withContext(ioDispatcher) {
        queries.transaction {
            queries.clear()
            trackIds.forEachIndexed { index, trackId ->
                queries.insert(trackId, index.toLong(), SOURCE_RESTORE)
            }
        }
    }

    override suspend fun loadQueue(): List<Track> = withContext(ioDispatcher) {
        queries.selectAll(::mapQueueRow).executeAsList()
    }

    // The transport snapshot rides in the Setting key/value table — it's
    // playback scratch state, not library data, so it doesn't justify a
    // schema migration of its own.
    override suspend fun savePlaybackSnapshot(snapshot: PlaybackSnapshot) =
        withContext(ioDispatcher) {
            db.settingQueries.transaction {
                db.settingQueries.insertOrReplace(KEY_INDEX, snapshot.queueIndex.toString())
                db.settingQueries.insertOrReplace(KEY_POSITION, snapshot.positionMs.toString())
                db.settingQueries.insertOrReplace(KEY_SHUFFLE, snapshot.shuffleEnabled.toString())
                db.settingQueries.insertOrReplace(KEY_REPEAT, snapshot.repeatMode.name)
            }
        }

    override suspend fun loadPlaybackSnapshot(): PlaybackSnapshot? = withContext(ioDispatcher) {
        val index = setting(KEY_INDEX)?.toIntOrNull() ?: return@withContext null
        PlaybackSnapshot(
            queueIndex = index,
            positionMs = setting(KEY_POSITION)?.toLongOrNull() ?: 0L,
            shuffleEnabled = setting(KEY_SHUFFLE)?.toBooleanStrictOrNull() ?: false,
            repeatMode = setting(KEY_REPEAT)
                ?.let { name -> RepeatMode.entries.firstOrNull { it.name == name } }
                ?: RepeatMode.OFF,
        )
    }

    private fun setting(key: String): String? =
        db.settingQueries.selectByKey(key).executeAsOneOrNull()

    private companion object {
        const val SOURCE_RESTORE = "queue"
        const val KEY_INDEX = "queue.index"
        const val KEY_POSITION = "queue.position"
        const val KEY_SHUFFLE = "queue.shuffle"
        const val KEY_REPEAT = "queue.repeat"
    }

    @Suppress("LongParameterList", "UNUSED_PARAMETER")
    private fun mapQueueRow(
        queueItemId: Long,
        position: Long,
        source: String,
        trackId: Long,
        documentUri: String,
        treeUri: String,
        relativePath: String,
        fileName: String,
        fileSize: Long,
        lastModified: Long,
        title: String?,
        artistName: String?,
        albumArtistName: String?,
        albumName: String?,
        genre: String?,
        year: Long?,
        trackNumber: Long?,
        discNumber: Long?,
        durationMs: Long?,
        bitrate: Long?,
        sampleRate: Long?,
        channels: Long?,
        codec: String?,
        artistId: Long?,
        albumId: Long?,
        folderId: Long?,
        scanStatus: String,
        canonicalGenre: String?,
        createdAt: Long,
        updatedAt: Long,
    ): Track = trackFromColumns(
        id = trackId,
        documentUri = documentUri,
        treeUri = treeUri,
        relativePath = relativePath,
        fileName = fileName,
        title = title,
        artistName = artistName,
        albumArtistName = albumArtistName,
        albumName = albumName,
        genre = genre,
        year = year,
        trackNumber = trackNumber,
        discNumber = discNumber,
        durationMs = durationMs,
        bitrate = bitrate,
        sampleRate = sampleRate,
        channels = channels,
        codec = codec,
        artistId = artistId,
        albumId = albumId,
        folderId = folderId,
        scanStatus = scanStatus,
        canonicalGenre = canonicalGenre,
    )
}

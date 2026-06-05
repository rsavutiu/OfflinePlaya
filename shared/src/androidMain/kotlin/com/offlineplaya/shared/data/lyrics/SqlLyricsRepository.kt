package com.offlineplaya.shared.data.lyrics

import com.offlineplaya.shared.database.OfflinePlayaDatabase
import com.offlineplaya.shared.domain.lyrics.EmbeddedLyricsSource
import com.offlineplaya.shared.domain.lyrics.LrcParser
import com.offlineplaya.shared.domain.lyrics.Lyrics
import com.offlineplaya.shared.domain.lyrics.LyricsRepository
import com.offlineplaya.shared.domain.lyrics.RemoteLyricsSource
import com.offlineplaya.shared.domain.lyrics.SidecarLyricsSource
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.repository.SettingsRepository
import com.offlineplaya.shared.util.AppLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [LyricsRepository] orchestrating the resolution chain:
 * positive cache → embedded tags → `.lrc`/`.txt` sidecar → LRCLIB (when the
 * download-remote-lyrics preference is on). The raw source text is run
 * through [LrcParser], and any non-empty result is persisted to the
 * `Lyrics` table so the next play is instant.
 *
 * Local misses are deliberately NOT cached — a user can drop an `.lrc` next
 * to a track later, and the next play should pick it up. Remote misses use
 * the shared `RemoteArtMiss` negative cache, owned by [remote].
 */
internal class SqlLyricsRepository(
    private val db: OfflinePlayaDatabase,
    private val embedded: EmbeddedLyricsSource,
    private val sidecar: SidecarLyricsSource,
    private val remote: RemoteLyricsSource?,
    private val settings: SettingsRepository?,
    private val logger: AppLogger,
    private val now: () -> Long = { System.currentTimeMillis() },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : LyricsRepository {

    private val queries get() = db.lyricsQueries

    override suspend fun lyricsFor(track: Track): Lyrics = withContext(ioDispatcher) {
        cached(track)?.let { return@withContext it }

        // Embedded tags (USLT / Vorbis LYRICS / mp4 ©lyr) — usually plain text.
        resolveAndPersist(track, SOURCE_EMBEDDED) { embedded.read(track) }
            ?.let { return@withContext it }

        // Sidecar .lrc (synced) / .txt (plain) next to the audio file.
        resolveAndPersist(track, SOURCE_SIDECAR) { sidecar.read(track) }
            ?.let { return@withContext it }

        // LRCLIB. Gated by the download-remote-lyrics preference; on first
        // install the default is ON (mirroring artwork.download_remote).
        if (remote != null && downloadRemoteLyricsEnabled()) {
            resolveAndPersist(track, SOURCE_LRCLIB) { remote.resolve(track) }
                ?.let { return@withContext it }
        }

        Lyrics.None
    }

    private fun cached(track: Track): Lyrics? {
        val row = queries.selectByUri(track.documentUri).executeAsOneOrNull() ?: return null
        // Re-parse the stored source text — authoritative and keeps a single
        // code path. (is_synced is stored for possible future querying.)
        return LrcParser.parse(row.raw_text).takeIf { it !is Lyrics.None }
    }

    private suspend fun downloadRemoteLyricsEnabled(): Boolean {
        val s = settings ?: return true
        return runCatching { s.getLyricsPreferences().downloadRemoteLyrics }
            .getOrDefault(true)
    }

    private inline fun resolveAndPersist(
        track: Track,
        source: String,
        read: () -> String?,
    ): Lyrics? {
        val text = read() ?: return null
        val parsed = LrcParser.parse(text)
        if (parsed is Lyrics.None) return null
        queries.insertOrReplace(
            track_document_uri = track.documentUri,
            raw_text = text,
            is_synced = if (parsed is Lyrics.Synced) 1L else 0L,
            source = source,
            fetched_at = now(),
        )
        logger.d(TAG, "Resolved lyrics for ${track.documentUri} via $source")
        return parsed
    }

    private companion object {
        const val TAG = "SqlLyricsRepository"
        const val SOURCE_EMBEDDED = "embedded"
        const val SOURCE_SIDECAR = "sidecar"
        const val SOURCE_LRCLIB = "lrclib"
    }
}

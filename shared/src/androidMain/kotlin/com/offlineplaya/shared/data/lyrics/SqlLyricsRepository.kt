package com.offlineplaya.shared.data.lyrics

import com.offlineplaya.shared.database.OfflinePlayaDatabase
import com.offlineplaya.shared.domain.lyrics.EmbeddedLyricsSource
import com.offlineplaya.shared.domain.lyrics.LrcParser
import com.offlineplaya.shared.domain.lyrics.Lyrics
import com.offlineplaya.shared.domain.lyrics.LyricsRepository
import com.offlineplaya.shared.domain.lyrics.LyricsSidecarWriter
import com.offlineplaya.shared.domain.lyrics.RemoteLyricsSource
import com.offlineplaya.shared.domain.lyrics.SidecarLyricsSource
import com.offlineplaya.shared.domain.model.LyricsPreferences
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
 *
 * When a remote hit lands and the save-sidecar preference is on, the text
 * is also written out via [sidecarWriter] so the lyrics survive a cache
 * wipe and become visible to other players. The write is inline (a few
 * KB at most) and any failure is swallowed by the writer — the in-app
 * cache row is the source of truth.
 */
internal class SqlLyricsRepository(
    private val db: OfflinePlayaDatabase,
    private val embedded: EmbeddedLyricsSource,
    private val sidecar: SidecarLyricsSource,
    private val remote: RemoteLyricsSource?,
    private val sidecarWriter: LyricsSidecarWriter?,
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
        val prefs = lyricsPreferences()
        if (remote != null && prefs.downloadRemoteLyrics) {
            val remoteHit = resolveAndPersist(track, SOURCE_LRCLIB) { remote.resolve(track) }
            if (remoteHit != null) {
                if (sidecarWriter != null && prefs.saveLyricsAsSidecar) {
                    // The Lyrics cache row already has the raw text; the
                    // sidecar write is a best-effort durability step.
                    val rawText = queries.selectByUri(track.documentUri)
                        .executeAsOneOrNull()?.raw_text
                    if (rawText != null) {
                        sidecarWriter.write(
                            track = track,
                            text = rawText,
                            isSynced = remoteHit is Lyrics.Synced,
                        )
                    }
                }
                return@withContext remoteHit
            }
        }

        Lyrics.None
    }

    private fun cached(track: Track): Lyrics? {
        val row = queries.selectByUri(track.documentUri).executeAsOneOrNull() ?: return null
        // Re-parse the stored source text — authoritative and keeps a single
        // code path. (is_synced is stored for possible future querying.)
        return LrcParser.parse(row.raw_text).takeIf { it !is Lyrics.None }
    }

    private suspend fun lyricsPreferences(): LyricsPreferences {
        val s = settings ?: return LyricsPreferences.Default
        return runCatching { s.getLyricsPreferences() }
            .getOrDefault(LyricsPreferences.Default)
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

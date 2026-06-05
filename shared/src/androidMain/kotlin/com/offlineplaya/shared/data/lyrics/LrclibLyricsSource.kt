package com.offlineplaya.shared.data.lyrics

import com.offlineplaya.shared.database.OfflinePlayaDatabase
import com.offlineplaya.shared.domain.lyrics.RemoteLyricsSource
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * [RemoteLyricsSource] backed by LRCLIB (https://lrclib.net) — a free,
 * no-API-key crowdsourced LRC database used by the Lyricsify / LX Music
 * apps. The HTTP API returns JSON with both `syncedLyrics` (LRC) and
 * `plainLyrics`; we prefer the synced text when available.
 *
 * Match strategy:
 *  1. `/api/get?track_name=&artist_name=&album_name=&duration=` — exact
 *     match by metadata + duration. LRCLIB returns 404 if nothing matches
 *     to within a few seconds.
 *  2. `/api/search?track_name=&artist_name=` — broader lookup, then we
 *     pick the candidate whose duration is closest to the track. Without
 *     a duration filter LRCLIB sometimes returns wrong-album rips.
 *
 * Negative cache: reuses the shared [com.offlineplaya.shared.database.RemoteArtMiss]
 * table with a `lyrics:` key prefix so a clean miss doesn't translate
 * into a fresh HTTPS round-trip every time the user revisits Now Playing.
 *
 * Concurrency: a small [Semaphore] caps in-flight HTTP calls so a screen
 * full of track switches doesn't open dozens of sockets. LRCLIB publishes
 * no public rate limit, so we don't gate or delay further — the natural
 * pace (one resolve per track change) plus the negative cache should keep
 * us well within polite use without the MusicBrainz-style 1 rps mutex.
 */
internal class LrclibLyricsSource(
    private val httpClient: OkHttpClient,
    private val json: Json,
    private val db: OfflinePlayaDatabase,
    private val logger: AppLogger,
) : RemoteLyricsSource {

    private val concurrencyLimit = Semaphore(MAX_CONCURRENT_LOOKUPS)
    private val missKeysLock = Mutex()
    private val missKeys = mutableSetOf<String>()

    override suspend fun resolve(track: Track): String? {
        val artist = (track.albumArtistName ?: track.artistName).trim()
        val album = track.albumName.trim()
        val title = track.title.trim()
        if (artist.isEmpty() || title.isEmpty()) {
            logger.d(TAG, "Skipping lookup — missing artist/title")
            return null
        }
        val durationSec = track.durationMs?.let { (it / 1000L).toInt() } ?: 0
        val key = lookupKey(artist, album, title, durationSec)

        if (isKnownMiss(key)) {
            logger.d(TAG, "resolve('$artist' / '$title') skipped — persistent miss")
            return null
        }

        return concurrencyLimit.withPermit {
            if (isKnownMiss(key)) return@withPermit null

            val viaGet = runCatching { fetchViaGet(artist, album, title, durationSec) }
                .onFailure { logger.w(TAG, "LRCLIB /get failed for '$artist' / '$title': ${it.message}") }
            val gotResult = viaGet.getOrNull()
            if (gotResult != null) {
                logger.d(TAG, "resolve('$artist' / '$title') via /get (${gotResult.length} chars)")
                return@withPermit gotResult
            }

            val viaSearch = runCatching { fetchViaSearch(artist, title, durationSec) }
                .onFailure { logger.w(TAG, "LRCLIB /search failed for '$artist' / '$title': ${it.message}") }
            val searchResult = viaSearch.getOrNull()
            if (searchResult != null) {
                logger.d(TAG, "resolve('$artist' / '$title') via /search (${searchResult.length} chars)")
                return@withPermit searchResult
            }

            // Only persist a miss when neither call threw — a transient network
            // error shouldn't blacklist the track for 30 days.
            if (viaGet.isSuccess && viaSearch.isSuccess) {
                recordMiss(key)
                logger.d(TAG, "resolve('$artist' / '$title') clean miss across LRCLIB")
            }
            null
        }
    }

    private suspend fun fetchViaGet(
        artist: String,
        album: String,
        title: String,
        durationSec: Int,
    ): String? = withContext(Dispatchers.IO) {
        val params = buildString {
            append("track_name=").append(title.urlEncode())
            append("&artist_name=").append(artist.urlEncode())
            if (album.isNotEmpty()) append("&album_name=").append(album.urlEncode())
            if (durationSec > 0) append("&duration=").append(durationSec)
        }
        val url = "$BASE_URL/api/get?$params"
        val body = doRequest(url) ?: return@withContext null
        val parsed = runCatching { json.decodeFromString<LrcLibRow>(body) }
            .getOrElse {
                logger.d(TAG, "/get parse failed: ${it.message}")
                return@withContext null
            }
        parsed.bestText()
    }

    private suspend fun fetchViaSearch(
        artist: String,
        title: String,
        durationSec: Int,
    ): String? = withContext(Dispatchers.IO) {
        val params = "track_name=${title.urlEncode()}&artist_name=${artist.urlEncode()}"
        val url = "$BASE_URL/api/search?$params"
        val body = doRequest(url) ?: return@withContext null
        val rows = runCatching { json.decodeFromString<List<LrcLibRow>>(body) }
            .getOrElse {
                logger.d(TAG, "/search parse failed: ${it.message}")
                return@withContext null
            }
        if (rows.isEmpty()) return@withContext null
        val best = if (durationSec > 0) {
            rows.minByOrNull { abs((it.durationSec()) - durationSec) }
        } else {
            rows.first()
        } ?: return@withContext null
        // Bail if the closest duration is >15s off — that's almost certainly
        // a different recording / remix and would scroll out of sync.
        if (durationSec > 0) {
            val delta = abs(best.durationSec() - durationSec)
            if (delta > MAX_DURATION_DELTA_SEC) {
                logger.d(TAG, "Best /search match off by ${delta}s — skipping")
                return@withContext null
            }
        }
        best.bestText()
    }

    private fun doRequest(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .build()
        return httpClient.newCall(request).execute().use { response ->
            when {
                response.isSuccessful -> response.body?.string()
                // 404 from /get is the documented "no match" response — not
                // an error worth bubbling up.
                response.code == 404 -> null
                else -> throw LyricsLookupException("LRCLIB HTTP ${response.code} for $url")
            }
        }
    }

    private suspend fun isKnownMiss(key: String): Boolean {
        missKeysLock.withLock { if (key in missKeys) return true }
        val cutoff = System.currentTimeMillis() - MISS_EXPIRY_MS
        val hit = db.remoteArtMissQueries.isMiss(key, cutoff).executeAsOneOrNull()
        if (hit != null) missKeysLock.withLock { missKeys.add(key) }
        return hit != null
    }

    private suspend fun recordMiss(key: String) {
        missKeysLock.withLock { missKeys.add(key) }
        db.remoteArtMissQueries.insertMiss(key, System.currentTimeMillis())
    }

    private fun lookupKey(artist: String, album: String, title: String, durationSec: Int): String =
        "v${SOURCE_VERSION}:lyrics:${artist.lowercase()}|${album.lowercase()}|${title.lowercase()}|$durationSec"

    @Serializable
    private data class LrcLibRow(
        val id: Long? = null,
        val trackName: String? = null,
        val artistName: String? = null,
        val albumName: String? = null,
        // LRCLIB serialises duration as a Double (e.g. 295.0), so we can't
        // declare it Int here — kotlinx.serialization would reject the
        // number type at decode and the whole row would fail to parse.
        val duration: Double? = null,
        val instrumental: Boolean = false,
        val plainLyrics: String? = null,
        val syncedLyrics: String? = null,
    ) {
        fun durationSec(): Int = (duration ?: 0.0).toInt()

        fun bestText(): String? {
            // Instrumental rows are intentional empty results — treat as
            // "no lyrics", not as a hit to cache.
            if (instrumental) return null
            val synced = syncedLyrics?.takeIf { it.isNotBlank() }
            if (synced != null) return synced
            return plainLyrics?.takeIf { it.isNotBlank() }
        }
    }

    private class LyricsLookupException(message: String) : Exception(message)

    private fun String.urlEncode(): String = java.net.URLEncoder.encode(this, "UTF-8")

    private companion object {
        const val TAG = "LrclibLyrics"
        const val BASE_URL = "https://lrclib.net"
        const val USER_AGENT =
            "OfflinePlaya/0.1.0 ( https://github.com/rsavutiu/offlineplaya )"
        const val MAX_CONCURRENT_LOOKUPS = 4
        const val MAX_DURATION_DELTA_SEC = 15
        const val MISS_EXPIRY_MS = 30L * 24 * 60 * 60 * 1_000 // 30 days
        const val SOURCE_VERSION = 1
    }
}

/**
 * Factory mirroring [com.offlineplaya.shared.data.image.createMusicBrainzSource].
 * Uses a dedicated [OkHttpClient] so LRCLIB calls share a connection pool +
 * timeouts independent of the art-fetch client. No on-disk HTTP cache — the
 * resolved lyrics are persisted in the `Lyrics` SQL table, and misses in
 * `RemoteArtMiss`, so the OkHttp cache wouldn't earn its memory footprint.
 */
internal fun createLrclibLyricsSource(
    db: OfflinePlayaDatabase,
    logger: AppLogger,
): LrclibLyricsSource {
    val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    val json = Json { ignoreUnknownKeys = true }
    return LrclibLyricsSource(client, json, db, logger)
}

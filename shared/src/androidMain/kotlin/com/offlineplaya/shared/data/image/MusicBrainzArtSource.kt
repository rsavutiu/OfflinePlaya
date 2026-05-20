package com.offlineplaya.shared.data.image

import co.touchlab.kermit.Logger
import com.offlineplaya.shared.domain.image.RemoteArtSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * [RemoteArtSource] backed by MusicBrainz (release matching) + Cover Art
 * Archive (the JPEG itself). Free, no API key, but rate-limited at ~1
 * req/sec — we serialize calls through a [Mutex] with a small delay so we
 * stay polite.
 *
 * Match strategy:
 *  1. Query MusicBrainz release-group search with the album + artist.
 *  2. Pick the highest-scored result whose `primary-type` is "Album" (so
 *     compilations and EPs don't outrank the canonical pressing).
 *  3. GET `coverartarchive.org/release-group/{mbid}/front-500` for a 500px
 *     thumbnail — small enough to be quick, large enough for our 200dp
 *     `NowPlayingPage` art panel.
 *
 * In-memory cache by `"artist|album"` so repeated lookups within a session
 * don't re-hit either API.
 */
internal class MusicBrainzArtSource(
    private val httpClient: OkHttpClient,
    private val json: Json,
) : RemoteArtSource {

    private val mutex = Mutex()
    private val cache = mutableMapOf<String, ByteArray?>()
    private val log = Logger.withTag("MusicBrainzArt")

    override suspend fun resolve(artist: String, album: String): ByteArray? {
        val key = "${artist.lowercase()}|${album.lowercase()}"
        cache[key]?.let { return it }
        // Cache hit on a previous miss as well.
        if (cache.containsKey(key)) return null

        return mutex.withLock {
            // Re-check inside the lock — another caller may have populated.
            cache[key]?.let { return@withLock it }
            if (cache.containsKey(key)) return@withLock null

            val bytes = runCatching { fetchInternal(artist, album) }
                .onFailure { log.w(it) { "Lookup failed for $artist / $album" } }
                .getOrNull()
            cache[key] = bytes
            // Stay under the 1 req/sec policy.
            delay(RATE_LIMIT_DELAY_MS)
            bytes
        }
    }

    private suspend fun fetchInternal(artist: String, album: String): ByteArray? =
        withContext(Dispatchers.IO) {
            val mbid = findReleaseGroupMbid(artist, album) ?: return@withContext null
            downloadCover(mbid)
        }

    private fun findReleaseGroupMbid(artist: String, album: String): String? {
        val query = "release:\"${album.escapeLucene()}\" AND artist:\"${artist.escapeLucene()}\""
        val url = "https://musicbrainz.org/ws/2/release-group/?query=${query.urlEncode()}&fmt=json&limit=10"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .build()
        val body = httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                log.d { "MBz release-group search returned ${response.code} for $artist / $album" }
                return null
            }
            response.body?.string() ?: return null
        }
        val parsed = runCatching { json.decodeFromString<ReleaseGroupSearchResponse>(body) }
            .getOrElse {
                log.d(it) { "Failed to parse MBz search response" }
                return null
            }
        // Prefer Album / Soundtrack over EP / Single / Compilation.
        val preferred = parsed.releaseGroups
            .sortedWith(
                compareByDescending<ReleaseGroup> { it.primaryType == "Album" }
                    .thenByDescending { it.score },
            )
            .firstOrNull()
        return preferred?.id
    }

    private fun downloadCover(releaseGroupMbid: String): ByteArray? {
        val url = "https://coverartarchive.org/release-group/$releaseGroupMbid/front-500"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()
        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                log.d { "CAA returned ${response.code} for $releaseGroupMbid" }
                null
            } else {
                response.body?.bytes()
            }
        }
    }

    @Serializable
    private data class ReleaseGroupSearchResponse(
        @kotlinx.serialization.SerialName("release-groups")
        val releaseGroups: List<ReleaseGroup> = emptyList(),
    )

    @Serializable
    private data class ReleaseGroup(
        val id: String,
        val score: Int = 0,
        @kotlinx.serialization.SerialName("primary-type")
        val primaryType: String? = null,
    )

    private fun String.escapeLucene(): String =
        replace("\\", "\\\\").replace("\"", "\\\"")

    private fun String.urlEncode(): String =
        java.net.URLEncoder.encode(this, Charsets.UTF_8)

    private companion object {
        const val USER_AGENT =
            "OfflinePlaya/0.1.0 ( https://github.com/rsavutiu/offlineplaya )"
        const val RATE_LIMIT_DELAY_MS = 1_100L
    }
}

/** Public factory so DI doesn't have to know how to build the OkHttp client. */
fun createMusicBrainzArtSource(): RemoteArtSource {
    val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    val json = Json { ignoreUnknownKeys = true }
    return MusicBrainzArtSource(client, json)
}

package com.offlineplaya.shared.data.image

import co.touchlab.kermit.Logger
import com.offlineplaya.shared.database.OfflinePlayaDatabase
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
    private val db: OfflinePlayaDatabase,
) : RemoteArtSource {

    private val mutex = Mutex()
    private val missKeys = mutableSetOf<String>()
    private val log = Logger.withTag("MusicBrainzArt")

    private data class SourceAttempt(val name: String, val fetch: suspend () -> ByteArray?)

    override suspend fun resolve(artist: String, album: String): ByteArray? {
        val key = "v${SOURCE_VERSION}:album:${artist.lowercase()}|${album.lowercase()}"
        if (isKnownMiss(key)) {
            log.d { "resolve('$artist' / '$album') skipped — persistent miss" }
            return null
        }

        return mutex.withLock {
            if (isKnownMiss(key)) return@withLock null

            val sources = listOf(
                SourceAttempt("Deezer") { fetchViaDeezer(artist, album) },
                SourceAttempt("MusicBrainz/CAA") { fetchViaMusicBrainz(artist, album) },
                SourceAttempt("OpenLibrary") { fetchViaOpenLibrary(artist, album) },
                SourceAttempt("GoogleBooks") { fetchViaGoogleBooks(artist, album) },
            )

            var anyTransient = false
            var bytes: ByteArray? = null
            for (source in sources) {
                log.d { "resolve('$artist' / '$album') trying ${source.name}..." }
                val result = runCatching { source.fetch() }
                result.exceptionOrNull()?.let {
                    log.w(it) { "${source.name} lookup failed for $artist / $album" }
                    anyTransient = true
                }
                bytes = result.getOrNull()
                if (bytes != null) break
            }

            if (bytes == null) {
                if (!anyTransient) {
                    recordMiss(key)
                    log.d { "resolve('$artist' / '$album') clean miss across all sources — saved to DB" }
                } else {
                    log.d { "resolve('$artist' / '$album') had transient errors — not persisting miss" }
                }
            } else {
                log.d { "resolve('$artist' / '$album') got ${bytes.size} bytes" }
            }
            delay(RATE_LIMIT_DELAY_MS)
            bytes
        }
    }

    override suspend fun resolveArtistImage(artist: String): String? {
        val key = "v${SOURCE_VERSION}:artist:${artist.lowercase()}"
        if (isKnownMiss(key)) {
            log.d { "resolveArtistImage('$artist') skipped — persistent miss" }
            return null
        }

        return mutex.withLock {
            if (isKnownMiss(key)) return@withLock null

            log.d { "resolveArtistImage('$artist') querying Deezer..." }
            val result = runCatching { findArtistImageViaDeezer(artist) }
            result.exceptionOrNull()?.let {
                log.w(it) { "resolveArtistImage('$artist') failed" }
            }
            val url = result.getOrNull()
            if (url == null && result.isSuccess) {
                recordMiss(key)
                log.d { "resolveArtistImage('$artist') clean miss — saved to DB" }
            }
            delay(RATE_LIMIT_DELAY_MS)
            url
        }
    }

    private suspend fun findArtistImageViaDeezer(artist: String): String? =
        withContext(Dispatchers.IO) {
            val encoded = java.net.URLEncoder.encode(artist, Charsets.UTF_8)
            val url = "https://api.deezer.com/search/artist?q=$encoded&limit=3"
            log.d { "findArtistImageViaDeezer('$artist') -> $url" }
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()
            httpClient.newCall(request).execute().use { response ->
                log.d { "findArtistImageViaDeezer('$artist') HTTP ${response.code}" }
                if (!response.isSuccessful) throw ArtLookupException("Deezer HTTP ${response.code}")
                val body = response.body?.string() ?: return@withContext null
                parseDeezerArtistImage(artist, body)
            }
        }

    private fun parseDeezerArtistImage(artist: String, body: String): String? {
        try {
            val parsed = json.decodeFromString<DeezerArtistSearchResponse>(body)
            val match = parsed.data.firstOrNull { entry ->
                entry.name.equals(artist, ignoreCase = true)
            } ?: parsed.data.firstOrNull()

            if (match == null) {
                log.d { "parseDeezerArtistImage('$artist') no results" }
                return null
            }
            val imageUrl = match.pictureBig ?: match.pictureMedium
            if (imageUrl == null || !imageUrl.startsWith("http")) {
                log.d { "parseDeezerArtistImage('$artist') no usable picture URL" }
                return null
            }
            log.d { "parseDeezerArtistImage('$artist') matched '${match.name}' -> $imageUrl" }
            return imageUrl
        } catch (e: Exception) {
            log.e(e) { "parseDeezerArtistImage('$artist') parse failed" }
            return null
        }
    }

    private fun isKnownMiss(key: String): Boolean {
        if (key in missKeys) return true
        val cutoff = System.currentTimeMillis() - MISS_EXPIRY_MS
        val hit = db.remoteArtMissQueries.isMiss(key, cutoff).executeAsOneOrNull()
        if (hit != null) missKeys.add(key)
        return hit != null
    }

    private fun recordMiss(key: String) {
        missKeys.add(key)
        db.remoteArtMissQueries.insertMiss(key, System.currentTimeMillis())
    }

    private suspend fun fetchViaDeezer(artist: String, album: String): ByteArray? =
        withContext(Dispatchers.IO) {
            val query = "$artist $album"
            val encoded = java.net.URLEncoder.encode(query, Charsets.UTF_8)
            val url = "https://api.deezer.com/search/album?q=$encoded&limit=5"
            log.d { "fetchViaDeezer('$artist' / '$album') -> $url" }
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()
            val body = httpClient.newCall(request).execute().use { response ->
                log.d { "fetchViaDeezer HTTP ${response.code}" }
                if (!response.isSuccessful) throw ArtLookupException("Deezer HTTP ${response.code}")
                response.body?.string() ?: return@withContext null
            }

            val coverUrl = parseDeezerAlbumCover(artist, album, body)
            if (coverUrl == null) {
                log.d { "fetchViaDeezer no cover URL found" }
                return@withContext null
            }
            log.d { "fetchViaDeezer downloading $coverUrl" }
            val imgRequest = Request.Builder()
                .url(coverUrl)
                .header("User-Agent", USER_AGENT)
                .build()
            httpClient.newCall(imgRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    log.d { "fetchViaDeezer image download ${response.code}" }
                    null
                } else {
                    response.body?.bytes()
                }
            }
        }

    private fun parseDeezerAlbumCover(artist: String, album: String, body: String): String? {
        val parsed = runCatching { json.decodeFromString<DeezerAlbumSearchResponse>(body) }
            .getOrElse {
                log.d(it) { "parseDeezerAlbumCover parse failed" }
                return null
            }
        val exactMatch = parsed.data.firstOrNull { entry ->
            entry.title.equals(album, ignoreCase = true) &&
                entry.artist?.name?.equals(artist, ignoreCase = true) == true
        }
        val match = exactMatch ?: parsed.data.firstOrNull()
        if (match == null) {
            log.d { "parseDeezerAlbumCover no results for '$artist' / '$album'" }
            return null
        }
        log.d { "parseDeezerAlbumCover matched '${match.title}' by '${match.artist?.name}'" }
        return match.coverBig ?: match.coverMedium
    }

    private suspend fun fetchViaMusicBrainz(artist: String, album: String): ByteArray? =
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
                throw ArtLookupException("MBz HTTP ${response.code} for $artist / $album")
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
            when {
                response.isSuccessful -> response.body?.bytes()
                response.code in 400..499 -> {
                    log.d { "CAA returned ${response.code} for $releaseGroupMbid" }
                    null
                }
                else -> throw ArtLookupException("CAA HTTP ${response.code}")
            }
        }
    }

    // ── Open Library (audiobooks, books) ─────────────────────────────

    private suspend fun fetchViaOpenLibrary(artist: String, album: String): ByteArray? =
        withContext(Dispatchers.IO) {
            val titleEnc = java.net.URLEncoder.encode(album, Charsets.UTF_8)
            val authorEnc = java.net.URLEncoder.encode(artist, Charsets.UTF_8)
            val url = "https://openlibrary.org/search.json?title=$titleEnc&author=$authorEnc&limit=3&fields=title,author_name,cover_i"
            log.d { "fetchViaOpenLibrary('$artist' / '$album') -> $url" }
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()
            val body = httpClient.newCall(request).execute().use { response ->
                log.d { "fetchViaOpenLibrary HTTP ${response.code}" }
                if (!response.isSuccessful) throw ArtLookupException("OpenLibrary HTTP ${response.code}")
                response.body?.string() ?: return@withContext null
            }
            val coverId = parseOpenLibraryCoverId(album, body)
            if (coverId == null) {
                log.d { "fetchViaOpenLibrary no cover_i found" }
                return@withContext null
            }
            val coverUrl = "https://covers.openlibrary.org/b/id/$coverId-L.jpg"
            log.d { "fetchViaOpenLibrary downloading $coverUrl" }
            downloadImage(coverUrl)
        }

    private fun parseOpenLibraryCoverId(album: String, body: String): Long? {
        val parsed = runCatching { json.decodeFromString<OpenLibrarySearchResponse>(body) }
            .getOrElse {
                log.d(it) { "parseOpenLibraryCoverId parse failed" }
                return null
            }
        if (parsed.docs.isEmpty()) return null
        val exactMatch = parsed.docs.firstOrNull { doc ->
            doc.title.equals(album, ignoreCase = true) && doc.coverId != null
        }
        val match = exactMatch ?: parsed.docs.firstOrNull { it.coverId != null }
        return match?.coverId
    }

    // ── Google Books (audiobooks, books) ─────────────────────────────

    private suspend fun fetchViaGoogleBooks(artist: String, album: String): ByteArray? =
        withContext(Dispatchers.IO) {
            val titleEnc = java.net.URLEncoder.encode(album, Charsets.UTF_8)
            val authorEnc = java.net.URLEncoder.encode(artist, Charsets.UTF_8)
            val url = "https://www.googleapis.com/books/v1/volumes?q=$titleEnc+inauthor:$authorEnc&maxResults=3"
            log.d { "fetchViaGoogleBooks('$artist' / '$album') -> $url" }
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()
            val body = httpClient.newCall(request).execute().use { response ->
                log.d { "fetchViaGoogleBooks HTTP ${response.code}" }
                if (!response.isSuccessful) throw ArtLookupException("GoogleBooks HTTP ${response.code}")
                response.body?.string() ?: return@withContext null
            }
            val thumbUrl = parseGoogleBooksThumbnail(album, body)
            if (thumbUrl == null) {
                log.d { "fetchViaGoogleBooks no thumbnail found" }
                return@withContext null
            }
            log.d { "fetchViaGoogleBooks downloading $thumbUrl" }
            downloadImage(thumbUrl)
        }

    private fun parseGoogleBooksThumbnail(album: String, body: String): String? {
        val parsed = runCatching { json.decodeFromString<GoogleBooksResponse>(body) }
            .getOrElse {
                log.d(it) { "parseGoogleBooksThumbnail parse failed" }
                return null
            }
        val items = parsed.items ?: return null
        if (items.isEmpty()) return null
        val exactMatch = items.firstOrNull { item ->
            item.volumeInfo.title.equals(album, ignoreCase = true) &&
                item.volumeInfo.imageLinks?.thumbnail != null
        }
        val match = exactMatch ?: items.firstOrNull { it.volumeInfo.imageLinks?.thumbnail != null }
        val url = match?.volumeInfo?.imageLinks?.thumbnail ?: return null
        return url.replace("http://", "https://").replace("&edge=curl", "")
    }

    // ── Shared image download ────────────────────────────────────────

    private fun downloadImage(url: String): ByteArray? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()
        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                log.d { "downloadImage $url -> ${response.code}" }
                null
            } else {
                response.body?.bytes()
            }
        }
    }

    // ── Data classes ─────────────────────────────────────────────────

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

    @Serializable
    private data class DeezerArtistSearchResponse(
        val data: List<DeezerArtist> = emptyList(),
    )

    @Serializable
    private data class DeezerArtist(
        val name: String,
        @kotlinx.serialization.SerialName("picture_medium")
        val pictureMedium: String? = null,
        @kotlinx.serialization.SerialName("picture_big")
        val pictureBig: String? = null,
    )

    @Serializable
    private data class DeezerAlbumSearchResponse(
        val data: List<DeezerAlbum> = emptyList(),
    )

    @Serializable
    private data class DeezerAlbum(
        val title: String,
        val artist: DeezerAlbumArtist? = null,
        @kotlinx.serialization.SerialName("cover_medium")
        val coverMedium: String? = null,
        @kotlinx.serialization.SerialName("cover_big")
        val coverBig: String? = null,
    )

    @Serializable
    private data class DeezerAlbumArtist(
        val name: String,
    )

    // Open Library
    @Serializable
    private data class OpenLibrarySearchResponse(
        val docs: List<OpenLibraryDoc> = emptyList(),
    )

    @Serializable
    private data class OpenLibraryDoc(
        val title: String,
        @kotlinx.serialization.SerialName("cover_i")
        val coverId: Long? = null,
    )

    // Google Books
    @Serializable
    private data class GoogleBooksResponse(
        val items: List<GoogleBooksItem>? = null,
    )

    @Serializable
    private data class GoogleBooksItem(
        val volumeInfo: GoogleBooksVolumeInfo,
    )

    @Serializable
    private data class GoogleBooksVolumeInfo(
        val title: String,
        val imageLinks: GoogleBooksImageLinks? = null,
    )

    @Serializable
    private data class GoogleBooksImageLinks(
        val thumbnail: String? = null,
    )

    private class ArtLookupException(message: String) : Exception(message)

    private fun String.escapeLucene(): String =
        replace("\\", "\\\\").replace("\"", "\\\"")

    private fun String.urlEncode(): String =
        java.net.URLEncoder.encode(this, Charsets.UTF_8)

    private companion object {
        const val USER_AGENT =
            "OfflinePlaya/0.1.0 ( https://github.com/rsavutiu/offlineplaya )"
        const val RATE_LIMIT_DELAY_MS = 1_100L
        const val MISS_EXPIRY_MS = 30L * 24 * 60 * 60 * 1_000 // 30 days
        const val SOURCE_VERSION = 3 // bump when adding/removing sources to invalidate old misses
    }
}

/** Public factory so DI doesn't have to know how to build the OkHttp client. */
fun createMusicBrainzArtSource(db: OfflinePlayaDatabase): RemoteArtSource {
    val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    val json = Json { ignoreUnknownKeys = true }
    return MusicBrainzArtSource(client, json, db)
}

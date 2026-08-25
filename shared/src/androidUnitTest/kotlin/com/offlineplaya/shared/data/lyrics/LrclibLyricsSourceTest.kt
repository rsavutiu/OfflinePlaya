package com.offlineplaya.shared.data.lyrics

import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.testsupport.createInMemoryDatabase
import com.offlineplaya.shared.util.TestLogger
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Component tests for [LrclibLyricsSource]. A canned OkHttp [Interceptor]
 * stands in for the network so we can assert the request URL and decide
 * the response without spinning up MockWebServer (which isn't yet a test
 * dependency on this module).
 */
class LrclibLyricsSourceTest {

    private fun source(handler: (String) -> StubResponse): LrclibLyricsSource {
        val seenUrls = mutableListOf<String>()
        val client = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val url = chain.request().url.toString()
                seenUrls += url
                val (code, body) = handler(url)
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message(if (code == 200) "OK" else "ERR")
                    .body(body.toResponseBody("application/json".toMediaTypeOrNull()))
                    .build()
            })
            .build()
        urlsCaptured = seenUrls
        return LrclibLyricsSource(
            httpClient = client,
            json = Json { ignoreUnknownKeys = true },
            db = createInMemoryDatabase(),
            logger = TestLogger(),
        )
    }

    private data class StubResponse(val code: Int, val body: String)

    private lateinit var urlsCaptured: MutableList<String>

    private fun track(
        title: String = "Fix You",
        artist: String = "Coldplay",
        album: String = "X&Y",
        durationMs: Long? = 295_000L,
        uri: String = "content://track/1",
    ) = Track(
        id = 1, documentUri = uri, treeUri = "content://tree", relativePath = "x/y.mp3",
        fileName = "y.mp3", title = title, artistName = artist, albumArtistName = null,
        albumName = album, genre = null, year = null, trackNumber = null, discNumber = null,
        durationMs = durationMs, bitrate = null, sampleRate = null, channels = null, codec = null,
        artistId = null, albumId = null, folderId = null, scanStatus = ScanStatus.SCANNED,
    )

    private val syncedHit = """
        {"id":1,"trackName":"Fix You","artistName":"Coldplay","albumName":"X&Y","duration":295,
         "instrumental":false,"plainLyrics":"plain","syncedLyrics":"[00:01.00]Lights"}
    """.trimIndent()

    private val plainOnly = """
        {"id":2,"trackName":"Fix You","artistName":"Coldplay","albumName":"X&Y","duration":295,
         "instrumental":false,"plainLyrics":"just plain","syncedLyrics":null}
    """.trimIndent()

    @Test
    fun `get endpoint hit returns synced text`() = runTest {
        val src = source { _ -> StubResponse(200, syncedHit) }
        val result = src.resolve(track())
        assertEquals("[00:01.00]Lights", result)
        assertEquals(1, urlsCaptured.size)
        assertTrue(urlsCaptured[0].contains("/api/get?"), "expected /get URL, got ${urlsCaptured[0]}")
        assertTrue(urlsCaptured[0].contains("duration=295"))
    }

    @Test
    fun `prefers synced over plain when both present`() = runTest {
        val src = source { _ -> StubResponse(200, syncedHit) }
        assertEquals("[00:01.00]Lights", src.resolve(track()))
    }

    @Test
    fun `falls back to plain when synced is null`() = runTest {
        val src = source { _ -> StubResponse(200, plainOnly) }
        assertEquals("just plain", src.resolve(track()))
    }

    @Test
    fun `404 from get falls through to search`() = runTest {
        val src = source { url ->
            if (url.contains("/api/get")) StubResponse(404, "")
            else StubResponse(200, "[$syncedHit]")
        }
        assertEquals("[00:01.00]Lights", src.resolve(track()))
        assertEquals(2, urlsCaptured.size)
        assertTrue(urlsCaptured[0].contains("/api/get?"))
        assertTrue(urlsCaptured[1].contains("/api/search?"))
    }

    @Test
    fun `404 from get and empty search returns null`() = runTest {
        val src = source { url ->
            if (url.contains("/api/get")) StubResponse(404, "")
            else StubResponse(200, "[]")
        }
        assertNull(src.resolve(track()))
    }

    @Test
    fun `search picks closest duration match`() = runTest {
        val candidates = """
            [
              {"id":10,"trackName":"Fix You","artistName":"Coldplay","duration":180,"syncedLyrics":"[00:01.00]wrong"},
              {"id":11,"trackName":"Fix You","artistName":"Coldplay","duration":294,"syncedLyrics":"[00:01.00]right"},
              {"id":12,"trackName":"Fix You","artistName":"Coldplay","duration":400,"syncedLyrics":"[00:01.00]wrong2"}
            ]
        """.trimIndent()
        val src = source { url ->
            if (url.contains("/api/get")) StubResponse(404, "")
            else StubResponse(200, candidates)
        }
        assertEquals("[00:01.00]right", src.resolve(track(durationMs = 295_000L)))
    }

    @Test
    fun `search bails when closest match is too far off duration`() = runTest {
        val farOff = """
            [{"id":99,"trackName":"Fix You","artistName":"Coldplay","duration":120,"syncedLyrics":"[00:01.00]wrong"}]
        """.trimIndent()
        val src = source { url ->
            if (url.contains("/api/get")) StubResponse(404, "")
            else StubResponse(200, farOff)
        }
        assertNull(src.resolve(track(durationMs = 295_000L)))
    }

    @Test
    fun `instrumental hit is treated as null`() = runTest {
        val instrumental = """
            {"id":1,"trackName":"X","artistName":"Y","duration":200,"instrumental":true,
             "plainLyrics":null,"syncedLyrics":null}
        """.trimIndent()
        val src = source { _ -> StubResponse(200, instrumental) }
        assertNull(src.resolve(track(durationMs = 200_000L)))
    }

    @Test
    fun `missing title or artist short-circuits without HTTP`() = runTest {
        val src = source { _ -> StubResponse(500, "should not be reached") }
        assertNull(src.resolve(track(title = "", artist = "Someone")))
        assertNull(src.resolve(track(title = "Song", artist = "")))
        assertTrue(urlsCaptured.isEmpty(), "no HTTP call expected")
    }

    @Test
    fun `negative cache short-circuits the second clean miss`() = runTest {
        var calls = 0
        val src = source { url ->
            calls++
            if (url.contains("/api/get")) StubResponse(404, "")
            else StubResponse(200, "[]")
        }
        val track = track()
        assertNull(src.resolve(track))
        val callsAfterFirst = calls
        assertNull(src.resolve(track))
        assertEquals(callsAfterFirst, calls, "second lookup must hit cache, not network")
    }

    @Test
    fun `falls back to de-noised title when raw tags miss`() = runTest {
        // Raw "Hey Jude (2014 Remaster)" misses everywhere; the cleaned
        // "Hey Jude" variant hits via /get on the second pass.
        val hit = """
            {"id":7,"trackName":"Hey Jude","artistName":"The Beatles","duration":425,
             "instrumental":false,"syncedLyrics":"[00:01.00]Hey Jude"}
        """.trimIndent()
        val src = source { url ->
            when {
                url.contains("Remaster") -> StubResponse(404, "")
                url.contains("/api/get") -> StubResponse(200, hit)
                else -> StubResponse(200, "[]")
            }
        }
        val result = src.resolve(
            track(title = "Hey Jude (2014 Remaster)", artist = "The Beatles", album = "1", durationMs = 425_000L),
        )
        assertEquals("[00:01.00]Hey Jude", result)
        assertTrue(
            urlsCaptured.any { it.contains("Hey+Jude") && !it.contains("Remaster") },
            "expected a cleaned-title lookup, saw $urlsCaptured",
        )
    }

    @Test
    fun `clean-title miss still escalates by dropping the album`() = runTest {
        val src = source { url ->
            if (url.contains("/api/get")) StubResponse(404, "")
            else StubResponse(200, "[]")
        }
        assertNull(src.resolve(track()))
        // (album, title) → /get + /search, then the album-dropped variant →
        // /get only (the /search is deduped by title). Three calls total: an
        // album-tag mismatch is a common miss cause, so we retry without it.
        assertEquals(3, urlsCaptured.size, "expected album-drop escalation, saw $urlsCaptured")
        assertTrue(
            urlsCaptured.any { it.contains("/api/get?") && !it.contains("album_name") },
            "expected an album-dropped /get with no album_name param, saw $urlsCaptured",
        )
    }

    @Test
    fun `strips a spam url suffix and finds lyrics on the cleaned title`() = runTest {
        // The vk.com case: raw title with a scene-rip suffix misses; the
        // spam-stripped "Fire Rides" variant hits via /get.
        val hit = """
            {"id":9,"trackName":"Fire Rides","artistName":"MØ","duration":218,
             "instrumental":false,"syncedLyrics":"[00:01.00]Fire Rides"}
        """.trimIndent()
        val src = source { url ->
            when {
                url.contains("xclusives") || url.contains("vk.com") || url.contains("vk%2Ecom") ->
                    StubResponse(404, "")
                url.contains("Fire+Rides") && url.contains("/api/get") -> StubResponse(200, hit)
                else -> StubResponse(200, "[]")
            }
        }
        val result = src.resolve(
            track(
                title = "Fire Rides vk.com/xclusives_zone",
                artist = "MØ",
                album = "No Mythologies To Follow (Deluxe Edition)",
                durationMs = 218_000L,
            ),
        )
        assertEquals("[00:01.00]Fire Rides", result)
        assertTrue(
            urlsCaptured.any { it.contains("Fire+Rides") && !it.contains("xclusives") },
            "expected a spam-stripped lookup, saw $urlsCaptured",
        )
    }

    @Test
    fun `float duration in LRCLIB response decodes`() = runTest {
        // Real LRCLIB ships duration as a JSON number with decimals (295.0).
        // A regression to `duration: Int` in the data class makes the whole
        // row fail to parse — this is the live-API contract regression test.
        val floaty = """
            {"id":42,"trackName":"Fix You","artistName":"Coldplay","albumName":"X&Y",
             "duration":295.0,"instrumental":false,"plainLyrics":"p","syncedLyrics":"[00:01.00]s"}
        """.trimIndent()
        val src = source { _ -> StubResponse(200, floaty) }
        assertEquals("[00:01.00]s", src.resolve(track()))
    }

    @Test
    fun `search surfaces candidates deduped, instrumental-filtered, closest duration first`() = runTest {
        // id 2 is closest (Δ1s), id 1 is far off (Δ105s) but must still appear
        // — the picker shows options the auto-matcher would reject. id 3 is
        // instrumental (dropped), and id 2 is duplicated across the response.
        val rows = """
            [
              {"id":1,"trackName":"Fire Rides","artistName":"MØ","albumName":"NMTF","duration":400,"syncedLyrics":"[00:01.00]a"},
              {"id":2,"trackName":"Fire Rides","artistName":"MØ","albumName":"NMTF","duration":294,"syncedLyrics":"[00:01.00]b"},
              {"id":3,"trackName":"Fire Rides","artistName":"MØ","duration":295,"instrumental":true,"syncedLyrics":null,"plainLyrics":null},
              {"id":2,"trackName":"Fire Rides","artistName":"MØ","duration":294,"syncedLyrics":"[00:01.00]b"}
            ]
        """.trimIndent()
        val src = source { url ->
            if (url.contains("/api/search")) StubResponse(200, rows) else StubResponse(404, "")
        }
        val result = src.search(
            track(title = "Fire Rides", artist = "MØ", album = "NMTF", durationMs = 295_000L),
        )
        assertEquals(listOf(2L, 1L), result.map { it.id }, "closest duration first, deduped, no instrumental")
        assertTrue(result.first().synced)
        assertEquals("Fire Rides", result.first().trackName)
    }

    @Test
    fun `search returns empty on missing artist or title`() = runTest {
        val src = source { _ -> StubResponse(200, "[]") }
        assertTrue(src.search(track(title = "", artist = "A")).isEmpty())
        assertTrue(urlsCaptured.isEmpty(), "no HTTP for an unusable query")
    }

    @Test
    fun `transient error does not poison the negative cache`() = runTest {
        // During the first resolve every variant misses, and the very first
        // /get throws a transient 500 — that non-clean pass must NOT record a
        // negative-cache entry, so the retry can still succeed.
        var firstResolveDone = false
        var thrown = false
        val src = source { url ->
            if (!firstResolveDone) {
                when {
                    url.contains("/api/get") && !thrown -> {
                        thrown = true
                        StubResponse(500, "")
                    }
                    url.contains("/api/get") -> StubResponse(404, "")
                    else -> StubResponse(200, "[]")
                }
            } else {
                if (url.contains("/api/get")) StubResponse(200, syncedHit) else StubResponse(200, "[]")
            }
        }
        assertNull(src.resolve(track()))
        // Flip to the "service recovered" world; the transient 500 above must
        // not have blacklisted the track.
        firstResolveDone = true
        assertEquals("[00:01.00]Lights", src.resolve(track()))
    }
}

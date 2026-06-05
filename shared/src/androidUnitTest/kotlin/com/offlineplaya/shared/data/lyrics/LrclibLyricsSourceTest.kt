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
    fun `transient error does not poison the negative cache`() = runTest {
        var attempt = 0
        val src = source { url ->
            attempt++
            if (attempt == 1) StubResponse(500, "")
            else if (url.contains("/api/get")) StubResponse(200, syncedHit)
            else StubResponse(200, "[]")
        }
        // First attempt hits a 500 from /get → search returns []. Both
        // "successful" parses required for the miss to be recorded.
        // attempt 1: /get -> 500 (transient throw). Source records nothing.
        assertNull(src.resolve(track()))
        // attempt 2: /get -> 200 with hit. No cache poisoning blocks it.
        assertEquals("[00:01.00]Lights", src.resolve(track()))
    }
}

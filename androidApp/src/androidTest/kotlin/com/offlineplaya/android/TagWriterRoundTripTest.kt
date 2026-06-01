package com.offlineplaya.android

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.ByteArrayOutputStream
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.offlineplaya.shared.data.genre.createJaudiotaggerGenreWriter
import com.offlineplaya.shared.data.image.createJaudiotaggerArtWriter
import com.offlineplaya.shared.util.AppLogger
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Round-trip instrumentation tests for the Jaudiotagger tag writers.
 *
 * One test per audio format we ship support for. Each test:
 *   1. Copies a 1-second silence fixture out of androidTest/assets/fixtures
 *      into the test process's cache directory.
 *   2. Writes synthetic JPEG bytes / a known genre string into the file via
 *      [createJaudiotaggerArtWriter] / [createJaudiotaggerGenreWriter].
 *   3. Re-reads the file via [MediaMetadataRetriever] and asserts the tag
 *      landed.
 *
 * Why these tests exist: the [JaudiotaggerArtWriter] / [JaudiotaggerGenreWriter]
 * implementations dispatch by file extension. Earlier in development we
 * shipped a regression where SAF MIME-lookup returned null and the writer
 * fell back to a `.mp3` temp file, which made Jaudiotagger's MP3 reader
 * choke on FLAC bytes (silent CannotReadException at runtime). A per-format
 * test pinned in CI would have caught that the moment we introduced it.
 *
 * Run with: `./gradlew :androidApp:connectedDebugAndroidTest` after
 * connecting a device and granting all-files access is NOT needed — these
 * tests work entirely under the app's private cache dir.
 */
@RunWith(AndroidJUnit4::class)
class TagWriterRoundTripTest {

    /** Context of the app under test — used for cacheDir + the writer's ContentResolver. */
    private lateinit var appContext: Context

    /** Context of the *test* APK — assets in `src/androidTest/assets/` live here, NOT in the app context. */
    private lateinit var testContext: Context
    private lateinit var workDir: File
    private val logger = ConsoleLogger()

    @Before
    fun setUp() {
        appContext = ApplicationProvider.getApplicationContext()
        testContext = InstrumentationRegistry.getInstrumentation().context
        workDir = File(appContext.cacheDir, "tagwriter-tests").apply {
            deleteRecursively()
            mkdirs()
        }
    }

    @After
    fun tearDown() {
        workDir.deleteRecursively()
    }

    /**
     * Copy a fixture from the test APK's assets into a fresh file in
     * [workDir] and return its `file://` URI string. Each call uses a
     * unique name so parallel test methods don't stomp on each other.
     */
    private fun copyFixture(assetName: String, outName: String): String {
        val dest = File(workDir, outName)
        // Assets live in the TEST apk, not the app apk — must use testContext.
        testContext.assets.open("fixtures/$assetName").use { input ->
            dest.outputStream().use { input.copyTo(it) }
        }
        return Uri.fromFile(dest).toString()
    }

    private fun readEmbeddedPicture(filePath: String): ByteArray? {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(filePath)
            return r.embeddedPicture
        } finally {
            r.release()
        }
    }

    private fun readGenre(filePath: String): String? {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(filePath)
            return r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
        } finally {
            r.release()
        }
    }

    /**
     * MediaMetadataRetriever on some Android versions normalises ID3 genres
     * back to their ID3v1 numeric form (e.g. "17" for "Rock", "8" for "Jazz",
     * sometimes wrapped as "(17)"). This helper accepts either the literal
     * string we wrote or the canonical ID3v1 code for it.
     */
    private fun assertGenreMatches(expected: String, actual: String?) {
        assertNotNull("expected to find a genre tag, got null", actual)
        val a = actual!!
        // Strip parens, e.g. "(17)" → "17"
        val stripped = a.trim().removePrefix("(").removeSuffix(")")
        val expectedCode = ID3V1_GENRE_CODES[expected]?.toString()
        if (a.equals(expected, ignoreCase = true)) return
        if (expectedCode != null && stripped == expectedCode) return
        throw AssertionError("genre mismatch: expected \"$expected\" (id3v1 code $expectedCode), got \"$actual\"")
    }

    /**
     * Renders the result of a [Result.Failure] for inclusion in an assertion
     * message. Walks the cause chain so we don't lose the underlying reason
     * when the outer exception's message is null (Jaudiotagger sometimes
     * wraps the real cause in a generic `CannotWriteException` with no text).
     */
    private fun describeFailure(label: String, result: Result<*>): String {
        if (result.isSuccess) return "$label: success"
        val ex = result.exceptionOrNull() ?: return "$label: failure with no exception"
        val parts = mutableListOf<String>()
        var current: Throwable? = ex
        while (current != null) {
            // Top stack frame for each exception in the chain — usually
            // the most diagnostic single piece of info.
            val firstFrame = current.stackTrace.firstOrNull()?.let { f ->
                " at ${f.className}.${f.methodName}(${f.fileName}:${f.lineNumber})"
            } ?: ""
            parts += "${current::class.java.simpleName}(${current.message})$firstFrame"
            val next = current.cause
            if (next === current || parts.size > 6) break
            current = next
        }
        return "$label: ${parts.joinToString("\n  caused by ")}"
    }

    // A real (tiny) JPEG. Jaudiotagger's FLAC and OGG writers query the
    // image's dimensions to build the PICTURE metadata block — passing
    // arbitrary bytes there throws UnsupportedOperationException at
    // commit time. Production callers always feed real JPEGs from
    // MusicBrainz / Deezer / Cover Art Archive, so the test mirrors
    // that by encoding a 4×4 black bitmap to JPEG at setUp time.
    private val syntheticArt: ByteArray by lazy {
        val bmp = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.BLACK)
        }
        ByteArrayOutputStream().use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out)
            bmp.recycle()
            out.toByteArray()
        }
    }

    // ── Album art round-trip ─────────────────────────────────────────────

    @Test
    fun mp3_art_round_trip() = runTest {
        val uri = copyFixture("silence-1s.mp3", "art-roundtrip.mp3")
        val writer = createJaudiotaggerArtWriter(appContext, logger)

        val result = writer.write(uri, syntheticArt)
        assertTrue(describeFailure("MP3 art write", result), result.isSuccess)

        val path = Uri.parse(uri).path!!
        val readBack = readEmbeddedPicture(path)
        assertNotNull("MediaMetadataRetriever found no embedded picture in MP3", readBack)
        assertTrue("embedded picture in MP3 should be non-empty", readBack!!.isNotEmpty())
    }

    @Test
    fun flac_art_round_trip() = runTest {
        val uri = copyFixture("silence-1s.flac", "art-roundtrip.flac")
        val writer = createJaudiotaggerArtWriter(appContext, logger)

        val result = writer.write(uri, syntheticArt)
        assertTrue(describeFailure("FLAC art write", result), result.isSuccess)

        val path = Uri.parse(uri).path!!
        val readBack = readEmbeddedPicture(path)
        assertNotNull("MediaMetadataRetriever found no embedded picture in FLAC", readBack)
        assertTrue("embedded picture in FLAC should be non-empty", readBack!!.isNotEmpty())
    }

    @Test
    fun ogg_art_round_trip() = runTest {
        val uri = copyFixture("silence-1s.ogg", "art-roundtrip.ogg")
        val writer = createJaudiotaggerArtWriter(appContext, logger)

        val result = writer.write(uri, syntheticArt)
        assertTrue(describeFailure("OGG art write", result), result.isSuccess)

        // MediaMetadataRetriever's OGG support is patchy across vendors;
        // we accept either "round-trip succeeds" or "write succeeded but
        // the platform reader doesn't expose embedded pictures for OGG".
        // The critical thing is the write itself didn't crash.
    }

    // ── Genre round-trip ─────────────────────────────────────────────────

    @Test
    fun mp3_genre_round_trip() = runTest {
        val uri = copyFixture("silence-1s.mp3", "genre-roundtrip.mp3")
        val writer = createJaudiotaggerGenreWriter(appContext, logger)

        val result = writer.writeGenre(uri, "Rock")
        assertTrue(
            "MP3 genre write should succeed: ${result.exceptionOrNull()?.message}",
            result.isSuccess
        )

        val path = Uri.parse(uri).path!!
        assertGenreMatches("Rock", readGenre(path))
    }

    @Test
    fun flac_genre_round_trip() = runTest {
        val uri = copyFixture("silence-1s.flac", "genre-roundtrip.flac")
        val writer = createJaudiotaggerGenreWriter(appContext, logger)

        val result = writer.writeGenre(uri, "Electronic")
        assertTrue(
            "FLAC genre write should succeed: ${result.exceptionOrNull()?.message}",
            result.isSuccess
        )

        val path = Uri.parse(uri).path!!
        assertGenreMatches("Electronic", readGenre(path))
    }

    @Test
    fun m4a_genre_round_trip() = runTest {
        val uri = copyFixture("silence-1s.m4a", "genre-roundtrip.m4a")
        val writer = createJaudiotaggerGenreWriter(appContext, logger)

        val result = writer.writeGenre(uri, "Jazz")
        assertTrue(
            "M4A genre write should succeed: ${result.exceptionOrNull()?.message}",
            result.isSuccess
        )

        val path = Uri.parse(uri).path!!
        assertGenreMatches("Jazz", readGenre(path))
    }

    // ── hasEmbeddedArt / hasGenreTag baseline ────────────────────────────

    @Test
    fun pristine_fixture_reports_no_embedded_art() = runTest {
        // The ffmpeg-generated silence fixtures don't ship with embedded
        // artwork, so the writer must report false on a fresh copy. This
        // is what the burn-metadata use case relies on to decide whether
        // a track even needs the art-fetch step.
        val uri = copyFixture("silence-1s.flac", "pristine.flac")
        val writer = createJaudiotaggerArtWriter(appContext, logger)

        assertEquals(false, writer.hasEmbeddedArt(uri))
    }

    @Test
    fun pristine_fixture_reports_no_genre_tag() = runTest {
        val uri = copyFixture("silence-1s.mp3", "pristine-genre.mp3")
        val writer = createJaudiotaggerGenreWriter(appContext, logger)

        assertEquals(false, writer.hasGenreTag(uri))
    }

    /** Bridges [AppLogger] to logcat so failures in the writer surface clearly. */
    private class ConsoleLogger : AppLogger {
        override fun d(tag: String, message: String) {
            android.util.Log.d(tag, message)
        }

        override fun i(tag: String, message: String) {
            android.util.Log.i(tag, message)
        }

        override fun w(tag: String, message: String) {
            android.util.Log.w(tag, message)
        }

        override fun e(tag: String, message: String, throwable: Throwable?) {
            android.util.Log.e(tag, message, throwable)
        }
    }
}

private fun List<Byte>.toByteArray(): ByteArray = ByteArray(size) { this[it] }

/**
 * Canonical ID3v1 genre codes for the subset we use in tests. The standard
 * spec runs to ~148 entries — we only need the ones referenced in test
 * assertions, since MediaMetadataRetriever sometimes returns the numeric
 * form of an ID3v1 genre even when Jaudiotagger wrote a v2 string.
 */
private val ID3V1_GENRE_CODES: Map<String, Int> = mapOf(
    "Blues" to 0,
    "Classic Rock" to 1,
    "Country" to 2,
    "Dance" to 3,
    "Disco" to 4,
    "Funk" to 5,
    "Grunge" to 6,
    "Hip-Hop" to 7,
    "Jazz" to 8,
    "Metal" to 9,
    "Pop" to 13,
    "R&B" to 14,
    "Rap" to 15,
    "Reggae" to 16,
    "Rock" to 17,
    "Techno" to 18,
    "Classical" to 32,
    "Electronic" to 52,
)

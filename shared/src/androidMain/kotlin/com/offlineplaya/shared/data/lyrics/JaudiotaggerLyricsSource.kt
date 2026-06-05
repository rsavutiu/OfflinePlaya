package com.offlineplaya.shared.data.lyrics

import android.content.Context
import android.net.Uri
import com.offlineplaya.shared.data.util.resolveAudioExtension
import com.offlineplaya.shared.domain.lyrics.EmbeddedLyricsSource
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.util.AppLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.TagOptionSingleton
import java.io.File

/**
 * Reads embedded lyrics via jaudiotagger. Like [com.offlineplaya.shared.data.image.JaudiotaggerArtWriter],
 * jaudiotagger needs a real [File], not a content URI, so we copy the SAF
 * document into a cache temp file, read [FieldKey.LYRICS] (which jaudiotagger
 * maps to ID3 `USLT`, Vorbis `LYRICS`, and MP4 `©lyr` across formats), then
 * delete the temp.
 *
 * Lyrics tagged this way are virtually always **unsynced** plain text — the
 * synced `SYLT` frame is rare and poorly supported, so we don't attempt it.
 * The returned string is fed through `LrcParser` upstream, which yields
 * [com.offlineplaya.shared.domain.lyrics.Lyrics.Plain] for plain text (and
 * still upgrades to synced if a user happened to store LRC text in the field).
 *
 * On-demand for one track only — never run library-wide (the temp copy is
 * per-file I/O).
 */
internal class JaudiotaggerLyricsSource(
    private val context: Context,
    private val logger: AppLogger,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : EmbeddedLyricsSource {

    init {
        // Match the art writer: keep jaudiotagger off the java.awt code paths.
        TagOptionSingleton.getInstance().isAndroid = true
    }

    override suspend fun read(track: Track): String? = withContext(ioDispatcher) {
        val uri = Uri.parse(track.documentUri)
        val resolver = context.contentResolver
        var tempFile: File? = null
        try {
            val extension = resolveAudioExtension(context, uri)
            val tmp = File.createTempFile("lyrics-read-", ".$extension", context.cacheDir)
            tempFile = tmp

            resolver.openInputStream(uri)?.use { input ->
                tmp.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext null

            val audioFile = AudioFileIO.read(tmp)
            val tag = audioFile.tag ?: return@withContext null
            val lyrics = runCatching { tag.getFirst(FieldKey.LYRICS) }.getOrNull()
            lyrics?.takeIf { it.isNotBlank() }?.also {
                logger.d(TAG, "Embedded lyrics found for ${track.documentUri} (${it.length} chars)")
            }
        } catch (t: Throwable) {
            logger.w(TAG, "Embedded lyrics read failed for ${track.documentUri}: ${t.message}")
            null
        } finally {
            tempFile?.let { if (it.exists()) it.delete() }
        }
    }

    private companion object {
        const val TAG = "JaudiotaggerLyrics"
    }
}

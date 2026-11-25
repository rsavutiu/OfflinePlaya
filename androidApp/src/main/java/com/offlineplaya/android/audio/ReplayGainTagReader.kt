package com.offlineplaya.android.audio

import androidx.media3.common.Metadata
import androidx.media3.common.Tracks
import androidx.media3.extractor.metadata.id3.InternalFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import com.offlineplaya.shared.domain.model.ReplayGainTags

/**
 * Pulls ReplayGain tags out of the metadata ExoPlayer already parsed for the
 * playing file — no second pass over the file:
 *  - ID3v2 (MP3): `TXXX` frames, description = tag name
 *  - Vorbis comments (FLAC / Ogg / Opus): key = value
 *  - MP4 / M4A: iTunes `----` freeform atoms ([InternalFrame])
 */
internal object ReplayGainTagReader {

    fun read(tracks: Tracks): ReplayGainTags {
        val pairs = mutableListOf<Pair<String, String>>()
        for (group in tracks.groups) {
            if (group.type != androidx.media3.common.C.TRACK_TYPE_AUDIO) continue
            for (i in 0 until group.length) {
                collect(group.getTrackFormat(i).metadata, pairs)
            }
        }
        return ReplayGainTags.fromTags(pairs)
    }

    private fun collect(metadata: Metadata?, out: MutableList<Pair<String, String>>) {
        if (metadata == null) return
        for (i in 0 until metadata.length()) {
            when (val entry = metadata[i]) {
                is TextInformationFrame -> {
                    val name = entry.description
                    val value = entry.values.firstOrNull()
                    if (entry.id == "TXXX" && name != null && value != null) out += name to value
                }
                is VorbisComment -> out += entry.key to entry.value
                is InternalFrame -> out += entry.description to entry.text
                else -> Unit
            }
        }
    }
}

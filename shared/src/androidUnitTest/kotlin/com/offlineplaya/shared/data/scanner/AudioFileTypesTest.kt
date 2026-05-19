package com.offlineplaya.shared.data.scanner

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AudioFileTypesTest {

    @Test
    fun `audio mime prefix is always accepted regardless of extension`() {
        assertTrue(AudioFileTypes.isAudioFile("anything.bin", "audio/mpeg"))
        assertTrue(AudioFileTypes.isAudioFile("no-extension", "audio/flac"))
        assertTrue(AudioFileTypes.isAudioFile("song.weird", "audio/x-vorbis+ogg"))
    }

    @Test
    fun `known non-audio mime is rejected even with audio extension`() {
        assertFalse(AudioFileTypes.isAudioFile("song.mp3", "image/jpeg"))
        assertFalse(AudioFileTypes.isAudioFile("song.flac", "video/mp4"))
        assertFalse(AudioFileTypes.isAudioFile("song.mp3", "application/pdf"))
        assertFalse(AudioFileTypes.isAudioFile("song.mp3", "text/plain"))
    }

    @Test
    fun `null mime falls through to extension check`() {
        assertTrue(AudioFileTypes.isAudioFile("song.mp3", null))
        assertTrue(AudioFileTypes.isAudioFile("SONG.FLAC", null), "extension lookup is case-insensitive")
        assertTrue(AudioFileTypes.isAudioFile("track.m4a", null))
        assertTrue(AudioFileTypes.isAudioFile("audiobook.m4b", null))
        assertFalse(AudioFileTypes.isAudioFile("cover.jpg", null))
        assertFalse(AudioFileTypes.isAudioFile("README", null), "no extension at all is not audio")
    }

    @Test
    fun `octet-stream mime falls through to extension check`() {
        // SAF providers commonly report octet-stream for unknown types.
        assertTrue(AudioFileTypes.isAudioFile("song.flac", "application/octet-stream"))
        assertFalse(AudioFileTypes.isAudioFile("cover.jpg", "application/octet-stream"))
    }

    @Test
    fun `unknown extension with no mime hint is rejected`() {
        assertFalse(AudioFileTypes.isAudioFile("data.bin", null))
        assertFalse(AudioFileTypes.isAudioFile("playlist.m3u", null))
        assertFalse(AudioFileTypes.isAudioFile("art.jpeg", null))
    }
}

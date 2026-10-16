package com.offlineplaya.android.audio

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The decoded-PCM encoding → wire [com.offlineplaya.stream.StreamFormat] mapping.
 * Guards the format negotiation that opens the receiver's output line, and that
 * unsupported encodings (float, unsigned 8-bit) are rejected rather than streamed
 * as garbage.
 */
class PcmEncodingTest {

    @Test
    fun maps16BitLittleEndian() {
        val f = pcmEncodingToStreamFormat(44100, 2, C.ENCODING_PCM_16BIT)!!
        assertEquals(44100, f.sampleRate)
        assertEquals(2, f.channels)
        assertEquals(16, f.bitsPerSample)
        assertEquals(false, f.bigEndian)
    }

    @Test
    fun maps16BitBigEndian() {
        val f = pcmEncodingToStreamFormat(48000, 2, C.ENCODING_PCM_16BIT_BIG_ENDIAN)!!
        assertEquals(16, f.bitsPerSample)
        assertEquals(true, f.bigEndian)
    }

    @Test
    fun maps24And32Bit() {
        assertEquals(24, pcmEncodingToStreamFormat(96000, 2, C.ENCODING_PCM_24BIT)!!.bitsPerSample)
        assertEquals(32, pcmEncodingToStreamFormat(96000, 2, C.ENCODING_PCM_32BIT)!!.bitsPerSample)
    }

    @Test
    fun rejectsFloatAndUnsigned8Bit() {
        assertNull(pcmEncodingToStreamFormat(44100, 2, C.ENCODING_PCM_FLOAT))
        assertNull(pcmEncodingToStreamFormat(44100, 2, C.ENCODING_PCM_8BIT))
    }

    @Test
    fun rejectsDegenerateGeometry() {
        assertNull(pcmEncodingToStreamFormat(0, 2, C.ENCODING_PCM_16BIT))
        assertNull(pcmEncodingToStreamFormat(44100, 0, C.ENCODING_PCM_16BIT))
    }
}

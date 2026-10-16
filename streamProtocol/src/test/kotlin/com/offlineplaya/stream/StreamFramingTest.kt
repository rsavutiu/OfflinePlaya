package com.offlineplaya.stream

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StreamFramingTest {

    private fun roundTrip(frames: List<StreamFraming.Frame>): List<StreamFraming.Frame> {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            frames.forEach { StreamFraming.writeFrame(out, it.type, it.payload) }
        }
        val input = DataInputStream(ByteArrayInputStream(bytes.toByteArray()))
        return frames.indices.map { StreamFraming.readFrame(input) }
    }

    @Test
    fun audioAndControlFramesRoundTrip() {
        val audio = StreamFraming.Frame(StreamProtocol.TYPE_AUDIO, byteArrayOf(1, 2, 3, 4, 5))
        val control = StreamFraming.Frame(
            StreamProtocol.TYPE_CONTROL,
            StreamProtocol.encodeControl(StreamControl.Hello("PC", StreamProtocol.VERSION)),
        )
        val out = roundTrip(listOf(control, audio))
        assertEquals(control, out[0])
        assertEquals(audio, out[1])
    }

    @Test
    fun controlEncodeDecodeRoundTripsEveryVariant() {
        val controls = listOf(
            StreamControl.Hello("Phone"),
            StreamControl.Format(StreamFormat(44100, 2, 16)),
            StreamControl.Volume(70),
            StreamControl.Flush,
            StreamControl.Heartbeat,
            StreamControl.Bye,
        )
        controls.forEach { c ->
            assertEquals(c, StreamProtocol.decodeControl(StreamProtocol.encodeControl(c)))
        }
    }

    @Test
    fun formatCarriesSampleGeometry() {
        val f = StreamFormat(48000, 2, 24)
        assertEquals(6, f.frameBytes)
        assertEquals(288000, f.bytesPerSecond)
    }

    @Test
    fun rejectsOversizeLengthWord() {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeByte(StreamProtocol.TYPE_AUDIO.toInt())
            out.writeInt(StreamFraming.MAX_FRAME_BYTES + 1)
        }
        val input = DataInputStream(ByteArrayInputStream(bytes.toByteArray()))
        assertFailsWith<IOException> { StreamFraming.readFrame(input) }
    }

    @Test
    fun writeRejectsOversizeFrame() {
        val out = DataOutputStream(ByteArrayOutputStream())
        assertFailsWith<IllegalArgumentException> {
            StreamFraming.writeFrame(out, StreamProtocol.TYPE_AUDIO, ByteArray(0), StreamFraming.MAX_FRAME_BYTES + 1)
        }
    }

    @Test
    fun frameEqualityComparesPayloadContents() {
        val a = StreamFraming.Frame(1, byteArrayOf(1, 2))
        val b = StreamFraming.Frame(1, byteArrayOf(1, 2))
        assertTrue(a == b && a.hashCode() == b.hashCode())
    }
}

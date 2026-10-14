package com.offlineplaya.shared.domain.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DacOutputTest {

    private val dac = OutputDevice(id = 7, name = "FiiO K11", key = "usb:FiiO K11")

    // --- PcmEncoding.canCarryLosslessly -----------------------------------

    @Test
    fun `integer source fits in equal or wider integer container`() {
        assertTrue(PcmEncoding.PCM_16.canCarryLosslessly(PcmEncoding.PCM_16))
        assertTrue(PcmEncoding.PCM_24.canCarryLosslessly(PcmEncoding.PCM_16))
        assertTrue(PcmEncoding.PCM_32.canCarryLosslessly(PcmEncoding.PCM_24))
    }

    @Test
    fun `narrower container or int-float mixing is rejected`() {
        assertFalse(PcmEncoding.PCM_16.canCarryLosslessly(PcmEncoding.PCM_24))
        assertFalse(PcmEncoding.PCM_FLOAT.canCarryLosslessly(PcmEncoding.PCM_24))
        assertFalse(PcmEncoding.PCM_32.canCarryLosslessly(PcmEncoding.PCM_FLOAT))
    }

    // --- BitPerfectFormatSelector -----------------------------------------

    @Test
    fun `exact format wins over a wider container`() {
        val offered = listOf(
            PcmFormat(44_100, PcmEncoding.PCM_32),
            PcmFormat(44_100, PcmEncoding.PCM_16),
            PcmFormat(44_100, PcmEncoding.PCM_24),
        )
        val picked = BitPerfectFormatSelector.pick(offered, PcmFormat(44_100, PcmEncoding.PCM_16))
        assertEquals(PcmFormat(44_100, PcmEncoding.PCM_16), picked)
    }

    @Test
    fun `16-bit source goes into the smallest lossless container when exact is missing`() {
        val offered = listOf(
            PcmFormat(44_100, PcmEncoding.PCM_32),
            PcmFormat(44_100, PcmEncoding.PCM_24),
        )
        val picked = BitPerfectFormatSelector.pick(offered, PcmFormat(44_100, PcmEncoding.PCM_16))
        assertEquals(PcmFormat(44_100, PcmEncoding.PCM_24), picked)
    }

    @Test
    fun `no resampling - a different sample rate is never picked`() {
        val offered = listOf(PcmFormat(48_000, PcmEncoding.PCM_24))
        assertNull(BitPerfectFormatSelector.pick(offered, PcmFormat(44_100, PcmEncoding.PCM_24)))
    }

    @Test
    fun `channel count must match`() {
        val offered = listOf(PcmFormat(96_000, PcmEncoding.PCM_24, channelCount = 1))
        assertNull(BitPerfectFormatSelector.pick(offered, PcmFormat(96_000, PcmEncoding.PCM_24)))
    }

    @Test
    fun `24-bit source is not truncated into a 16-bit container`() {
        val offered = listOf(PcmFormat(96_000, PcmEncoding.PCM_16))
        assertNull(BitPerfectFormatSelector.pick(offered, PcmFormat(96_000, PcmEncoding.PCM_24)))
    }

    // --- signal-path label ------------------------------------------------

    @Test
    fun `no device means no badge`() {
        assertNull(DacOutputState().signalPathLabel())
    }

    @Test
    fun `bit-perfect badge shows rate and depth`() {
        val state = DacOutputState(
            device = dac,
            mode = OutputMode.BitPerfect(PcmFormat(96_000, PcmEncoding.PCM_24)),
        )
        assertEquals("FiiO K11 · 96 kHz / 24-bit · Bit-perfect", state.signalPathLabel())
        assertFalse(state.softwareDspAllowed)
    }

    @Test
    fun `fractional rates and float depth are formatted`() {
        val state = DacOutputState(
            device = dac,
            mode = OutputMode.BitPerfect(PcmFormat(44_100, PcmEncoding.PCM_FLOAT)),
        )
        assertEquals("FiiO K11 · 44.1 kHz / 32-bit float · Bit-perfect", state.signalPathLabel())
    }

    @Test
    fun `standard mode badge and dsp allowed`() {
        val state = DacOutputState(device = dac)
        assertEquals("FiiO K11 · System mixer", state.signalPathLabel())
        assertTrue(state.softwareDspAllowed)
    }

    @Test
    fun `khz formatting`() {
        assertEquals("44.1", formatKhz(44_100))
        assertEquals("88.2", formatKhz(88_200))
        assertEquals("352.8", formatKhz(352_800))
        assertEquals("192", formatKhz(192_000))
    }
}

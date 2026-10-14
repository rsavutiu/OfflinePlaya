package com.offlineplaya.shared.domain.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DacOutputTest {

    private val cdDac = listOf(
        PcmFormat(44_100, PcmEncoding.PCM_16),
        PcmFormat(48_000, PcmEncoding.PCM_16),
        PcmFormat(96_000, PcmEncoding.PCM_16),
        PcmFormat(44_100, PcmEncoding.PCM_24),
    )

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

    // --- BitPerfectPlanner ------------------------------------------------

    @Test
    fun `16-bit track at an offered rate is bit-perfect in the sink encoding`() {
        val plan = BitPerfectPlanner.plan(
            offered = cdDac,
            source = PcmFormat(44_100, PcmEncoding.PCM_16),
            sinkEncoding = PcmEncoding.PCM_16,
        )
        assertEquals(BitPerfectPlan.Use(PcmFormat(44_100, PcmEncoding.PCM_16)), plan)
    }

    @Test
    fun `track deeper than the sink can never be bit-perfect`() {
        val plan = BitPerfectPlanner.plan(
            offered = cdDac,
            source = PcmFormat(44_100, PcmEncoding.PCM_24),
            sinkEncoding = PcmEncoding.PCM_16,
        )
        assertEquals(BitPerfectPlan.Fallback(FallbackReason.HI_RES_SOURCE), plan)
    }

    @Test
    fun `no resampling - a rate the DAC does not offer falls back`() {
        val plan = BitPerfectPlanner.plan(
            offered = cdDac,
            source = PcmFormat(88_200, PcmEncoding.PCM_16),
            sinkEncoding = PcmEncoding.PCM_16,
        )
        assertEquals(BitPerfectPlan.Fallback(FallbackReason.FORMAT_NOT_SUPPORTED), plan)
    }

    @Test
    fun `sink encoding must be offered exactly - a wider mixer is not used`() {
        val plan = BitPerfectPlanner.plan(
            offered = listOf(PcmFormat(44_100, PcmEncoding.PCM_24)),
            source = PcmFormat(44_100, PcmEncoding.PCM_16),
            sinkEncoding = PcmEncoding.PCM_16,
        )
        assertEquals(BitPerfectPlan.Fallback(FallbackReason.FORMAT_NOT_SUPPORTED), plan)
    }

    @Test
    fun `channel count must match`() {
        val plan = BitPerfectPlanner.plan(
            offered = cdDac,
            source = PcmFormat(44_100, PcmEncoding.PCM_16, channelCount = 1),
            sinkEncoding = PcmEncoding.PCM_16,
        )
        assertEquals(BitPerfectPlan.Fallback(FallbackReason.FORMAT_NOT_SUPPORTED), plan)
    }

    @Test
    fun `nothing offered means the device does not support it`() {
        val plan = BitPerfectPlanner.plan(
            offered = emptyList(),
            source = PcmFormat(44_100, PcmEncoding.PCM_16),
            sinkEncoding = PcmEncoding.PCM_16,
        )
        assertEquals(BitPerfectPlan.Fallback(FallbackReason.DEVICE_NOT_SUPPORTED), plan)
    }

    // --- state + formatting -----------------------------------------------

    @Test
    fun `software dsp is only blocked while bit-perfect`() {
        assertTrue(DacOutputState().softwareDspAllowed)
        assertFalse(
            DacOutputState(mode = OutputMode.BitPerfect(PcmFormat(44_100, PcmEncoding.PCM_16)))
                .softwareDspAllowed,
        )
    }

    @Test
    fun `khz formatting`() {
        assertEquals("44.1", formatKhz(44_100))
        assertEquals("88.2", formatKhz(88_200))
        assertEquals("352.8", formatKhz(352_800))
        assertEquals("192", formatKhz(192_000))
    }
}

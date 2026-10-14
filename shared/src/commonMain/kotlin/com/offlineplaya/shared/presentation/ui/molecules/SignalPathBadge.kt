package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.offlineplaya.shared.domain.audio.DacOutputState
import com.offlineplaya.shared.domain.audio.OutputDevice
import com.offlineplaya.shared.domain.audio.OutputMode
import com.offlineplaya.shared.domain.audio.PcmEncoding
import com.offlineplaya.shared.domain.audio.PcmFormat
import com.offlineplaya.shared.domain.audio.formatKhz
import com.offlineplaya.shared.presentation.ui.TestTags
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.AppSpacing
import com.offlineplaya.shared.presentation.ui.theme.LocalBrandAccent
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.dac_badge_bit_perfect
import offlineplaya.shared.generated.resources.dac_badge_system_mixer
import org.jetbrains.compose.resources.stringResource

/**
 * Signal-path pill for Now Playing, e.g. "FiiO K11 · 96 kHz · 16-bit ·
 * Bit-perfect". Shown only while a USB DAC is connected. Bit-perfect wears the
 * brand accent; the system-mixer path is a quiet neutral pill. Left-aligned to
 * sit under [NowPlayingHeadline].
 */
@Composable
fun SignalPathBadge(
    state: DacOutputState,
    modifier: Modifier = Modifier,
) {
    val device = state.device ?: return
    val mode = state.mode
    val bitPerfect = mode is OutputMode.BitPerfect
    val text = when (mode) {
        is OutputMode.BitPerfect -> stringResource(
            Res.string.dac_badge_bit_perfect,
            device.name,
            formatKhz(mode.format.sampleRate),
            mode.format.encoding.bitDepth,
        )
        OutputMode.Standard -> stringResource(Res.string.dac_badge_system_mixer, device.name)
    }
    val brand = LocalBrandAccent.current
    Box(modifier = modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(percent = 50),
            color = if (bitPerfect) brand.accent else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (bitPerfect) brand.onAccent else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(TestTags.NowPlaying.SIGNAL_PATH),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
            )
        }
    }
}

private val PreviewDac = OutputDevice(id = 7, name = "FiiO K11", key = "usb:FiiO K11")

@PreviewScreenSizes
@Composable
private fun SignalPathBadgeBitPerfectPreview() {
    PreviewTheme {
        Surface {
            SignalPathBadge(
                state = DacOutputState(
                    device = PreviewDac,
                    mode = OutputMode.BitPerfect(PcmFormat(44_100, PcmEncoding.PCM_16)),
                    bitPerfectCapable = true,
                ),
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun SignalPathBadgeSystemMixerPreview() {
    PreviewTheme(darkTheme = true) {
        Surface {
            SignalPathBadge(
                state = DacOutputState(device = PreviewDac, bitPerfectCapable = true),
            )
        }
    }
}

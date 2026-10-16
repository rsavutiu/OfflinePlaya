package com.offlineplaya.android.audio

import android.content.Context
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.TeeAudioProcessor

/**
 * A [DefaultRenderersFactory] whose audio sink always has [tap] installed as
 * the first audio processor, so decoded PCM can be captured for "Play to PC".
 *
 * The tee is a pass-through that outputs its input unchanged, so local playback
 * is unaffected when the tap has no listener. `setAudioProcessors([tee])` keeps
 * the sink's default silence-skipping and Sonic (speed/pitch) processors — the
 * chain appends them after ours — so the tap sees PCM *before* speed adjustment
 * (identical at 1.0×, the only case v1 streams).
 *
 * Installing the tap unconditionally matters: the sink is fixed at player
 * construction, so there is no way to add a processor later when the user turns
 * the mode on.
 */
class StreamingRenderersFactory(
    context: Context,
    private val tap: PcmTap,
) : DefaultRenderersFactory(context) {

    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioOutputPlaybackParams: Boolean,
    ): AudioSink =
        DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioOutputPlaybackParameters(enableAudioOutputPlaybackParams)
            .setAudioProcessors(arrayOf<AudioProcessor>(TeeAudioProcessor(tap)))
            .build()
}

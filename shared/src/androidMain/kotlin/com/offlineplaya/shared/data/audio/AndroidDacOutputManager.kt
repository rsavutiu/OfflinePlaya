package com.offlineplaya.shared.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioMixerAttributes
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import com.offlineplaya.shared.domain.audio.BitPerfectFormatSelector
import com.offlineplaya.shared.domain.audio.DacOutputManager
import com.offlineplaya.shared.domain.audio.DacOutputState
import com.offlineplaya.shared.domain.audio.DacPreferences
import com.offlineplaya.shared.domain.audio.FallbackReason
import com.offlineplaya.shared.domain.audio.OutputDevice
import com.offlineplaya.shared.domain.audio.OutputMode
import com.offlineplaya.shared.domain.audio.PcmEncoding
import com.offlineplaya.shared.domain.audio.PcmFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Android implementation of [DacOutputManager].
 *
 * - Detects USB DACs via [AudioDeviceCallback].
 * - On Android 14+ requests a bit-perfect mixer with
 *   [AudioManager.setPreferredMixerAttributes] at the track's native rate.
 * - Watches for the platform revoking it and falls back to [OutputMode.Standard].
 *
 * Everything API-34-specific lives in [Api34] so older devices never load
 * `AudioMixerAttributes`.
 */
class AndroidDacOutputManager(
    private val context: Context,
    private val prefs: DacPreferences,
) : DacOutputManager {

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Must match the AudioAttributes PlaybackService gives ExoPlayer (MUSIC / MEDIA). */
    private val mediaAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private val _state = MutableStateFlow(DacOutputState())
    override val state: StateFlow<DacOutputState> = _state.asStateFlow()

    private val api34: Api34? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) Api34() else null

    private var dac: AudioDeviceInfo? = null
    private var lastSource: PcmFormat? = null
    private var started = false

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = refreshDac()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = refreshDac()
    }

    override fun start() {
        if (started) return
        started = true
        audioManager.registerAudioDeviceCallback(deviceCallback, mainHandler)
        api34?.register()
        refreshDac()
    }

    override fun stop() {
        if (!started) return
        started = false
        audioManager.unregisterAudioDeviceCallback(deviceCallback)
        dac?.let { api34?.clear(it) }
        api34?.unregister()
        dac = null
        lastSource = null
        _state.value = DacOutputState()
    }

    override fun setBitPerfectEnabled(enabled: Boolean) {
        val device = _state.value.device ?: return
        prefs.setBitPerfectEnabled(device.key, enabled)
        _state.update { it.copy(bitPerfectEnabled = enabled) }
        lastSource?.let { prepareFor(it) } // engine observes state and re-prepares the sink
    }

    override fun prepareFor(source: PcmFormat): OutputMode {
        lastSource = source
        val decision = decide(source)
        _state.update { it.copy(mode = decision.mode, fallbackReason = decision.reason) }
        return decision.mode
    }

    // ---------------------------------------------------------------------

    private data class Decision(val mode: OutputMode, val reason: FallbackReason?)

    private fun decide(source: PcmFormat): Decision {
        val device = dac ?: return Decision(OutputMode.Standard, null)
        val api = api34 ?: return Decision(OutputMode.Standard, FallbackReason.API_TOO_OLD)
        if (!prefs.isBitPerfectEnabled(device.key())) {
            api.clear(device)
            return Decision(OutputMode.Standard, FallbackReason.DISABLED_BY_USER)
        }
        return api.apply(device, source)
    }

    private fun refreshDac() {
        val usb = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .firstOrNull { it.type in USB_OUTPUT_TYPES }
        if (usb?.id == dac?.id) return

        dac?.let { old -> api34?.clear(old) }
        dac = usb

        if (usb == null) {
            _state.value = DacOutputState()
            return
        }

        val key = usb.key()
        val capable = api34?.isCapable(usb) ?: false
        _state.value = DacOutputState(
            device = OutputDevice(id = usb.id, name = usb.productName.toString(), key = key),
            bitPerfectCapable = capable,
            bitPerfectEnabled = prefs.isBitPerfectEnabled(key),
            fallbackReason = when {
                api34 == null -> FallbackReason.API_TOO_OLD
                !capable -> FallbackReason.DEVICE_NOT_SUPPORTED
                else -> null
            },
        )
        lastSource?.let { prepareFor(it) } // re-apply for the track already playing
    }

    private fun AudioDeviceInfo.key(): String = "usb:$productName"

    // ---------------------------------------------------------------------
    // API 34+ only
    // ---------------------------------------------------------------------

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private inner class Api34 {

        private val listener =
            AudioManager.OnPreferredMixerAttributesChangedListener { _, device, mixer ->
                if (device.id != dac?.id) return@OnPreferredMixerAttributesChangedListener
                val stillBitPerfect =
                    mixer?.mixerBehavior == AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT
                if (!stillBitPerfect && _state.value.mode is OutputMode.BitPerfect) {
                    _state.update {
                        it.copy(
                            mode = OutputMode.Standard,
                            fallbackReason = FallbackReason.SYSTEM_REJECTED,
                        )
                    }
                }
            }

        fun register() =
            audioManager.addOnPreferredMixerAttributesChangedListener(context.mainExecutor, listener)

        fun unregister() =
            audioManager.removeOnPreferredMixerAttributesChangedListener(listener)

        fun isCapable(device: AudioDeviceInfo): Boolean = bitPerfectOffers(device).isNotEmpty()

        fun clear(device: AudioDeviceInfo) {
            runCatching { audioManager.clearPreferredMixerAttributes(mediaAttributes, device) }
        }

        fun apply(device: AudioDeviceInfo, source: PcmFormat): Decision {
            val offers = bitPerfectOffers(device)
            if (offers.isEmpty()) {
                return Decision(OutputMode.Standard, FallbackReason.DEVICE_NOT_SUPPORTED)
            }

            val chosen = BitPerfectFormatSelector.pick(offers.keys.toList(), source)
            val mixer = chosen?.let { offers[it] }
            if (chosen == null || mixer == null) {
                clear(device)
                return Decision(OutputMode.Standard, FallbackReason.FORMAT_NOT_SUPPORTED)
            }

            val ok = runCatching {
                audioManager.setPreferredMixerAttributes(mediaAttributes, device, mixer)
            }.getOrDefault(false)

            return if (ok) {
                Decision(OutputMode.BitPerfect(chosen), null)
            } else {
                Decision(OutputMode.Standard, FallbackReason.SYSTEM_REJECTED)
            }
        }

        /** Bit-perfect mixer configurations the HAL offers, keyed by their domain format. */
        private fun bitPerfectOffers(device: AudioDeviceInfo): Map<PcmFormat, AudioMixerAttributes> =
            runCatching { audioManager.getSupportedMixerAttributes(device) }
                .getOrDefault(emptyList())
                .filter { it.mixerBehavior == AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT }
                .mapNotNull { mixer ->
                    val encoding = mixer.format.encoding.toPcmEncoding() ?: return@mapNotNull null
                    PcmFormat(
                        sampleRate = mixer.format.sampleRate,
                        encoding = encoding,
                        channelCount = mixer.format.channelCount,
                    ) to mixer
                }
                .toMap()
    }

    private companion object {
        val USB_OUTPUT_TYPES = setOf(
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_HEADSET,
        )

        fun Int.toPcmEncoding(): PcmEncoding? = when (this) {
            AudioFormat.ENCODING_PCM_16BIT -> PcmEncoding.PCM_16
            AudioFormat.ENCODING_PCM_24BIT_PACKED -> PcmEncoding.PCM_24
            AudioFormat.ENCODING_PCM_32BIT -> PcmEncoding.PCM_32
            AudioFormat.ENCODING_PCM_FLOAT -> PcmEncoding.PCM_FLOAT
            else -> null
        }
    }
}

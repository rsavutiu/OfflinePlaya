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
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import com.offlineplaya.shared.domain.audio.BitPerfectPlan
import com.offlineplaya.shared.domain.audio.BitPerfectPlanner
import com.offlineplaya.shared.domain.audio.DacOutputManager
import com.offlineplaya.shared.domain.audio.DacOutputState
import com.offlineplaya.shared.domain.audio.DacPreferences
import com.offlineplaya.shared.domain.audio.FallbackReason
import com.offlineplaya.shared.domain.audio.OutputDevice
import com.offlineplaya.shared.domain.audio.OutputMode
import com.offlineplaya.shared.domain.audio.PcmEncoding
import com.offlineplaya.shared.domain.audio.PcmFormat
import com.offlineplaya.shared.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Android implementation of [DacOutputManager].
 *
 * - Detects USB DACs via [AudioDeviceCallback].
 * - On Android 14+ requests a bit-perfect mixer with
 *   [AudioManager.setPreferredMixerAttributes] in exactly the format the player
 *   writes ([sinkEncoding] at the track's rate and channel count).
 * - Watches for the platform revoking it and falls back to [OutputMode.Standard].
 *
 * Everything API-34-specific lives in [Api34] so older devices never load
 * `AudioMixerAttributes`. All methods are main-thread only: the device callback
 * is delivered on the main looper and the player calls in from its application
 * thread, which is the main looper in PlaybackService.
 *
 * @param sinkEncoding what the player writes to its AudioTrack. Media3's
 *   DefaultAudioSink without float output writes 16-bit PCM for every source.
 */
class AndroidDacOutputManager(
    private val context: Context,
    private val prefs: DacPreferences,
    private val logger: AppLogger,
    private val sinkEncoding: PcmEncoding = PcmEncoding.PCM_16,
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
    private var startCount = 0

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = refreshDac()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = refreshDac()
    }

    @MainThread
    override fun start() {
        startCount++
        if (startCount > 1) return
        audioManager.registerAudioDeviceCallback(deviceCallback, mainHandler)
        api34?.register()
        refreshDac()
    }

    @MainThread
    override fun stop() {
        if (startCount == 0) return
        startCount--
        if (startCount > 0) return
        audioManager.unregisterAudioDeviceCallback(deviceCallback)
        releasePlayback()
        api34?.unregister()
        dac = null
        _state.value = DacOutputState()
    }

    @MainThread
    override fun releasePlayback() {
        lastSource = null
        dac?.let { api34?.clear(it) }
        _state.update {
            it.copy(
                mode = OutputMode.Standard,
                fallbackReason = it.idleFallbackReason(),
            )
        }
    }

    @MainThread
    override fun setBitPerfectEnabled(enabled: Boolean) {
        val device = _state.value.device ?: return
        prefs.setBitPerfectEnabled(device.key, enabled)
        _state.update { it.copy(bitPerfectEnabled = enabled) }
        val source = lastSource
        if (source != null) {
            prepareFor(source) // the player observes state and re-routes
        } else if (!enabled) {
            dac?.let { api34?.clear(it) }
        }
    }

    @MainThread
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

    /** Fallback reason to show while nothing is playing. */
    private fun DacOutputState.idleFallbackReason(): FallbackReason? = when {
        device == null -> null
        api34 == null -> FallbackReason.API_TOO_OLD
        !bitPerfectCapable -> FallbackReason.DEVICE_NOT_SUPPORTED
        else -> null
    }

    private fun refreshDac() {
        val usb = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .firstOrNull { it.type in USB_OUTPUT_TYPES }
        if (usb?.id == dac?.id) return

        dac?.let { old -> api34?.clear(old) }
        dac = usb

        if (usb == null) {
            logger.i(TAG, "USB DAC disconnected")
            _state.value = DacOutputState()
            return
        }

        val key = usb.key()
        val capable = api34?.isCapable(usb) ?: false
        logger.i(TAG, "USB DAC connected: ${usb.productName} (bit-perfect capable: $capable)")
        _state.value = DacOutputState(
            device = OutputDevice(id = usb.id, name = usb.productName.toString(), key = key),
            bitPerfectCapable = capable,
            bitPerfectEnabled = prefs.isBitPerfectEnabled(key),
        ).let { it.copy(fallbackReason = it.idleFallbackReason()) }
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
                    logger.w(TAG, "Bit-perfect mixer was revoked by the system")
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
            return when (val plan = BitPerfectPlanner.plan(offers.keys, source, sinkEncoding)) {
                is BitPerfectPlan.Fallback -> {
                    clear(device)
                    Decision(OutputMode.Standard, plan.reason)
                }
                is BitPerfectPlan.Use -> {
                    val mixer = offers.getValue(plan.format)
                    val ok = runCatching {
                        audioManager.setPreferredMixerAttributes(mediaAttributes, device, mixer)
                    }.onFailure {
                        logger.w(TAG, "setPreferredMixerAttributes threw: ${it.message}")
                    }.getOrDefault(false)
                    if (ok) {
                        Decision(OutputMode.BitPerfect(plan.format), null)
                    } else {
                        Decision(OutputMode.Standard, FallbackReason.SYSTEM_REJECTED)
                    }
                }
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
        const val TAG = "DacOutput"

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

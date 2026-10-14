package com.offlineplaya.shared.data.audio

import android.content.Context
import android.content.SharedPreferences
import com.offlineplaya.shared.domain.audio.DacPreferences

/**
 * Per-DAC settings in a small SharedPreferences file. Not part of
 * `SettingsRepository` because reads happen synchronously on the playback
 * path at every track boundary. Bit-perfect defaults to ON for each new DAC.
 */
class SharedPrefsDacPreferences(context: Context) : DacPreferences {

    private val sp: SharedPreferences =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override fun isBitPerfectEnabled(deviceKey: String): Boolean =
        sp.getBoolean(KEY_PREFIX + deviceKey, true)

    override fun setBitPerfectEnabled(deviceKey: String, enabled: Boolean) {
        sp.edit().putBoolean(KEY_PREFIX + deviceKey, enabled).apply()
    }

    private companion object {
        const val FILE_NAME = "dac_output"
        const val KEY_PREFIX = "bit_perfect_"
    }
}

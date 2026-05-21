package com.offlineplaya.shared.util

import android.util.Log
import com.offlineplaya.shared.BuildConfig

actual val isDebugMode: Boolean = BuildConfig.DEBUG

actual fun createPlatformLogger(isDebug: Boolean): AppLogger = AndroidLogger(isDebug)

private class AndroidLogger(private val isDebug: Boolean) : AppLogger {
    override fun d(tag: String, message: String) {
        if (isDebug) Log.d(tag, message)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        if (isDebug) Log.e(tag, message, throwable)
    }

    override fun i(tag: String, message: String) {
        if (isDebug) Log.i(tag, message)
    }

    override fun w(tag: String, message: String) {
        if (isDebug) Log.w(tag, message)
    }
}

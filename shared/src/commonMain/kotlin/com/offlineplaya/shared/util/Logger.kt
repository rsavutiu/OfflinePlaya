package com.offlineplaya.shared.util

interface AppLogger {
    fun d(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String)
}

expect val isDebugMode: Boolean

fun createLogger(): AppLogger = createPlatformLogger(isDebugMode)

expect fun createPlatformLogger(isDebug: Boolean): AppLogger

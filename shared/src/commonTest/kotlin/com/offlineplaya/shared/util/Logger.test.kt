package com.offlineplaya.shared.util

actual val isDebugMode: Boolean = true

actual fun createPlatformLogger(isDebug: Boolean): AppLogger = TestLogger()

private class TestLogger : AppLogger {
    override fun d(tag: String, message: String) {
        println("DEBUG: [$tag] $message")
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        println("ERROR: [$tag] $message")
        throwable?.printStackTrace()
    }

    override fun i(tag: String, message: String) {
        println("INFO: [$tag] $message")
    }

    override fun w(tag: String, message: String) {
        println("WARN: [$tag] $message")
    }
}

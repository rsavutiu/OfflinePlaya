package com.offlineplaya.shared.util

/**
 * Stdout-printing [AppLogger] for unit tests. Construct directly where a repo
 * or use case wants a logger — no platform plumbing required.
 */
internal class TestLogger : AppLogger {
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

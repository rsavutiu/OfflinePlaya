package com.offlineplaya.shared.util

/**
 * Hour-of-day (0–23) in the user's local timezone. expect/actual because
 * commonMain has no platform clock — Android uses `java.time.LocalTime`.
 */
expect fun currentHourOfDay(): Int

/**
 * Wall-clock time in epoch milliseconds. expect/actual so commonMain callers
 * (play-history timestamps, recent-album use, smart-playlist cutoffs) don't
 * reach for the JVM-only `System.currentTimeMillis()` — that would break the
 * moment a non-JVM target (iOS/desktop) is added.
 */
expect fun currentTimeMillis(): Long

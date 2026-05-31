package com.offlineplaya.shared.util

/**
 * Hour-of-day (0–23) in the user's local timezone. expect/actual because
 * commonMain has no platform clock — Android uses `java.time.LocalTime`.
 */
expect fun currentHourOfDay(): Int

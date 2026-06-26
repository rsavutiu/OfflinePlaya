package com.offlineplaya.shared.util

import java.time.LocalTime

actual fun currentHourOfDay(): Int = LocalTime.now().hour

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

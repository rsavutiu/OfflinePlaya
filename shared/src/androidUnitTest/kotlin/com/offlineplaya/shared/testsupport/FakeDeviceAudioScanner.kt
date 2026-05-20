package com.offlineplaya.shared.testsupport

import com.offlineplaya.shared.domain.scanner.DeviceAudioScanner
import com.offlineplaya.shared.domain.scanner.DeviceAudioTrack

/**
 * Test fake for [DeviceAudioScanner]. Returns whatever [DeviceAudioTrack] list
 * the test gives it — defaults to empty, which is also the behaviour the real
 * MediaStore scanner produces when permission isn't granted.
 */
internal class FakeDeviceAudioScanner(
    private val tracks: List<DeviceAudioTrack> = emptyList(),
) : DeviceAudioScanner {
    override suspend fun scan(): List<DeviceAudioTrack> = tracks
}

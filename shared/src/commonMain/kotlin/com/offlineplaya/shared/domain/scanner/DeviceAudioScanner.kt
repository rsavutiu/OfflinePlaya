package com.offlineplaya.shared.domain.scanner

/**
 * Enumerates audio files the platform already indexes for us — MediaStore
 * on Android, the system music index on iOS/macOS, an `xdg-user-dirs` walk
 * on Linux.
 *
 * This is the *seamless* counterpart to [FolderScanner]:
 *  - [FolderScanner] needs an explicit user-granted SAF tree URI, which can
 *    reach anywhere they pick — but Android 11+ refuses to let users pick
 *    sensitive roots like `Download/`, the internal-storage root, or
 *    `Android/data`. So a user with music in their Downloads folder is
 *    stuck unless we surface it some other way.
 *  - [DeviceAudioScanner] needs no picker — it asks the OS for "every audio
 *    file you know about" and gets back the union of everything the user
 *    has downloaded, ripped, or copied to shared storage, including the
 *    folders SAF won't let us tree-pick.
 *
 * The platform decides what permission to gate it on. On Android that's
 * `READ_MEDIA_AUDIO` (API 33+) / `READ_EXTERNAL_STORAGE` (older); if the
 * user hasn't granted it, implementations MUST return an empty list rather
 * than throwing — the sync pass should be a no-op, not a failure.
 *
 * The returned tracks carry full metadata inline ([DeviceAudioTrack.metadata])
 * because the platform index already extracted it; the sync pipeline doesn't
 * need to round-trip through [MetadataReader] for them.
 */
interface DeviceAudioScanner {

    /**
     * Snapshot the device's audio index. Returns an empty list when the
     * required permission isn't granted, or when the platform has no such
     * index (e.g. a Desktop target that hasn't shipped an implementation yet).
     */
    suspend fun scan(): List<DeviceAudioTrack>

    companion object {
        /**
         * Sentinel `tree_uri` used for tracks discovered via [DeviceAudioScanner].
         * Distinct from any real SAF `content://com.android.externalstorage/...`
         * URI so the rest of the codebase can tell the two sources apart
         * without a separate column.
         */
        const val ROOT_URI: String = "mediastore://audio"

        /** Human-readable display name for the synthesized root folder. */
        const val ROOT_DISPLAY_NAME: String = "Device audio"
    }
}

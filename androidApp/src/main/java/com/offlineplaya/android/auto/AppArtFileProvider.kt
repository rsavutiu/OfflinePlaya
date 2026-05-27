package com.offlineplaya.android.auto

import androidx.core.content.FileProvider

/**
 * Concrete [FileProvider] subclass declared in the manifest with authority
 * `<applicationId>${com.offlineplaya.shared.data.image.TrackArtCache.AUTHORITY_SUFFIX}`.
 *
 * The subclass exists purely to claim our own authority namespace —
 * multiple libraries each declaring the default `FileProvider` under one
 * applicationId would collide. All actual logic (cache key derivation,
 * URI construction, existence probing) lives in
 * [com.offlineplaya.shared.data.image.TrackArtCache] so the FileProvider
 * code stays in one place and consumers don't import this class.
 */
class AppArtFileProvider : FileProvider()

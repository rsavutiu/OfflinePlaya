package com.offlineplaya.shared.presentation.auto

/**
 * Platform-neutral representation of a single row in the Android Auto
 * browse tree. The androidx.media3 `MediaItem` is built from this on the
 * platform side; keeping the model here lets the routing logic stay in
 * `commonMain` and be unit-tested.
 *
 * @property mediaId stable id from [MediaIdRouter]. Used as both the row
 *   key and the route key when the user taps it.
 * @property title the primary text shown on the row.
 * @property subtitle secondary text (artist name on album rows, count of
 *   albums on artist rows, etc.). Null for top-level nodes.
 * @property isBrowsable true → tapping drills into a child list; false →
 *   tapping plays. Mutually exclusive with [isPlayable].
 * @property isPlayable true → tapping triggers `onAddMediaItems`. Top-level
 *   nodes and category folders are NOT playable.
 * @property trackId backing track row id when [isPlayable] is true and the
 *   entry represents a real track. Browse-tree projection consumers don't
 *   need this — the parsed media id carries it — but it's convenient to
 *   surface for logging.
 * @property artworkUri opaque platform-specific URI string for the row's
 *   thumbnail (album cover, etc.). Null when no art is available — the
 *   head unit renders a generic placeholder in that case. Stored as
 *   `String` so the commonMain projection stays platform-neutral; the
 *   androidApp side wraps it into a `Uri` before handing to media3.
 */
data class BrowseEntry(
    val mediaId: String,
    val title: String,
    val subtitle: String? = null,
    val isBrowsable: Boolean = false,
    val isPlayable: Boolean = false,
    val trackId: Long? = null,
    val artworkUri: String? = null,
) {
    init {
        require(isBrowsable xor isPlayable) {
            "BrowseEntry must be exactly one of browsable or playable: $mediaId"
        }
    }
}

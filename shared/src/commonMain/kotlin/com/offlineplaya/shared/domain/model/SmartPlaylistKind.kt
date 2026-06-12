package com.offlineplaya.shared.domain.model

/**
 * Built-in, always-up-to-date playlists derived from listening history and
 * library state — no user curation involved. Rendered above the user's own
 * playlists on the Playlists page.
 */
enum class SmartPlaylistKind {
    RECENTLY_PLAYED,
    MOST_PLAYED,
    RECENTLY_ADDED,
    FORGOTTEN_FAVORITES,
    NEVER_PLAYED,
}

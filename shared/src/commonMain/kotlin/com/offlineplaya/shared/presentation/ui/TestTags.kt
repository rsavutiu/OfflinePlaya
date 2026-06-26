package com.offlineplaya.shared.presentation.ui

/**
 * Stable `Modifier.testTag` identifiers for UI tests.
 *
 * Centralised so tests key off a single source of truth instead of string
 * literals, and so each page exposes a known **root** tag — navigation
 * (push/pop) assertions just check which root is present. Deliberately
 * harness-agnostic: the same tags are used whether screen tests run via
 * `commonTest` `runComposeUiTest` or `androidApp/androidTest`.
 *
 * Tagging is added per page as that page's tests are written (see
 * docs/instrumentation-test-plan.md), not all at once.
 */
object TestTags {
    object Artists {
        /** Page root. */
        const val ROOT = "library_artists_page"
    }

    object Home {
        /** Page root. */
        const val ROOT = "home_page"

        /** Search affordance in the header. */
        const val SEARCH = "home_search"

        /** The three-cell stat strip (tracks / folders / total). */
        const val STAT_STRIP = "home_stat_strip"

        /** Total-duration stat cell — the only route to onOpenStats. */
        const val STAT_TOTAL = "home_stat_total"

        /** Recently-played albums shelf (present only with recent albums). */
        const val RECENT_SHELF = "home_recent_shelf"

        /** Browse-grid cards, one per library facet. */
        const val BROWSE_TRACKS = "home_browse_tracks"
        const val BROWSE_ALBUMS = "home_browse_albums"
        const val BROWSE_ARTISTS = "home_browse_artists"
        const val BROWSE_PLAYLISTS = "home_browse_playlists"
    }

    object Flat {
        /** Page root. */
        const val ROOT = "library_flat_page"
    }

    object Queue {
        /** Page root. */
        const val ROOT = "queue_page"
    }

    object TrackActions {
        /** "Add to queue" action in the long-press track-details sheet. */
        const val ADD_TO_QUEUE = "track_actions_add_to_queue"
    }

    object Settings {
        /** Page root. */
        const val ROOT = "settings_page"

        /** Album-art color on/off switch row (Appearance section). */
        const val ALBUM_ART_COLOR_TOGGLE = "settings_album_art_color_toggle"

        /** Crossfade on/off switch row (Playback section). */
        const val CROSSFADE_TOGGLE = "settings_crossfade_toggle"
    }

    object Playlists {
        /** Page root. */
        const val ROOT = "playlists_page"

        /** Each smart-playlist row (five of them when wired). */
        const val SMART_ROW = "playlists_smart_row"
    }

    object Lyrics {
        /** Page root. */
        const val ROOT = "lyrics_page"

        /** Spinner while lyrics are being resolved. */
        const val LOADING = "lyrics_loading"

        /** Empty-state body — no lyrics found for the track. */
        const val EMPTY = "lyrics_empty"

        /** Plain (un-timed) lyrics block. */
        const val PLAIN = "lyrics_plain"

        /** Synced (timestamped) lyrics list. */
        const val SYNCED = "lyrics_synced"
    }

    object NowPlaying {
        /** Page root. */
        const val ROOT = "now_playing_page"

        /** Empty-state body — nothing loaded into the player. */
        const val EMPTY = "now_playing_empty"

        /** The large play / pause toggle in the transport row. */
        const val PLAY_PAUSE = "now_playing_play_pause"
    }

    object Search {
        /** Page root. */
        const val ROOT = "search_page"

        /** The query text field. */
        const val FIELD = "search_field"

        /** "Type to search" prompt — query shorter than the 2-char minimum. */
        const val PROMPT = "search_prompt"

        /** "No results" state — a valid query that matched nothing. */
        const val NO_RESULTS = "search_no_results"

        /** The results list (present only when there are matches). */
        const val RESULTS = "search_results"
    }
}

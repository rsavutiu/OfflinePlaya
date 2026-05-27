package com.offlineplaya.shared.presentation.auto

/**
 * Encode / decode the stable media-id strings used as the browse-tree
 * primary key in Android Auto. Pure Kotlin so the routing logic can be
 * unit-tested in `commonTest` without pulling in androidx.media3.
 *
 * Format: `kind/id` (e.g. `album/42`) for entities with a backing row id,
 * or just the bare kind constant for the top-level browsable nodes
 * (`root`, `recents`, `albums`, `artists`, `playlists`).
 *
 * Why not opaque ids: Auto round-trips the id through several IPC layers
 * (browse → click → play). Making them human-readable keeps logs grep-able
 * and lets `onAddMediaItems` route by string parsing rather than holding
 * server-side state.
 */
object MediaIdRouter {

    const val ROOT: String = "root"
    const val NODE_RECENTS: String = "recents"
    const val NODE_ALBUMS: String = "albums"
    const val NODE_ARTISTS: String = "artists"
    const val NODE_PLAYLISTS: String = "playlists"

    fun albumId(id: Long): String = "${Kind.ALBUM.prefix}/$id"
    fun artistId(id: Long): String = "${Kind.ARTIST.prefix}/$id"
    fun playlistId(id: Long): String = "${Kind.PLAYLIST.prefix}/$id"

    /**
     * Track ids carry their parent context so playback knows which queue to
     * build (a track played from inside Album X gets the rest of Album X as
     * its queue). Form: `track/<trackId>?from=<albumId|artistId|playlistId>`.
     *
     * `from` is optional — a top-level "play this track" without a parent
     * just queues the single track.
     */
    fun trackId(trackId: Long, parent: ParentContext? = null): String {
        val base = "${Kind.TRACK.prefix}/$trackId"
        return if (parent == null) base else "$base?from=${parent.encode()}"
    }

    /**
     * Parse [mediaId] back into a structured [ParsedId]. Returns null for
     * the top-level nodes (`root`, `albums`, etc.) — callers handle those
     * by direct string match against the constants above.
     */
    fun parse(mediaId: String): ParsedId? {
        if (mediaId == ROOT) return ParsedId.Node(ROOT)
        if (mediaId in TOP_NODES) return ParsedId.Node(mediaId)

        val (head, query) = splitQuery(mediaId)
        val slash = head.indexOf('/')
        if (slash <= 0 || slash == head.lastIndex) return null
        val prefix = head.substring(0, slash)
        val rawId = head.substring(slash + 1)
        val id = rawId.toLongOrNull() ?: return null
        val kind = Kind.fromPrefix(prefix) ?: return null

        val from = query["from"]?.let { ParentContext.decode(it) }
        return ParsedId.Entity(kind = kind, id = id, parent = from)
    }

    /** Parsed media id. Either a static node (no entity behind it) or an entity ref. */
    sealed interface ParsedId {
        data class Node(val name: String) : ParsedId
        data class Entity(val kind: Kind, val id: Long, val parent: ParentContext? = null) : ParsedId
    }

    enum class Kind(val prefix: String) {
        ALBUM("album"),
        ARTIST("artist"),
        PLAYLIST("playlist"),
        TRACK("track");

        companion object {
            private val byPrefix: Map<String, Kind> = entries.associateBy { it.prefix }
            fun fromPrefix(p: String): Kind? = byPrefix[p]
        }
    }

    /**
     * Where a track came from when the user tapped it. Used to build a
     * playback queue with the rest of its siblings rather than just the
     * single track.
     */
    sealed interface ParentContext {
        data class Album(val id: Long) : ParentContext
        data class Artist(val id: Long) : ParentContext
        data class Playlist(val id: Long) : ParentContext

        fun encode(): String = when (this) {
            is Album -> "album:$id"
            is Artist -> "artist:$id"
            is Playlist -> "playlist:$id"
        }

        companion object {
            fun decode(raw: String): ParentContext? {
                val sep = raw.indexOf(':')
                if (sep <= 0 || sep == raw.lastIndex) return null
                val kind = raw.substring(0, sep)
                val id = raw.substring(sep + 1).toLongOrNull() ?: return null
                return when (kind) {
                    "album" -> Album(id)
                    "artist" -> Artist(id)
                    "playlist" -> Playlist(id)
                    else -> null
                }
            }
        }
    }

    private val TOP_NODES = setOf(NODE_RECENTS, NODE_ALBUMS, NODE_ARTISTS, NODE_PLAYLISTS)

    /**
     * Tiny query-string parser — handles a single `?from=X` suffix only.
     * Doing this by hand keeps commonMain free of any url-encoding deps.
     */
    private fun splitQuery(mediaId: String): Pair<String, Map<String, String>> {
        val q = mediaId.indexOf('?')
        if (q < 0) return mediaId to emptyMap()
        val head = mediaId.substring(0, q)
        val tail = mediaId.substring(q + 1)
        val map = tail.split('&').mapNotNull { pair ->
            val eq = pair.indexOf('=')
            if (eq <= 0 || eq == pair.lastIndex) return@mapNotNull null
            pair.substring(0, eq) to pair.substring(eq + 1)
        }.toMap()
        return head to map
    }
}

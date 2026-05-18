package com.offlineplaya.shared.domain.model

enum class LibraryView {
    ARTIST_ALBUM_TRACK,
    FOLDER,
    FLAT;

    companion object {
        val Default = ARTIST_ALBUM_TRACK
    }
}

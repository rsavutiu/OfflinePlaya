package com.offlineplaya.shared.domain.model

data class Track(
    val id: Long,
    val documentUri: String,
    val treeUri: String,
    val relativePath: String,
    val fileName: String,
    val title: String,
    val artistName: String,
    val albumArtistName: String?,
    val albumName: String,
    val genre: String?,
    val year: Int?,
    val trackNumber: Int?,
    val discNumber: Int?,
    val durationMs: Long?,
    val bitrate: Int?,
    val sampleRate: Int?,
    val channels: Int?,
    val codec: String?,
    val artistId: Long?,
    val albumId: Long?,
    val folderId: Long?,
    val scanStatus: ScanStatus,
    /**
     * Bucketed genre derived from [genre] by
     * [com.offlineplaya.shared.domain.usecase.GenreClassifier]. Null on rows
     * that pre-date the EQ feature; the lazy backfill in
     * [com.offlineplaya.shared.domain.usecase.LibrarySyncUseCase] populates
     * those on the next sync pass.
     */
    val canonicalGenre: CanonicalGenre? = null,
)

enum class ScanStatus(val dbValue: String) {
    PENDING("pending"),
    SCANNED("scanned"),
    ERROR("error");

    companion object {
        fun fromDbValue(value: String): ScanStatus =
            entries.firstOrNull { it.dbValue == value } ?: PENDING
    }
}

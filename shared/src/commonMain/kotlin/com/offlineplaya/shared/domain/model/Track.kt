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

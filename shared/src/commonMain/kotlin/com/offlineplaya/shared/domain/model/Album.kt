package com.offlineplaya.shared.domain.model

data class Album(
    val id: Long,
    val name: String,
    val artistId: Long?,
    val year: Int?,
    val trackCount: Int,
    val durationMs: Long,
)

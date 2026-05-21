package com.offlineplaya.shared.domain.model

data class Artist(
    val id: Long,
    val name: String,
    val albumCount: Int,
    val trackCount: Int,
    val imageUrl: String? = null,
)

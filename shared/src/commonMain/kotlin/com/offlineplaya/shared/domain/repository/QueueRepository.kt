package com.offlineplaya.shared.domain.repository

import com.offlineplaya.shared.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface QueueRepository {
    fun observeQueue(): Flow<List<Track>>
    suspend fun count(): Long
    suspend fun enqueue(trackId: Long, source: String = "queue")
    suspend fun clear()
}

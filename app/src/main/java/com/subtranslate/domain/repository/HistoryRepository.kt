package com.subtranslate.domain.repository

import com.subtranslate.domain.model.HistoryItem
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun getAll(): Flow<List<HistoryItem>>
    suspend fun save(item: HistoryItem): Long
    suspend fun delete(id: Long)
    suspend fun deleteAll()
}

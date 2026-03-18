package com.subtranslate.data.repository

import com.subtranslate.data.local.dao.SubtitleHistoryDao
import com.subtranslate.data.local.mapper.toDomain
import com.subtranslate.data.local.mapper.toEntity
import com.subtranslate.domain.model.HistoryItem
import com.subtranslate.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val dao: SubtitleHistoryDao
) : HistoryRepository {

    override fun getAll(): Flow<List<HistoryItem>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun save(item: HistoryItem): Long =
        dao.insert(item.toEntity())

    override suspend fun delete(id: Long) =
        dao.deleteById(id)

    override suspend fun deleteAll() =
        dao.deleteAll()
}

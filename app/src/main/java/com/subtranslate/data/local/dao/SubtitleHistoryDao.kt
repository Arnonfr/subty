package com.subtranslate.data.local.dao

import androidx.room.*
import com.subtranslate.data.local.entity.SubtitleHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtitleHistoryDao {

    @Query("SELECT * FROM subtitle_history ORDER BY downloadedAt DESC")
    fun getAll(): Flow<List<SubtitleHistoryEntity>>

    @Query("SELECT * FROM subtitle_history WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SubtitleHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SubtitleHistoryEntity): Long

    @Delete
    suspend fun delete(entity: SubtitleHistoryEntity)

    @Query("DELETE FROM subtitle_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM subtitle_history")
    suspend fun deleteAll()
}

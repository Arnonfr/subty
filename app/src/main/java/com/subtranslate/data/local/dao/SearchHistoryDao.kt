package com.subtranslate.data.local.dao

import androidx.room.*
import com.subtranslate.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {

    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT 100")
    fun getAll(): Flow<List<SearchHistoryEntity>>

    @Query("SELECT * FROM search_history WHERE query LIKE :prefix || '%' ORDER BY searchedAt DESC LIMIT 5")
    suspend fun searchByPrefix(prefix: String): List<SearchHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SearchHistoryEntity): Long

    @Query("DELETE FROM search_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM search_history")
    suspend fun deleteAll()

    /** Most recent unique-by-title searches (movies + TV) — for the home carousel */
    @Query("""
        SELECT * FROM search_history
        WHERE id IN (
            SELECT MAX(id) FROM search_history
            GROUP BY query
        )
        ORDER BY searchedAt DESC LIMIT 12
    """)
    fun getRecentTitles(): Flow<List<SearchHistoryEntity>>

    /** Most recent unique-by-title TV searches — kept for compatibility */
    @Query("""
        SELECT * FROM search_history
        WHERE id IN (
            SELECT MAX(id) FROM search_history
            WHERE contentType = 'tv'
            GROUP BY query
        )
        ORDER BY searchedAt DESC LIMIT 10
    """)
    fun getRecentTvShows(): Flow<List<SearchHistoryEntity>>
}

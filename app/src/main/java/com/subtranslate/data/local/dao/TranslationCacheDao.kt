package com.subtranslate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.subtranslate.data.local.entity.TranslationCacheEntity

@Dao
interface TranslationCacheDao {
    @Query("SELECT translatedText FROM translation_cache WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun get(cacheKey: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TranslationCacheEntity)
}

package com.subtranslate.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.subtranslate.data.local.dao.TranslationCacheDao
import com.subtranslate.data.local.dao.SearchHistoryDao
import com.subtranslate.data.local.dao.SubtitleHistoryDao
import com.subtranslate.data.local.entity.SearchHistoryEntity
import com.subtranslate.data.local.entity.SubtitleHistoryEntity
import com.subtranslate.data.local.entity.TranslationCacheEntity

@Database(
    entities = [SubtitleHistoryEntity::class, SearchHistoryEntity::class, TranslationCacheEntity::class],
    version = 5,
    exportSchema = false
)
abstract class SubTranslateDatabase : RoomDatabase() {
    abstract fun historyDao(): SubtitleHistoryDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun translationCacheDao(): TranslationCacheDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS search_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        query TEXT NOT NULL,
                        season INTEGER,
                        episode INTEGER,
                        languages TEXT,
                        contentType TEXT,
                        searchedAt INTEGER NOT NULL DEFAULT 0
                    )"""
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE search_history ADD COLUMN posterUrl TEXT")
                db.execSQL("ALTER TABLE search_history ADD COLUMN imdbId TEXT")
                db.execSQL("ALTER TABLE search_history ADD COLUMN seasonsCount INTEGER")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS translation_cache (
                        cacheKey TEXT NOT NULL PRIMARY KEY,
                        translatedText TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )"""
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE search_history ADD COLUMN tmdbId INTEGER")
            }
        }
    }
}

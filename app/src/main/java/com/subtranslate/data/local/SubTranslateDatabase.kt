package com.subtranslate.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.subtranslate.data.local.dao.SearchHistoryDao
import com.subtranslate.data.local.dao.SubtitleHistoryDao
import com.subtranslate.data.local.entity.SearchHistoryEntity
import com.subtranslate.data.local.entity.SubtitleHistoryEntity

@Database(
    entities = [SubtitleHistoryEntity::class, SearchHistoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class SubTranslateDatabase : RoomDatabase() {
    abstract fun historyDao(): SubtitleHistoryDao
    abstract fun searchHistoryDao(): SearchHistoryDao

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
    }
}

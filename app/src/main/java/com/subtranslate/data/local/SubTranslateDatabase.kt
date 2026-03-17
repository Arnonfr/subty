package com.subtranslate.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.subtranslate.data.local.dao.SubtitleHistoryDao
import com.subtranslate.data.local.entity.SubtitleHistoryEntity

@Database(
    entities = [SubtitleHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SubTranslateDatabase : RoomDatabase() {
    abstract fun historyDao(): SubtitleHistoryDao
}

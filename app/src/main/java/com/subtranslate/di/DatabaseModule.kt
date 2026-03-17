package com.subtranslate.di

import android.content.Context
import androidx.room.Room
import com.subtranslate.data.local.SubTranslateDatabase
import com.subtranslate.data.local.dao.SubtitleHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SubTranslateDatabase =
        Room.databaseBuilder(
            context,
            SubTranslateDatabase::class.java,
            "subtranslate.db"
        ).build()

    @Provides
    fun provideHistoryDao(db: SubTranslateDatabase): SubtitleHistoryDao =
        db.historyDao()
}

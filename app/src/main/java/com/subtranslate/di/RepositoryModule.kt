package com.subtranslate.di

import com.subtranslate.data.repository.HistoryRepositoryImpl
import com.subtranslate.data.repository.SubtitleRepositoryImpl
import com.subtranslate.data.repository.TranslationRepositoryImpl
import com.subtranslate.domain.repository.HistoryRepository
import com.subtranslate.domain.repository.SubtitleRepository
import com.subtranslate.domain.repository.TranslationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSubtitleRepository(impl: SubtitleRepositoryImpl): SubtitleRepository

    @Binds
    @Singleton
    abstract fun bindTranslationRepository(impl: TranslationRepositoryImpl): TranslationRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository
}

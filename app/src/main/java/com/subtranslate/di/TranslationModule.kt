package com.subtranslate.di

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.subtranslate.data.analytics.AnalyticsTracker
import com.subtranslate.data.analytics.FirestoreTranslationEventLogger
import com.subtranslate.data.analytics.TranslationEventLogger
import com.subtranslate.data.local.datastore.SettingsDataStore
import com.subtranslate.data.local.datastore.TranslationUsageStore
import com.subtranslate.data.remote.translation.OpenRouterTranslationService
import com.subtranslate.data.remote.translation.TranslationGateway
import com.subtranslate.data.remote.translation.TranslationGatewayRouter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TranslationModule {

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore =
        SettingsDataStore(context)

    @Provides
    @Singleton
    fun provideTranslationUsageStore(settingsDataStore: SettingsDataStore): TranslationUsageStore =
        settingsDataStore

    @Provides
    @Singleton
    fun provideTranslationGateway(
        router: TranslationGatewayRouter,
    ): TranslationGateway = router

    @Provides
    @Singleton
    fun provideTranslationEventLogger(
        analyticsTracker: AnalyticsTracker,
    ): TranslationEventLogger = FirestoreTranslationEventLogger(analyticsTracker)

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}

package com.subtranslate.di

import android.content.Context
import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.subtranslate.data.remote.opensubtitles.TokenProvider
import com.subtranslate.data.remote.translation.AnthropicClientProvider
import com.subtranslate.util.SettingsDataStore
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
    fun provideTokenProvider(settingsDataStore: SettingsDataStore): TokenProvider =
        object : TokenProvider {
            override val apiKey: String
                get() = settingsDataStore.openSubtitlesApiKey
            override val jwtToken: String?
                get() = settingsDataStore.openSubtitlesJwt
        }

    @Provides
    @Singleton
    fun provideAnthropicClientProvider(settingsDataStore: SettingsDataStore): AnthropicClientProvider =
        object : AnthropicClientProvider {
            override fun getClient(): AnthropicClient =
                AnthropicOkHttpClient.builder()
                    .apiKey(settingsDataStore.anthropicApiKey)
                    .build()
        }
}

package com.subtranslate.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.subtranslate.data.remote.opensubtitles.OpenSubtitlesApi
import com.subtranslate.data.remote.opensubtitles.OpenSubtitlesAuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private fun loggingInterceptor() = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    // ── OpenSubtitles ────────────────────────────────────────────────────────

    @Provides
    @Singleton
    @Named("opensubtitles")
    fun provideOsOkHttpClient(authInterceptor: OpenSubtitlesAuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideOpenSubtitlesApi(
        @Named("opensubtitles") client: OkHttpClient,
        moshi: Moshi
    ): OpenSubtitlesApi = Retrofit.Builder()
        .baseUrl("https://api.opensubtitles.com/api/v1/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(OpenSubtitlesApi::class.java)

}

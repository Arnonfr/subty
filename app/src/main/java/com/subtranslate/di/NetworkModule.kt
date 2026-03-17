package com.subtranslate.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.subtranslate.BuildConfig
import com.subtranslate.data.remote.opensubtitles.OpenSubtitlesApi
import com.subtranslate.data.remote.opensubtitles.OpenSubtitlesAuthInterceptor
import com.subtranslate.data.remote.tmdb.TmdbApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
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

    // ── TMDB ─────────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    @Named("tmdb")
    fun provideTmdbOkHttpClient(): OkHttpClient {
        val tmdbKeyInterceptor = Interceptor { chain ->
            val req = chain.request()
            val url = req.url.newBuilder()
                .addQueryParameter("api_key", BuildConfig.TMDB_API_KEY)
                .build()
            chain.proceed(req.newBuilder().url(url).build())
        }
        return OkHttpClient.Builder()
            .addInterceptor(tmdbKeyInterceptor)
            .addInterceptor(loggingInterceptor())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideTmdbApi(
        @Named("tmdb") client: OkHttpClient,
        moshi: Moshi
    ): TmdbApi = Retrofit.Builder()
        .baseUrl("https://api.themoviedb.org/3/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(TmdbApi::class.java)
}

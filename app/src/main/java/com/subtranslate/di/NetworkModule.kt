package com.subtranslate.di

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.subtranslate.data.remote.opensubtitles.OpenSubtitlesApi
import com.subtranslate.data.remote.opensubtitles.OpenSubtitlesAuthInterceptor
import com.subtranslate.data.remote.subdl.SubDLApi
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

    /**
     * Moshi adapter for nullable Int that gracefully handles empty strings and
     * unexpected types (e.g. OpenSubtitles returns `"year": ""` for some entries).
     */
    private val lenientNullableIntAdapter = object : JsonAdapter<Int?>() {
        override fun fromJson(reader: JsonReader): Int? {
            if (reader.peek() == JsonReader.Token.NULL) return reader.nextNull()
            if (reader.peek() == JsonReader.Token.STRING) {
                val s = reader.nextString()
                return s.toIntOrNull()
            }
            return reader.nextInt()
        }
        override fun toJson(writer: JsonWriter, value: Int?) {
            writer.value(value)
        }
    }.nullSafe()

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(Int::class.javaObjectType, lenientNullableIntAdapter)
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

    // ── SubDL ─────────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    @Named("subdl")
    fun provideSubDLOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                // dl.subdl.com requires a Referer header or it returns 403
                val req = chain.request().newBuilder()
                    .header("Referer", "https://subdl.com/")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36")
                    .build()
                chain.proceed(req)
            }
            .addInterceptor(loggingInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideSubDLApi(
        @Named("subdl") client: OkHttpClient,
        moshi: Moshi
    ): SubDLApi = Retrofit.Builder()
        .baseUrl("https://api.subdl.com/api/v1/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(SubDLApi::class.java)

}

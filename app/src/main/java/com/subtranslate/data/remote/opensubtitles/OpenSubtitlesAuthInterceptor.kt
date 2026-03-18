package com.subtranslate.data.remote.opensubtitles

import com.subtranslate.BuildConfig
import com.subtranslate.data.local.datastore.SettingsDataStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenSubtitlesAuthInterceptor @Inject constructor(
    private val session: SessionStore,
    private val settingsDataStore: SettingsDataStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val apiKey = settingsDataStore.openSubtitlesApiKey?.ifBlank { null } ?: BuildConfig.OPENSUBTITLES_API_KEY

        val builder = chain.request().newBuilder()
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            // Disable OkHttp's automatic gzip — Varnish CDN routes gzip-accepting
            // POST requests to a different backend that intermittently returns 503
            .header("Accept-Encoding", "identity")
            .header("User-Agent", "SubtranslateApp/1.5")
            .header("Api-Key", apiKey)

        session.jwtToken?.let { builder.header("Authorization", "Bearer $it") }

        return chain.proceed(builder.build())
    }
}

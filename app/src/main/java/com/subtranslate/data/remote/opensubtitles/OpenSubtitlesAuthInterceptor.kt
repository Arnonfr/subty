package com.subtranslate.data.remote.opensubtitles

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenSubtitlesAuthInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()
            .header("Content-Type", "application/json")
            .header("User-Agent", "SubTranslate v1.0")

        val apiKey = tokenProvider.apiKey
        if (apiKey.isNotEmpty()) {
            builder.header("Api-Key", apiKey)
        }

        val jwt = tokenProvider.jwtToken
        if (!jwt.isNullOrEmpty()) {
            builder.header("Authorization", "Bearer $jwt")
        }

        return chain.proceed(builder.build())
    }
}

interface TokenProvider {
    val apiKey: String
    val jwtToken: String?
}

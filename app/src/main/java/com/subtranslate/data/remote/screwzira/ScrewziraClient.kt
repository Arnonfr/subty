package com.subtranslate.data.remote.screwzira

import android.util.Log
import com.subtranslate.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

private const val TAG = "ScrewziraClient"

@Singleton
class ScrewziraClient @Inject constructor(
    @Named("screwzira") private val httpClient: OkHttpClient,
) {
    private val baseUrl = "https://www.ktuvit.me"
    @Volatile private var cookieHeader: String? = null

    fun isConfigured(): Boolean {
        return BuildConfig.SCREWZIRA_EMAIL.isNotBlank() && BuildConfig.SCREWZIRA_PASSWORD.isNotBlank()
    }

    suspend fun search(
        query: String,
        isSeries: Boolean,
        season: Int? = null,
        episode: Int? = null,
        releaseYear: Int? = null,
    ): ScrewziraSearchResult? {
        if (!isConfigured()) return null
        ensureLoggedIn()

        val searchBody = """
            {"request":{"FilmName":${jsonString(query)},"Actors":[],"Studios":null,"Directors":[],"Genres":[],"Countries":[],"Languages":[],"Rating":[],"Page":1,"WithSubsOnly":false,"SearchType":"${if (isSeries) 1 else 0}","Year":${releaseYear?.toString() ?: "\"\""}}}
        """.trimIndent()

        val searchResponse = execute(
            Request.Builder()
                .url("$baseUrl/Services/ContentProvider.svc/SearchPage_search")
                .post(searchBody.toRequestBody(jsonMimeType))
                .header("Content-Type", "application/json")
                .build()
        )

        val candidates = ScrewziraParser.parseSearchCandidates(searchResponse)
        val best = ScrewziraParser.pickBestCandidate(query, isSeries, releaseYear, candidates) ?: return null
        val subtitlesBody = if (isSeries && season != null && episode != null) {
            execute(
                Request.Builder()
                    .url("$baseUrl/Services/GetModuleAjax.ashx?moduleName=SubtitlesList&SeriesID=${best.id}&Season=$season&Episode=$episode")
                    .header("Referer", "$baseUrl/MovieInfo.aspx?ID=${best.id}")
                    .get()
                    .build()
            )
        } else {
            execute(
                Request.Builder()
                    .url("$baseUrl/MovieInfo.aspx?ID=${best.id}")
                    .get()
                    .build()
            )
        }

        return ScrewziraSearchResult(
            filmId = best.id,
            imdbId = best.imdbId,
            subtitles = ScrewziraParser.parseSubtitleEntries(subtitlesBody),
        )
    }

    suspend fun downloadSubtitle(filmId: String, subtitleId: String, fallbackFileName: String): Pair<String, String> {
        ensureLoggedIn()

        val requestBody = """
            {"request":{"FilmID":"$filmId","SubtitleID":"$subtitleId","FontSize":0,"FontColor":"","PredefinedLayout":-1}}
        """.trimIndent()

        val downloadTokenResponse = execute(
            Request.Builder()
                .url("$baseUrl/Services/ContentProvider.svc/RequestSubtitleDownload")
                .post(requestBody.toRequestBody(jsonMimeType))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Referer", "$baseUrl/MovieInfo.aspx?ID=$filmId")
                .build()
        )

        val downloadIdentifier = ScrewziraParser.parseDownloadIdentifier(downloadTokenResponse)
            ?: error("Screwzira download token missing")

        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$baseUrl/Services/DownloadFile.ashx?DownloadIdentifier=$downloadIdentifier")
                .header("Referer", "$baseUrl/MovieInfo.aspx?ID=$filmId")
                .build()

            httpClient.newCall(withCommonHeaders(request).build()).execute().use { response ->
                if (!response.isSuccessful) error("Screwzira HTTP ${response.code}: ${response.message}")
                val bytes = response.body?.bytes() ?: error("Empty Screwzira response")
                val fileName = response.header("Content-Disposition")
                    ?.substringAfter("filename=", "")
                    ?.trim('"')
                    ?.takeIf { it.isNotBlank() }
                    ?: fallbackFileName.withSrtExtension()
                fileName to decodeSubtitleBytes(bytes)
            }
        }
    }

    private suspend fun ensureLoggedIn() {
        if (cookieHeader != null) return

        val body = """
            {"request":{"Email":${jsonString(BuildConfig.SCREWZIRA_EMAIL)},"Password":${jsonString(BuildConfig.SCREWZIRA_PASSWORD)}}}
        """.trimIndent()

        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$baseUrl/Services/MembershipService.svc/Login")
                .post(body.toRequestBody(jsonMimeType))
                .header("Content-Type", "application/json")
                .build()

            httpClient.newCall(withCommonHeaders(request).build()).execute().use { response ->
                if (!response.isSuccessful) error("Screwzira login failed: HTTP ${response.code}")
                val cookies = response.headers("Set-Cookie")
                    .map { it.substringBefore(';') }
                    .filter { it.isNotBlank() }
                if (cookies.isEmpty()) error("Screwzira login returned no cookies")
                cookieHeader = cookies.joinToString("; ")
                Log.d(TAG, "Screwzira login ok")
            }
        }
    }

    private suspend fun execute(request: Request): String {
        return withContext(Dispatchers.IO) {
            httpClient.newCall(withCommonHeaders(request).build()).execute().use { response ->
                if (!response.isSuccessful) error("Screwzira HTTP ${response.code}: ${response.message}")
                response.body?.string() ?: error("Empty Screwzira response")
            }
        }
    }

    private fun withCommonHeaders(request: Request): Request.Builder {
        val builder = request.newBuilder()
            .header("User-Agent", USER_AGENT)
        cookieHeader?.let { builder.header("Cookie", it) }
        return builder
    }

    private fun jsonString(value: String): String {
        return "\"" + value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"") + "\""
    }

    private fun String.withSrtExtension(): String {
        return if (contains('.')) this else "$this.srt"
    }

    private fun decodeSubtitleBytes(bytes: ByteArray): String {
        val utf8 = bytes.toString(Charsets.UTF_8)
        if ('\uFFFD' !in utf8) return utf8

        val windows1255 = runCatching { bytes.toString(Charset.forName("windows-1255")) }.getOrNull()
        return when {
            windows1255 == null -> utf8
            windows1255.count { it in '\u0590'..'\u05FF' } > utf8.count { it in '\u0590'..'\u05FF' } -> windows1255
            else -> utf8
        }
    }

    private companion object {
        val jsonMimeType = "application/json".toMediaType()
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36"
    }
}

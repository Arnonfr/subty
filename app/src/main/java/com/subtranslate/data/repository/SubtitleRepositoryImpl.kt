package com.subtranslate.data.repository

import android.util.Log
import com.subtranslate.BuildConfig
import com.subtranslate.data.parser.AssParser
import com.subtranslate.data.parser.SrtParser
import com.subtranslate.data.parser.VttParser
import com.subtranslate.data.remote.opensubtitles.OpenSubtitlesApi
import com.subtranslate.data.remote.opensubtitles.SessionStore
import com.subtranslate.data.remote.opensubtitles.dto.DownloadRequest
import com.subtranslate.data.remote.opensubtitles.dto.LoginRequest
import com.subtranslate.data.remote.opensubtitles.mapper.toDomain
import com.subtranslate.data.remote.subdl.SubDLApi
import com.subtranslate.data.remote.subdl.SubDLSubtitleDto
import com.subtranslate.domain.model.SubtitleFile
import com.subtranslate.domain.model.SubtitleFormat
import com.subtranslate.domain.model.SubtitleSearchResult
import com.subtranslate.domain.repository.SubtitleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

private const val TAG = "SubtitleRepo"
private const val SUBDL_DL_BASE = "https://dl.subdl.com"

@Singleton
class SubtitleRepositoryImpl @Inject constructor(
    private val osApi: OpenSubtitlesApi,
    private val subDLApi: SubDLApi,
    @Named("opensubtitles") private val osHttpClient: OkHttpClient,
    @Named("subdl")         private val subDLHttpClient: OkHttpClient,
    private val session: SessionStore,
) : SubtitleRepository {

    // Negative IDs are SubDL results; OpenSubtitles always uses positive ints.
    private val subDLIdGen    = AtomicInteger(-1)
    private val subDLRegistry = mutableMapOf<Int, SubDLSubtitleDto>()

    /** Login with VIP credentials from BuildConfig if not already logged in. */
    private suspend fun ensureLoggedIn() {
        if (session.jwtToken != null) return
        val user = BuildConfig.OPENSUBTITLES_USERNAME
        val pass = BuildConfig.OPENSUBTITLES_PASSWORD
        if (user.isBlank() || pass.isBlank()) return
        try {
            val resp = osApi.login(LoginRequest(username = user, password = pass))
            resp.token?.let {
                session.jwtToken = it
                Log.d(TAG, "OS login ok — VIP=${resp.user?.vip}, quota=${resp.user?.allowedDownloads}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "OS auto-login failed: ${e.message}")
        }
    }

    // ── OpenSubtitles search ──────────────────────────────────────────────────

    override suspend fun search(
        query: String?,
        imdbId: Int?,
        languages: String?,
        movieHash: String?,
        season: Int?,
        episode: Int?,
        page: Int,
    ): List<SubtitleSearchResult> {
        val response = osApi.searchSubtitles(
            query     = query,
            imdbId    = imdbId,
            languages = languages,
            movieHash = movieHash,
            season    = season,
            episode   = episode,
            page      = page,
        )
        return response.data.mapNotNull { it.toDomain() }
    }

    // ── SubDL search (returns SubtitleSearchResult with negative fileIds) ─────

    /**
     * Format an IMDB ID string to SubDL's required format: "tt" + 7-digit zero-padded number.
     * Accepts "386676", "tt386676", "tt0386676", etc.
     */
    private fun formatImdbId(raw: String): String {
        val digits = raw.removePrefix("tt").trimStart('0').ifEmpty { "0" }
        return "tt" + digits.padStart(7, '0')
    }

    suspend fun searchSubDL(
        title: String?,
        imdbId: String?    = null,
        season: Int?       = null,
        episode: Int?      = null,
        languages: String? = null,
        type: String?      = null,
    ): List<SubtitleSearchResult> {
        val apiKey = BuildConfig.SUBDL_API_KEY.ifBlank { return emptyList() }
        // SubDL requires an IMDB ID — title-only queries return nothing
        if (imdbId.isNullOrBlank()) return emptyList()
        val formattedImdbId = formatImdbId(imdbId)
        return try {
            val resp = subDLApi.searchSubtitles(
                apiKey    = apiKey,
                title     = null,          // SubDL ignores title when imdb_id is given
                imdbId    = formattedImdbId,
                season    = season,
                episode   = episode,
                languages = languages,
                type      = type,
            )
            (resp.subtitles ?: emptyList()).map { dto ->
                val id = subDLIdGen.getAndDecrement()
                subDLRegistry[id] = dto
                dto.toSearchResult(id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "SubDL search failed: ${e.message}")
            emptyList()
        }
    }

    // ── Download dispatcher ───────────────────────────────────────────────────

    override suspend fun download(fileId: Int, languageCode: String?): SubtitleFile {
        if (fileId < 0) {
            val dto = subDLRegistry[fileId]
                ?: error("SubDL entry $fileId not found")
            return downloadFromSubDLUrl(dto.url, dto.releaseName ?: dto.name ?: "subtitle", languageCode)
        }

        return try {
            downloadFromOpenSubtitles(fileId, languageCode)
        } catch (osEx: Exception) {
            Log.w(TAG, "OpenSubtitles download failed: ${osEx.message}")
            throw osEx
        }
    }

    // ── OpenSubtitles download ────────────────────────────────────────────────

    private suspend fun downloadFromOpenSubtitles(fileId: Int, languageCode: String?): SubtitleFile {
        ensureLoggedIn()

        // Varnish sometimes returns 503 "Guru Meditation" transiently — retry up to 3×
        var dlResponse = run {
            var lastEx: Exception? = null
            repeat(3) { attempt ->
                try {
                    return@run osApi.requestDownload(DownloadRequest(fileId = fileId))
                } catch (e: retrofit2.HttpException) {
                    if (e.code() == 503) {
                        lastEx = e
                        Log.w(TAG, "OS download 503 (attempt ${attempt + 1}/3) — retrying…")
                        delay(1500L * (attempt + 1))
                    } else throw e
                }
            }
            throw lastEx!!
        }
        Log.d(TAG, "OS download link: ${dlResponse.link}  remaining=${dlResponse.remaining}")

        val content = withContext(Dispatchers.IO) {
            val request = Request.Builder().url(dlResponse.link).build()
            osHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}: ${response.message}")
                response.body?.string() ?: error("Empty subtitle file")
            }
        }
        return parseSubtitle(content, dlResponse.fileName, languageCode)
    }

    // ── SubDL download (ZIP extraction) ──────────────────────────────────────

    private suspend fun downloadFromSubDLUrl(urlPath: String, fileName: String, languageCode: String?): SubtitleFile {
        val fullUrl = if (urlPath.startsWith("http")) urlPath else "$SUBDL_DL_BASE$urlPath"
        Log.d(TAG, "SubDL download: $fullUrl")

        val (entryName, content) = withContext(Dispatchers.IO) {
            val request  = Request.Builder().url(fullUrl).build()
            val zipBytes = subDLHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("SubDL HTTP ${response.code}: ${response.message}")
                response.body?.bytes() ?: error("Empty SubDL response")
            }
            Companion.extractSubtitleFromZip(zipBytes)
                ?: error("No subtitle file found inside SubDL ZIP")
        }

        return parseSubtitle(content, entryName.ifBlank { fileName }, languageCode)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun parseSubtitle(content: String, fileName: String, languageCode: String?): SubtitleFile {
        val extension = fileName.substringAfterLast(".").lowercase()
        val format    = SubtitleFormat.fromExtension(extension) ?: SubtitleFormat.SRT
        val parser    = when (format) {
            SubtitleFormat.SRT, SubtitleFormat.SUB -> SrtParser()
            SubtitleFormat.VTT                     -> VttParser()
            SubtitleFormat.ASS, SubtitleFormat.SSA -> AssParser()
        }
        return parser.parse(content).copy(title = fileName, sourceLanguage = languageCode)
    }

    companion object {
        internal fun extractSubtitleFromZip(zipBytes: ByteArray): Pair<String, String>? {
            ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory &&
                        entry.name.matches(Regex(".*\\.(srt|vtt|ass|ssa|sub)$", RegexOption.IGNORE_CASE))
                    ) {
                        return entry.name to zis.readBytes().toString(Charsets.UTF_8)
                    }
                    entry = zis.nextEntry
                }
            }
            return null
        }
    }

    private fun SubDLSubtitleDto.toSearchResult(id: Int) = SubtitleSearchResult(
        fileId            = id,
        fileName          = releaseName ?: name ?: "subtitle",
        language          = lang ?: "?",
        languageCode      = lang?.lowercase() ?: "?",
        downloadCount     = 0,
        rating            = ratings,
        format            = "srt",
        uploadedAt        = null,
        movieTitle        = name,
        movieYear         = null,
        imdbId            = null,
        isHearingImpaired = hi ?: false,
        isTrusted         = false,
        uploaderName      = author,
    )
}

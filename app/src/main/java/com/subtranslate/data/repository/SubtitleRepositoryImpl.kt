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
import com.subtranslate.data.remote.screwzira.ScrewziraClient
import com.subtranslate.data.remote.screwzira.ScrewziraSubtitleEntry
import com.subtranslate.data.remote.subdl.SubDLApi
import com.subtranslate.data.remote.subdl.SubDLSubtitleDto
import com.subtranslate.domain.model.SubtitleFile
import com.subtranslate.domain.model.SubtitleFormat
import com.subtranslate.domain.model.SubtitleSearchResult
import com.subtranslate.domain.model.SubtitleSource
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
    private val screwziraClient: ScrewziraClient,
    @Named("opensubtitles") private val osHttpClient: OkHttpClient,
    @Named("subdl")         private val subDLHttpClient: OkHttpClient,
    private val session: SessionStore,
) : SubtitleRepository {

    // Negative IDs are reserved for non-OpenSubtitles sources.
    private val externalIdGen = AtomicInteger(-1)
    private val externalRegistry = mutableMapOf<Int, ExternalSubtitleHandle>()

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
        tmdbId: Int?,
        languages: String?,
        movieHash: String?,
        season: Int?,
        episode: Int?,
        page: Int,
    ): List<SubtitleSearchResult> {
        val response = osApi.searchSubtitles(
            query     = query,
            imdbId    = imdbId,
            tmdbId    = tmdbId,
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
        tmdbId: Int?       = null,
        season: Int?       = null,
        episode: Int?      = null,
        languages: String? = null,
        type: String?      = null,
    ): List<SubtitleSearchResult> {
        val apiKey = BuildConfig.SUBDL_API_KEY.ifBlank { return emptyList() }

        val formattedImdbId = imdbId?.let { formatImdbId(it) }

        return try {
            val resp = subDLApi.searchSubtitles(
                apiKey    = apiKey,
                title     = if (formattedImdbId == null && tmdbId == null) title else null,
                imdbId    = formattedImdbId,
                tmdbId    = tmdbId,
                season    = season,
                episode   = episode,
                languages = languages,
                type      = type,
            )
            
            // SubDL can return results either in 'subtitles' (when imdb_id is used)
            // or 'results' (when title is used, which might be movies instead of subtitles)
            // For now, we only handle direct subtitle results.
            (resp.subtitles ?: emptyList())
                .filter { dto -> dto.matchesRequestedEpisode(season, episode) }
                .map { dto ->
                val id = externalIdGen.getAndDecrement()
                externalRegistry[id] = ExternalSubtitleHandle.SubDL(dto)
                dto.toSearchResult(id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "SubDL search failed: ${e.message}")
            emptyList()
        }
    }

    suspend fun searchScrewzira(
        title: String?,
        season: Int? = null,
        episode: Int? = null,
        languages: String? = null,
        type: String? = null,
    ): List<SubtitleSearchResult> {
        val normalizedTitle = title?.trim().orEmpty()
        if (normalizedTitle.isBlank()) return emptyList()
        if (!supportsHebrew(languages)) return emptyList()

        return try {
            val result = screwziraClient.search(
                query = normalizedTitle,
                isSeries = type == "tv",
                season = season,
                episode = episode,
            ) ?: return emptyList()

            result.subtitles.map { entry ->
                val id = externalIdGen.getAndDecrement()
                externalRegistry[id] = ExternalSubtitleHandle.Screwzira(
                    filmId = result.filmId,
                    subtitle = entry,
                )
                entry.toSearchResult(
                    id = id,
                    movieTitle = normalizedTitle,
                    imdbId = result.imdbId,
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Screwzira search failed: ${e.message}")
            emptyList()
        }
    }

    // ── Download dispatcher ───────────────────────────────────────────────────

    override suspend fun download(fileId: Int, languageCode: String?): SubtitleFile {
        if (fileId < 0) {
            return when (val handle = externalRegistry[fileId]
                ?: error("External subtitle entry $fileId not found")) {
                is ExternalSubtitleHandle.SubDL -> {
                    downloadFromSubDLUrl(
                        handle.dto.url,
                        handle.dto.releaseName ?: handle.dto.name ?: "subtitle",
                        languageCode,
                    )
                }
                is ExternalSubtitleHandle.Screwzira -> {
                    downloadFromScrewzira(handle.filmId, handle.subtitle, languageCode)
                }
            }
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

    private suspend fun downloadFromScrewzira(
        filmId: String,
        subtitle: ScrewziraSubtitleEntry,
        languageCode: String?,
    ): SubtitleFile {
        val (fileName, content) = screwziraClient.downloadSubtitle(
            filmId = filmId,
            subtitleId = subtitle.subtitleId,
            fallbackFileName = subtitle.name,
        )
        return parseSubtitle(content, fileName, languageCode ?: "he")
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

    private fun subdlLangToIsoCode(lang: String): String = when (lang.lowercase().trim()) {
        "english"                                        -> "en"
        "arabic"                                         -> "ar"
        "hebrew"                                         -> "he"
        "french"                                         -> "fr"
        "spanish"                                        -> "es"
        "german"                                         -> "de"
        "portuguese"                                     -> "pt"
        "brazillian portuguese", "brazilian portuguese",
        "portuguese (brazil)", "portuguese-br"           -> "pt-br"
        "italian"                                        -> "it"
        "dutch"                                          -> "nl"
        "russian"                                        -> "ru"
        "chinese", "simplified chinese"                  -> "zh"
        "traditional chinese", "big 5 code"              -> "zh-tw"
        "japanese"                                       -> "ja"
        "korean"                                         -> "ko"
        "turkish"                                        -> "tr"
        "polish"                                         -> "pl"
        "czech"                                          -> "cs"
        "hungarian"                                      -> "hu"
        "romanian"                                       -> "ro"
        "bulgarian"                                      -> "bg"
        "greek"                                          -> "el"
        "swedish"                                        -> "sv"
        "norwegian", "norwegian bokmål"                  -> "no"
        "danish"                                         -> "da"
        "finnish"                                        -> "fi"
        "indonesian"                                     -> "id"
        "malay"                                          -> "ms"
        "thai"                                           -> "th"
        "vietnamese"                                     -> "vi"
        "ukrainian"                                      -> "uk"
        "croatian"                                       -> "hr"
        "serbian"                                        -> "sr"
        "slovenian"                                      -> "sl"
        "slovak"                                         -> "sk"
        "farsi/persian", "persian", "farsi"              -> "fa"
        "hindi"                                          -> "hi"
        else -> {
            Log.d(TAG, "subdlLangToIsoCode: unmapped lang='$lang' → fallback '${lang.lowercase().take(2)}'")
            lang.lowercase().take(2)
        }
    }

    private fun supportsHebrew(languages: String?): Boolean {
        if (languages.isNullOrBlank()) return true
        return languages
            .split(",", " ")
            .map { it.trim().lowercase() }
            .any { it == "he" || it == "heb" || it == "hebrew" }
    }

    private fun SubDLSubtitleDto.matchesRequestedEpisode(season: Int?, episode: Int?): Boolean {
        if (season == null && episode == null) return true

        val dtoSeasonMatches = season == null || this.season == null || this.season == season
        if (episode != null && this.episode != null) {
            return this.episode == episode && dtoSeasonMatches
        }
        if (episode == null && season != null && this.season != null) {
            return this.season == season
        }

        val text = listOfNotNull(releaseName, name).joinToString(" ").lowercase()
        if (season != null && episode != null) {
            val paddedSeason = season.toString().padStart(2, '0')
            val paddedEpisode = episode.toString().padStart(2, '0')
            val patterns = listOf(
                Regex("""s0?$season\s*e0?$episode\b""", RegexOption.IGNORE_CASE),
                Regex("""s$paddedSeason\s*e$paddedEpisode\b""", RegexOption.IGNORE_CASE),
                Regex("""\b0?$season[x.]0?$episode\b""", RegexOption.IGNORE_CASE),
            )
            return patterns.any { it.containsMatchIn(text) }
        }

        if (season != null) {
            val paddedSeason = season.toString().padStart(2, '0')
            return Regex("""\bs0?$season\b|\bs$paddedSeason\b""", RegexOption.IGNORE_CASE)
                .containsMatchIn(text)
        }

        return true
    }

    private fun SubDLSubtitleDto.toSearchResult(id: Int): SubtitleSearchResult {
        val isoCode = lang?.let { subdlLangToIsoCode(it) } ?: "?"
        Log.d(TAG, "SubDL result: lang='$lang' → isoCode='$isoCode'  file=${releaseName ?: name}")
        return SubtitleSearchResult(
        fileId            = id,
        fileName          = releaseName ?: name ?: "subtitle",
        language          = lang ?: "?",
        languageCode      = isoCode,
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
        source            = SubtitleSource.SUBDL,
        )
    }

    private fun ScrewziraSubtitleEntry.toSearchResult(
        id: Int,
        movieTitle: String,
        imdbId: String?,
    ): SubtitleSearchResult {
        return SubtitleSearchResult(
            fileId = id,
            fileName = name,
            language = "Hebrew",
            languageCode = "he",
            downloadCount = 0,
            rating = null,
            format = "srt",
            uploadedAt = null,
            movieTitle = movieTitle,
            movieYear = null,
            imdbId = imdbId,
            isHearingImpaired = false,
            isTrusted = false,
            uploaderName = null,
            source = SubtitleSource.SCREWZIRA,
        )
    }

    private sealed interface ExternalSubtitleHandle {
        data class SubDL(val dto: SubDLSubtitleDto) : ExternalSubtitleHandle
        data class Screwzira(
            val filmId: String,
            val subtitle: ScrewziraSubtitleEntry,
        ) : ExternalSubtitleHandle
    }
}

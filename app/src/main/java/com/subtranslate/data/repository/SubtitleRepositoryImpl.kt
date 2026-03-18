package com.subtranslate.data.repository

import android.util.Log
import com.subtranslate.BuildConfig
import com.subtranslate.data.parser.AssParser
import com.subtranslate.data.parser.SrtParser
import com.subtranslate.data.parser.VttParser
import com.subtranslate.data.remote.opensubtitles.OpenSubtitlesApi
import com.subtranslate.data.remote.opensubtitles.dto.DownloadRequest
import com.subtranslate.data.remote.opensubtitles.mapper.toDomain
import com.subtranslate.data.remote.subdl.SubDLApi
import com.subtranslate.data.remote.subdl.SubDLSubtitleDto
import com.subtranslate.domain.model.SubtitleFile
import com.subtranslate.domain.model.SubtitleFormat
import com.subtranslate.domain.model.SubtitleSearchResult
import com.subtranslate.domain.repository.SubtitleRepository
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
) : SubtitleRepository {

    // Negative IDs are SubDL results; OpenSubtitles always uses positive ints.
    private val subDLIdGen      = AtomicInteger(-1)
    private val subDLRegistry   = mutableMapOf<Int, SubDLSubtitleDto>()

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

    suspend fun searchSubDL(
        title: String?,
        imdbId: String?    = null,
        season: Int?       = null,
        episode: Int?      = null,
        languages: String? = null,
        type: String?      = null,
    ): List<SubtitleSearchResult> {
        val apiKey = BuildConfig.SUBDL_API_KEY.ifBlank { return emptyList() }
        return try {
            val resp = subDLApi.searchSubtitles(
                apiKey    = apiKey,
                title     = title,
                imdbId    = imdbId,
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
            val msg = osEx.message ?: ""
            Log.w(TAG, "OpenSubtitles download failed: $msg")
            throw osEx
        }
    }

    // ── OpenSubtitles download ────────────────────────────────────────────────

    private suspend fun downloadFromOpenSubtitles(fileId: Int, languageCode: String?): SubtitleFile {
        val dlResponse = osApi.requestDownload(DownloadRequest(fileId = fileId))
        Log.d(TAG, "OS download link: ${dlResponse.link}  remaining=${dlResponse.remaining}")

        val request = Request.Builder().url(dlResponse.link).build()
        val content = osHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}: ${response.message}")
            response.body?.string() ?: error("Empty subtitle file")
        }
        return parseSubtitle(content, dlResponse.fileName, languageCode)
    }

    // ── SubDL download (ZIP extraction) ──────────────────────────────────────

    private fun downloadFromSubDLUrl(urlPath: String, fileName: String, languageCode: String?): SubtitleFile {
        val fullUrl = if (urlPath.startsWith("http")) urlPath else "$SUBDL_DL_BASE$urlPath"
        Log.d(TAG, "SubDL download: $fullUrl")

        val request  = Request.Builder().url(fullUrl).build()
        val zipBytes = subDLHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("SubDL HTTP ${response.code}: ${response.message}")
            response.body?.bytes() ?: error("Empty SubDL response")
        }

        val (entryName, content) = extractSubtitleFromZip(zipBytes)
            ?: error("No subtitle file found inside SubDL ZIP")

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

    private fun extractSubtitleFromZip(zipBytes: ByteArray): Pair<String, String>? {
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

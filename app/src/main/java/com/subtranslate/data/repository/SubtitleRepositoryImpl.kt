package com.subtranslate.data.repository

import com.subtranslate.data.parser.AssParser
import com.subtranslate.data.parser.SrtParser
import com.subtranslate.data.parser.VttParser
import com.subtranslate.data.remote.opensubtitles.OpenSubtitlesApi
import com.subtranslate.data.remote.opensubtitles.dto.DownloadRequest
import com.subtranslate.data.remote.opensubtitles.mapper.toDomain
import com.subtranslate.domain.model.SubtitleFile
import com.subtranslate.domain.model.SubtitleFormat
import com.subtranslate.domain.model.SubtitleSearchResult
import com.subtranslate.domain.repository.SubtitleRepository
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class SubtitleRepositoryImpl @Inject constructor(
    private val api: OpenSubtitlesApi,
    @Named("opensubtitles") private val okHttpClient: OkHttpClient
) : SubtitleRepository {

    override suspend fun search(
        query: String?,
        imdbId: Int?,
        languages: String?,
        movieHash: String?,
        season: Int?,
        episode: Int?,
        page: Int
    ): List<SubtitleSearchResult> {
        val response = api.searchSubtitles(
            query = query,
            imdbId = imdbId,
            languages = languages,
            movieHash = movieHash,
            season = season,
            episode = episode,
            page = page
        )
        return response.data.mapNotNull { it.toDomain() }
    }

    override suspend fun download(fileId: Int, languageCode: String?): SubtitleFile {
        val downloadResponse = api.requestDownload(DownloadRequest(fileId = fileId))
        val url = downloadResponse.link
        val fileName = downloadResponse.fileName

        val request = Request.Builder().url(url).build()
        val content = okHttpClient.newCall(request).execute().use { response ->
            response.body?.string() ?: error("Empty subtitle file")
        }

        val extension = fileName.substringAfterLast(".").lowercase()
        val format = SubtitleFormat.fromExtension(extension) ?: SubtitleFormat.SRT

        val parser = when (format) {
            SubtitleFormat.SRT, SubtitleFormat.SUB -> SrtParser()
            SubtitleFormat.VTT -> VttParser()
            SubtitleFormat.ASS, SubtitleFormat.SSA -> AssParser()
        }

        // Carry the languageCode from the search result so TranslateViewModel can auto-detect
        return parser.parse(content).copy(title = fileName, sourceLanguage = languageCode)
    }
}

package com.subtranslate.domain.repository

import com.subtranslate.domain.model.SubtitleFile
import com.subtranslate.domain.model.SubtitleSearchResult

interface SubtitleRepository {
    suspend fun search(
        query: String? = null,
        imdbId: Int? = null,
        languages: String? = null,
        movieHash: String? = null,
        season: Int? = null,
        episode: Int? = null,
        page: Int = 1
    ): List<SubtitleSearchResult>

    suspend fun download(fileId: Int): SubtitleFile
}

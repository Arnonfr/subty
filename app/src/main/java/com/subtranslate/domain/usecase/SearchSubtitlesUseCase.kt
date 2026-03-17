package com.subtranslate.domain.usecase

import com.subtranslate.domain.model.SubtitleSearchResult
import com.subtranslate.domain.repository.SubtitleRepository
import javax.inject.Inject

class SearchSubtitlesUseCase @Inject constructor(
    private val repository: SubtitleRepository
) {
    suspend operator fun invoke(
        query: String? = null,
        imdbId: Int? = null,
        languages: String? = null,
        season: Int? = null,
        episode: Int? = null,
        page: Int = 1
    ): Result<List<SubtitleSearchResult>> = runCatching {
        repository.search(
            query = query,
            imdbId = imdbId,
            languages = languages,
            season = season,
            episode = episode,
            page = page
        )
    }
}

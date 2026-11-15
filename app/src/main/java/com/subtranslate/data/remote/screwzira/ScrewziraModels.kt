package com.subtranslate.data.remote.screwzira

data class ScrewziraSearchCandidate(
    val id: String,
    val englishName: String?,
    val hebrewName: String?,
    val releaseYear: Int?,
    val isSeries: Boolean,
    val imdbId: String?,
)

data class ScrewziraSubtitleEntry(
    val subtitleId: String,
    val name: String,
)

data class ScrewziraSearchResult(
    val filmId: String,
    val imdbId: String?,
    val subtitles: List<ScrewziraSubtitleEntry>,
)

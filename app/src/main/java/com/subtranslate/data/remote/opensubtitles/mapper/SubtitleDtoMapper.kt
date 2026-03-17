package com.subtranslate.data.remote.opensubtitles.mapper

import com.subtranslate.data.remote.opensubtitles.dto.SubtitleSearchDto
import com.subtranslate.domain.model.SubtitleSearchResult

fun SubtitleSearchDto.toDomain(): SubtitleSearchResult? {
    val file = attributes.files?.firstOrNull() ?: return null
    return SubtitleSearchResult(
        fileId = file.fileId,
        fileName = file.fileName ?: attributes.release ?: "subtitle",
        language = attributes.language ?: "unknown",
        languageCode = attributes.language ?: "unknown",
        downloadCount = attributes.downloadCount ?: attributes.newDownloadCount ?: 0,
        rating = attributes.ratings,
        format = file.fileName?.substringAfterLast(".")?.lowercase() ?: "srt",
        uploadedAt = attributes.uploadDate,
        movieTitle = attributes.featureDetails?.title
            ?: attributes.featureDetails?.movieName,
        movieYear = attributes.featureDetails?.year,
        imdbId = attributes.featureDetails?.imdbId?.toString(),
        isHearingImpaired = attributes.hearingImpaired ?: false,
        isTrusted = attributes.fromTrusted ?: false,
        uploaderName = attributes.uploader?.name
    )
}

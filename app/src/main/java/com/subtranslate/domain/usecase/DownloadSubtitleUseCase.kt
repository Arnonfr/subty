package com.subtranslate.domain.usecase

import com.subtranslate.domain.model.SubtitleFile
import com.subtranslate.domain.repository.SubtitleRepository
import javax.inject.Inject

class DownloadSubtitleUseCase @Inject constructor(
    private val repository: SubtitleRepository
) {
    suspend operator fun invoke(fileId: Int): Result<SubtitleFile> = runCatching {
        repository.download(fileId)
    }
}

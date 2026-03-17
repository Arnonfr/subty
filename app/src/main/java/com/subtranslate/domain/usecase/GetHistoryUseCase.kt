package com.subtranslate.domain.usecase

import com.subtranslate.domain.model.HistoryItem
import com.subtranslate.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetHistoryUseCase @Inject constructor(
    private val repository: HistoryRepository
) {
    operator fun invoke(): Flow<List<HistoryItem>> = repository.getAll()
}

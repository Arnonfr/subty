package com.subtranslate.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subtranslate.domain.model.HistoryItem
import com.subtranslate.domain.usecase.GetHistoryUseCase
import com.subtranslate.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    getHistoryUseCase: GetHistoryUseCase,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    val history: StateFlow<List<HistoryItem>> = getHistoryUseCase()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun delete(id: Long) {
        viewModelScope.launch { historyRepository.delete(id) }
    }
}

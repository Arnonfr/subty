package com.subtranslate.presentation.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subtranslate.domain.model.SubtitleSearchResult
import com.subtranslate.domain.usecase.SearchSubtitlesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResultsUiState(
    val results: List<SubtitleSearchResult> = emptyList(),
    val filteredResults: List<SubtitleSearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val languageFilter: String? = null
)

@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val searchUseCase: SearchSubtitlesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResultsUiState())
    val uiState: StateFlow<ResultsUiState> = _uiState

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val result = searchUseCase(query = query.ifBlank { null })
            val items = result.getOrNull() ?: emptyList()
            _uiState.value = _uiState.value.copy(
                results = items,
                filteredResults = items,
                isLoading = false,
                error = result.exceptionOrNull()?.message
            )
        }
    }

    fun filterByLanguage(lang: String?) {
        val all = _uiState.value.results
        _uiState.value = _uiState.value.copy(
            languageFilter = lang,
            filteredResults = if (lang == null) all else all.filter { it.languageCode == lang }
        )
    }
}

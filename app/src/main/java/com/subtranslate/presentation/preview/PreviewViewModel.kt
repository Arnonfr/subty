package com.subtranslate.presentation.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subtranslate.domain.model.SubtitleFile
import com.subtranslate.domain.usecase.DownloadSubtitleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PreviewUiState(
    val subtitleFile: SubtitleFile? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PreviewViewModel @Inject constructor(
    private val downloadUseCase: DownloadSubtitleUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PreviewUiState())
    val uiState: StateFlow<PreviewUiState> = _uiState

    fun load(fileId: Int) {
        _uiState.value = PreviewUiState(isLoading = true)
        viewModelScope.launch {
            val result = downloadUseCase(fileId)
            _uiState.value = PreviewUiState(
                subtitleFile = result.getOrNull(),
                error = result.exceptionOrNull()?.message
            )
        }
    }
}

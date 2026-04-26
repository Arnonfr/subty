package com.subtranslate.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subtranslate.data.local.dao.SearchHistoryDao
import com.subtranslate.data.local.entity.SearchHistoryEntity
import com.subtranslate.data.remote.tmdb.SearchSession
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
    private val historyRepository: HistoryRepository,
    private val searchHistoryDao: SearchHistoryDao,
    private val searchSession: SearchSession,
) : ViewModel() {

    val history: StateFlow<List<HistoryItem>> = getHistoryUseCase()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val searchHistory: StateFlow<List<SearchHistoryEntity>> = searchHistoryDao.getAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun delete(id: Long) {
        viewModelScope.launch { historyRepository.delete(id) }
    }

    fun deleteSearch(id: Long) {
        viewModelScope.launch { searchHistoryDao.deleteById(id) }
    }

    fun clearAllDownloads() {
        viewModelScope.launch { historyRepository.deleteAll() }
    }

    fun clearAllSearches() {
        viewModelScope.launch { searchHistoryDao.deleteAll() }
    }

    /** Restore search params into the shared session before navigating to Results. */
    fun restoreSession(item: SearchHistoryEntity) {
        searchSession.movieTitle  = item.query
        searchSession.season      = item.season
        searchSession.episode     = item.episode
        searchSession.languages   = item.languages
        searchSession.contentType = item.contentType
        searchSession.imdbId      = null
        searchSession.posterUrl   = null
    }

    /** Set pending browse data so SearchScreen pre-populates with this show on next entry. */
    fun prepareSeriesBrowse(item: SearchHistoryEntity) {
        searchSession.movieTitle  = item.query
        searchSession.season      = null
        searchSession.episode     = null
        searchSession.languages   = item.languages
        searchSession.contentType = "tv"
        searchSession.imdbId      = null
        searchSession.posterUrl   = null
        searchSession.pendingBrowseTitle = null
        searchSession.pendingBrowseLangs = null
    }
}

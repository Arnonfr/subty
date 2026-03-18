package com.subtranslate.service

import com.subtranslate.domain.model.TranslationProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationStateHolder @Inject constructor() {
    private val _progress = MutableStateFlow(TranslationProgress())
    val progress: StateFlow<TranslationProgress> = _progress

    fun update(p: TranslationProgress) { _progress.value = p }
    fun reset() { _progress.value = TranslationProgress() }
}

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
    private var generation = 0L

    @Synchronized
    fun beginTranslation(): Long {
        generation += 1
        _progress.value = TranslationProgress()
        return generation
    }

    // Cancellation invalidates the job token before its queued progress can reach the UI.
    @Synchronized
    fun update(p: TranslationProgress, jobId: Long) {
        if (jobId == generation) _progress.value = p
    }

    fun reset() { beginTranslation() }
}

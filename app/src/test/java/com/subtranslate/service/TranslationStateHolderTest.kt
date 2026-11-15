package com.subtranslate.service

import com.subtranslate.domain.model.TranslationProgress
import com.subtranslate.domain.model.TranslationStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class TranslationStateHolderTest {
    @Test
    fun progressQueuedBeforeCancellationCannotRestoreRunningState() {
        val holder = TranslationStateHolder()
        val job = holder.beginTranslation()
        val running = TranslationProgress(status = TranslationStatus.TRANSLATING, translatedEntries = 25, totalEntries = 100)
        holder.update(running, job)
        holder.reset()
        holder.update(running, job)
        assertEquals(TranslationStatus.IDLE, holder.progress.value.status)
    }

    @Test
    fun oldCompletionCannotReplaceTheNextTranslation() {
        val holder = TranslationStateHolder()
        val first = holder.beginTranslation()
        val second = holder.beginTranslation()
        holder.update(TranslationProgress(status = TranslationStatus.TRANSLATING, translatedEntries = 3), second)
        holder.update(TranslationProgress(status = TranslationStatus.COMPLETE, translatedEntries = 100), first)
        assertEquals(TranslationStatus.TRANSLATING, holder.progress.value.status)
        assertEquals(3, holder.progress.value.translatedEntries)
    }
}

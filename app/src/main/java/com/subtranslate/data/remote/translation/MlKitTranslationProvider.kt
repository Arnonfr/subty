package com.subtranslate.data.remote.translation

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.subtranslate.domain.model.SubtitleEntry
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/** One ML Kit translator is retained for a complete subtitle job and always closed by [translateEntries]. */
@Singleton
class MlKitTranslationProvider @Inject constructor() {
    suspend fun translateEntries(
        entries: List<SubtitleEntry>,
        sourceLang: String,
        targetLang: String,
        onPreparing: suspend (String) -> Unit,
        onProgress: (Int, Int) -> Unit,
    ): List<SubtitleEntry> {
        val source = languageFor(sourceLang) ?: throw UnsupportedOfflineLanguageException(sourceLang)
        val target = languageFor(targetLang) ?: throw UnsupportedOfflineLanguageException(targetLang)
        if (source == target) return entries

        val translator = Translation.getClient(
            TranslatorOptions.Builder().setSourceLanguage(source).setTargetLanguage(target).build(),
        )
        try {
            onPreparing("Preparing offline translation. Downloading language models if needed…")
            // No Wi-Fi restriction here: pressing Translate is explicit consent to download on mobile data.
            translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
            onPreparing("Ready for offline translation")
            return SubtitleTranslationProcessor.translate(entries, targetLang, { text -> translator.translate(text).await() }, onProgress)
        } finally {
            translator.close()
        }
    }

    suspend fun downloadedLanguageCodes(): Set<String> = RemoteModelManager.getInstance()
        .getDownloadedModels(TranslateRemoteModel::class.java).await()
        .map { it.language }.toSet()

    suspend fun downloadLanguage(languageCode: String) {
        val language = languageFor(languageCode) ?: throw UnsupportedOfflineLanguageException(languageCode)
        RemoteModelManager.getInstance().download(TranslateRemoteModel.Builder(language).build(), DownloadConditions.Builder().build()).await()
    }

    suspend fun deleteLanguage(languageCode: String) {
        val language = languageFor(languageCode) ?: throw UnsupportedOfflineLanguageException(languageCode)
        RemoteModelManager.getInstance().deleteDownloadedModel(TranslateRemoteModel.Builder(language).build()).await()
    }

    fun supports(languageCode: String): Boolean = languageFor(languageCode) != null

    private fun languageFor(languageCode: String): String? {
        val normalized = languageCode.trim().lowercase().replace('_', '-')
        return TranslateLanguage.fromLanguageTag(normalized)
            ?: TranslateLanguage.fromLanguageTag(normalized.substringBefore('-'))
    }
}

class UnsupportedOfflineLanguageException(language: String) : IllegalArgumentException(
    "$language is not available for offline translation. Choose a supported language.",
)

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}

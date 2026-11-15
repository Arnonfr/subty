package com.subtranslate.data.remote.translation

/** The launch build intentionally supports only on-device translation. */
object TranslationModelCatalog {
    const val MLKIT_ON_DEVICE = "mlkit"

    val launchModels = listOf(MLKIT_ON_DEVICE)

    /** Migrates every previously persisted cloud choice to the offline engine. */
    fun normalize(modelId: String?): String = MLKIT_ON_DEVICE

    fun isSupported(modelId: String): Boolean = normalize(modelId) == MLKIT_ON_DEVICE

    fun displayName(modelId: String): String = "Offline translation"
}

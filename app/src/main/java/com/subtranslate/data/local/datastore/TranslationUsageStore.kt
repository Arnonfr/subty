package com.subtranslate.data.local.datastore

interface TranslationUsageStore {
    val starBalance: Int
    val freeTranslationUsed: Boolean

    fun addCharsUsed(engine: String, chars: Int)

    fun debitStars(stars: Int)

    fun markFreeTranslationUsed()
}

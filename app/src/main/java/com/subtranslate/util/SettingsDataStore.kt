package com.subtranslate.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsDataStore @Inject constructor(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "subty_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // ── Translation ──────────────────────────────────────────────────────────

    var defaultSourceLanguage: String
        get() = prefs.getString(KEY_SOURCE_LANG, "en") ?: "en"
        set(value) = prefs.edit().putString(KEY_SOURCE_LANG, value).apply()

    var defaultTargetLanguage: String
        get() = prefs.getString(KEY_TARGET_LANG, "he") ?: "he"
        set(value) = prefs.edit().putString(KEY_TARGET_LANG, value).apply()

    var translationModel: String
        get() = prefs.getString(KEY_MODEL, "google") ?: "google"
        set(value) = prefs.edit().putString(KEY_MODEL, value).apply()

    var autoTranslateAfterDownload: Boolean
        get() = prefs.getBoolean(KEY_AUTO_TRANSLATE, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_TRANSLATE, value).apply()

    // ── Display ──────────────────────────────────────────────────────────────

    var showPosters: Boolean
        get() = prefs.getBoolean(KEY_SHOW_POSTERS, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_POSTERS, value).apply()

    var compactResults: Boolean
        get() = prefs.getBoolean(KEY_COMPACT_RESULTS, false)
        set(value) = prefs.edit().putBoolean(KEY_COMPACT_RESULTS, value).apply()

    // ── Save ─────────────────────────────────────────────────────────────────

    var preferredSaveFormat: String
        get() = prefs.getString(KEY_SAVE_FORMAT, "srt") ?: "srt"
        set(value) = prefs.edit().putString(KEY_SAVE_FORMAT, value).apply()

    var autoSaveTranslated: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SAVE, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SAVE, value).apply()

    companion object {
        private const val KEY_SOURCE_LANG = "source_lang"
        private const val KEY_TARGET_LANG = "target_lang"
        private const val KEY_MODEL = "translation_model"
        private const val KEY_AUTO_TRANSLATE = "auto_translate"
        private const val KEY_SHOW_POSTERS = "show_posters"
        private const val KEY_COMPACT_RESULTS = "compact_results"
        private const val KEY_SAVE_FORMAT = "save_format"
        private const val KEY_AUTO_SAVE = "auto_save"
    }
}

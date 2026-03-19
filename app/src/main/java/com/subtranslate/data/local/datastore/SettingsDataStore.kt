package com.subtranslate.data.local.datastore

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SettingsDataStore(context: Context) {

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
        get() = prefs.getString("translation_model", "google") ?: "google"
        set(v) = prefs.edit().putString("translation_model", v).apply()

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

    // ── API Keys ─────────────────────────────────────────────────────────────

    var geminiApiKey: String?
        get() = prefs.getString(KEY_GEMINI_API_KEY, null)
        set(value) = prefs.edit().putString(KEY_GEMINI_API_KEY, value).apply()

    var openSubtitlesApiKey: String?
        get() = prefs.getString(KEY_OPENSUBTITLES_API_KEY, null)
        set(value) = prefs.edit().putString(KEY_OPENSUBTITLES_API_KEY, value).apply()

    var googleTranslateApiKey: String?
        get() = prefs.getString("google_translate_api_key", null)
        set(v) = prefs.edit().putString("google_translate_api_key", v).apply()

    companion object {
        private const val KEY_SOURCE_LANG = "source_lang"
        private const val KEY_TARGET_LANG = "target_lang"
        private const val KEY_MODEL = "translation_model"
        private const val KEY_AUTO_TRANSLATE = "auto_translate"
        private const val KEY_SHOW_POSTERS = "show_posters"
        private const val KEY_COMPACT_RESULTS = "compact_results"
        private const val KEY_SAVE_FORMAT = "save_format"
        private const val KEY_AUTO_SAVE = "auto_save"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_OPENSUBTITLES_API_KEY = "opensubtitles_api_key"
    }
}

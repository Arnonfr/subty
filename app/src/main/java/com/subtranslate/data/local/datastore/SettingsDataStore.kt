package com.subtranslate.data.local.datastore

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.subtranslate.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    init {
        if (!prefs.contains(KEY_LIGHT_THEME_DEFAULT_APPLIED)) {
            prefs.edit()
                .putBoolean("is_dark_theme", false)
                .putBoolean(KEY_LIGHT_THEME_DEFAULT_APPLIED, true)
                .apply()
        }
    }

    // ── Translation ──────────────────────────────────────────────────────────

    var defaultSourceLanguage: String
        get() = prefs.getString(KEY_SOURCE_LANG, "en") ?: "en"
        set(value) = prefs.edit().putString(KEY_SOURCE_LANG, value).apply()

    var defaultTargetLanguage: String
        get() = prefs.getString(KEY_TARGET_LANG, "he") ?: "he"
        set(value) = prefs.edit().putString(KEY_TARGET_LANG, value).apply()

    var translationModel: String
        get() = when (val model = prefs.getString(KEY_MODEL, "")) {
            "google" -> "mymemory"
            "" -> BuildConfig.MICROSOFT_API_KEY.takeIf { it.isNotBlank() }?.let { "microsoft" } ?: "mymemory"
            null -> BuildConfig.MICROSOFT_API_KEY.takeIf { it.isNotBlank() }?.let { "microsoft" } ?: "mymemory"
            else -> model
        }
        set(v) = prefs.edit().putString(KEY_MODEL, v).apply()

    var autoTranslateAfterDownload: Boolean
        get() = prefs.getBoolean(KEY_AUTO_TRANSLATE, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_TRANSLATE, value).apply()

    // ── Display ──────────────────────────────────────────────────────────────

    private val _isDarkThemeFlow = MutableStateFlow(prefs.getBoolean("is_dark_theme", false))
    val isDarkThemeFlow: StateFlow<Boolean> = _isDarkThemeFlow.asStateFlow()

    var isDarkTheme: Boolean
        get() = prefs.getBoolean("is_dark_theme", false)
        set(v) {
            prefs.edit().putBoolean("is_dark_theme", v).apply()
            _isDarkThemeFlow.value = v
        }

    var useSeasonEpisodeTextFields: Boolean
        get() = prefs.getBoolean(KEY_SEASON_EPISODE_TEXT_FIELDS, false)
        set(value) = prefs.edit().putBoolean(KEY_SEASON_EPISODE_TEXT_FIELDS, value).apply()

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

    var deeplApiKey: String?
        get() = prefs.getString("deepl_api_key", null)
        set(v) = prefs.edit().putString("deepl_api_key", v).apply()

    var microsoftApiKey: String?
        get() = prefs.getString("microsoft_api_key", null)
        set(v) = prefs.edit().putString("microsoft_api_key", v).apply()

    val effectiveMicrosoftApiKey: String?
        get() = microsoftApiKey?.takeIf { it.isNotBlank() }
            ?: BuildConfig.MICROSOFT_API_KEY.takeIf { it.isNotBlank() }

    var microsoftRegion: String
        get() = prefs.getString("microsoft_region", null)
            ?: BuildConfig.MICROSOFT_REGION.takeIf { it.isNotBlank() }
            ?: "global"
        set(v) = prefs.edit().putString("microsoft_region", v).apply()

    // ── Usage Tracking ───────────────────────────────────────────────────────

    /** Returns total characters translated this month for a given engine. */
    fun getCharsUsed(engine: String): Int {
        val currentMonth = java.time.YearMonth.now().toString()
        val storedMonth = prefs.getString("usage_month", "") ?: ""
        if (storedMonth != currentMonth) {
            // New month — reset counters implicitly by returning 0 until saved
            return 0
        }
        return prefs.getInt("chars_used_$engine", 0)
    }

    /** Adds translated characters to the current month's counter for an engine. */
    fun addCharsUsed(engine: String, chars: Int) {
        val currentMonth = java.time.YearMonth.now().toString()
        val storedMonth = prefs.getString("usage_month", "") ?: ""
        val editor = prefs.edit()
        if (storedMonth != currentMonth) {
            // Reset all engine counters for new month
            editor.putString("usage_month", currentMonth)
            listOf("mymemory", "deepl", "microsoft", "gemini").forEach {
                editor.putInt("chars_used_$it", 0)
            }
        }
        val current = prefs.getInt("chars_used_$engine", 0)
        editor.putInt("chars_used_$engine", current + chars)
        editor.apply()
    }

    companion object {
        private const val KEY_SOURCE_LANG = "source_lang"
        private const val KEY_TARGET_LANG = "target_lang"
        private const val KEY_MODEL = "translation_model"
        private const val KEY_AUTO_TRANSLATE = "auto_translate"
        private const val KEY_SHOW_POSTERS = "show_posters"
        private const val KEY_COMPACT_RESULTS = "compact_results"
        private const val KEY_SEASON_EPISODE_TEXT_FIELDS = "season_episode_text_fields"
        private const val KEY_LIGHT_THEME_DEFAULT_APPLIED = "light_theme_default_applied"
        private const val KEY_SAVE_FORMAT = "save_format"
        private const val KEY_AUTO_SAVE = "auto_save"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_OPENSUBTITLES_API_KEY = "opensubtitles_api_key"
    }
}

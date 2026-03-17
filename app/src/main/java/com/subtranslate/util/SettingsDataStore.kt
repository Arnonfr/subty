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
        "subtranslate_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var openSubtitlesApiKey: String
        get() = prefs.getString(KEY_OS_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_OS_API_KEY, value).apply()

    var openSubtitlesUsername: String
        get() = prefs.getString(KEY_OS_USERNAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_OS_USERNAME, value).apply()

    var openSubtitlesPassword: String
        get() = prefs.getString(KEY_OS_PASSWORD, "") ?: ""
        set(value) = prefs.edit().putString(KEY_OS_PASSWORD, value).apply()

    var openSubtitlesJwt: String?
        get() = prefs.getString(KEY_OS_JWT, null)
        set(value) = prefs.edit().putString(KEY_OS_JWT, value).apply()

    var anthropicApiKey: String
        get() = prefs.getString(KEY_ANTHROPIC_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ANTHROPIC_KEY, value).apply()

    var defaultSourceLanguage: String
        get() = prefs.getString(KEY_DEFAULT_SOURCE_LANG, "en") ?: "en"
        set(value) = prefs.edit().putString(KEY_DEFAULT_SOURCE_LANG, value).apply()

    var defaultTargetLanguage: String
        get() = prefs.getString(KEY_DEFAULT_TARGET_LANG, "he") ?: "he"
        set(value) = prefs.edit().putString(KEY_DEFAULT_TARGET_LANG, value).apply()

    var translationModel: String
        get() = prefs.getString(KEY_TRANSLATION_MODEL, "claude-sonnet-4-5") ?: "claude-sonnet-4-5"
        set(value) = prefs.edit().putString(KEY_TRANSLATION_MODEL, value).apply()

    companion object {
        private const val KEY_OS_API_KEY = "os_api_key"
        private const val KEY_OS_USERNAME = "os_username"
        private const val KEY_OS_PASSWORD = "os_password"
        private const val KEY_OS_JWT = "os_jwt"
        private const val KEY_ANTHROPIC_KEY = "anthropic_key"
        private const val KEY_DEFAULT_SOURCE_LANG = "default_source_lang"
        private const val KEY_DEFAULT_TARGET_LANG = "default_target_lang"
        private const val KEY_TRANSLATION_MODEL = "translation_model"
    }
}

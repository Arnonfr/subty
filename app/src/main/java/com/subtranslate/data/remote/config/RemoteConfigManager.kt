package com.subtranslate.data.remote.config

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class AppConfig(
    val appEnabled: Boolean,
    val maintenanceMessage: String,
    val searchEnabled: Boolean = true,
    val translateEnabled: Boolean = true,
    val defaultTranslationEngine: String = "",
)

@Singleton
class RemoteConfigManager @Inject constructor() {

    companion object {
        private const val KEY_APP_ENABLED              = "app_enabled"
        private const val KEY_MAINTENANCE_MESSAGE       = "maintenance_message"
        private const val KEY_SEARCH_ENABLED            = "search_enabled"
        private const val KEY_TRANSLATE_ENABLED         = "translate_enabled"
        private const val KEY_DEFAULT_TRANSLATION_ENGINE = "default_translation_engine"

        // Default values — app works normally if Firebase is unreachable
        private val DEFAULTS = mapOf(
            KEY_APP_ENABLED              to true,
            KEY_MAINTENANCE_MESSAGE      to "We'll be back shortly. Thanks for your patience!",
            KEY_SEARCH_ENABLED           to true,
            KEY_TRANSLATE_ENABLED        to true,
            KEY_DEFAULT_TRANSLATION_ENGINE to "",
        )

        // In production: 1 hour cache. During dev you can lower this.
        private const val FETCH_INTERVAL_SECONDS = 3600L
    }

    private val remoteConfig = Firebase.remoteConfig

    init {
        remoteConfig.setConfigSettingsAsync(
            remoteConfigSettings { minimumFetchIntervalInSeconds = FETCH_INTERVAL_SECONDS }
        )
        remoteConfig.setDefaultsAsync(DEFAULTS)
    }

    /**
     * Fetches fresh config from Firebase and returns the current [AppConfig].
     * Falls back to defaults / last cached values if fetch fails (e.g. offline).
     */
    suspend fun fetchAndGet(): AppConfig {
        runCatching { remoteConfig.fetchAndActivate().await() }
        return current()
    }

    /** Returns the currently cached config without a network call. */
    fun current(): AppConfig = AppConfig(
        appEnabled              = remoteConfig.getBoolean(KEY_APP_ENABLED),
        maintenanceMessage      = remoteConfig.getString(KEY_MAINTENANCE_MESSAGE),
        searchEnabled           = remoteConfig.getBoolean(KEY_SEARCH_ENABLED),
        translateEnabled        = remoteConfig.getBoolean(KEY_TRANSLATE_ENABLED),
        defaultTranslationEngine = remoteConfig.getString(KEY_DEFAULT_TRANSLATION_ENGINE),
    )
}

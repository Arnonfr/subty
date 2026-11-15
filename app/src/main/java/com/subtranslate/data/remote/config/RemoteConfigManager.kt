package com.subtranslate.data.remote.config

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
    /**
     * Fixed local configuration for the cost-free release. Remote Config and
     * Firestore controls are intentionally absent, so launch has no backend read.
     */
    suspend fun fetchAndGet(): AppConfig = current()

    fun current(): AppConfig = AppConfig(
        appEnabled = true,
        maintenanceMessage = "",
        searchEnabled = true,
        translateEnabled = true,
        defaultTranslationEngine = "",
    )
}

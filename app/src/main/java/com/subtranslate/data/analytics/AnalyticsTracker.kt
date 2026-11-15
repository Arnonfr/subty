package com.subtranslate.data.analytics

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.subtranslate.data.local.datastore.SettingsDataStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsTracker @Inject constructor(
    private val settings: SettingsDataStore,
    private val firestore: FirebaseFirestore,
) {
    fun track(event: String, params: Map<String, String> = emptyMap()) {
        if (!settings.liveAnalyticsEnabled || !EVENT_NAME.matches(event)) return

        val safeParams = params
            .filterKeys { it in ALLOWED_PARAM_KEYS }
            .mapNotNull { (key, value) ->
                when {
                    key == SEARCH_QUERY_KEY && !settings.shareSearchTerms -> null
                    else -> key to value.take(MAX_PARAM_LENGTH)
                }
            }
            .toMap()

        firestore.collection("live_events").document().set(
            mapOf(
                "event" to event,
                "event_key" to "${event}_${System.currentTimeMillis()}",
                "platform" to "android",
                "client_ts" to System.currentTimeMillis(),
                "server_ts" to FieldValue.serverTimestamp(),
                "schema_version" to 1,
                "install_id" to settings.analyticsUserId,
                "params" to safeParams,
            ),
        )
    }

    private companion object {
        val EVENT_NAME = Regex("[a-z][a-z0-9_]{0,79}")
        const val SEARCH_QUERY_KEY = "search_query"
        const val MAX_PARAM_LENGTH = 120
        val ALLOWED_PARAM_KEYS = setOf(
            SEARCH_QUERY_KEY,
            "mode", "has_season_episode", "languages_count", "type", "query_length",
            "file_id", "language_code", "model", "engine", "source_lang", "target_lang",
            "chars", "entries", "stars",
        )
    }
}

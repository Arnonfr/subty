package com.subtranslate.data.remote.opensubtitles

import javax.inject.Inject
import javax.inject.Singleton

/** Holds the OpenSubtitles JWT obtained after login. In-memory only — no persistence needed. */
@Singleton
class SessionStore @Inject constructor() {
    var jwtToken: String? = null
}

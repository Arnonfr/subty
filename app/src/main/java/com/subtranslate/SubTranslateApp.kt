package com.subtranslate

import android.app.Application
import com.subtranslate.service.TranslationForegroundService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SubTranslateApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TranslationForegroundService.createChannel(this)
    }
}

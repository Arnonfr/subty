package com.subtranslate.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.subtranslate.data.remote.translation.TranslationModelCatalog
import com.subtranslate.domain.model.SubtitleFile
import com.subtranslate.domain.model.TranslationStatus
import com.subtranslate.domain.usecase.TranslateSubtitleUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class TranslationForegroundService : Service() {

    @Inject lateinit var translateUseCase: TranslateSubtitleUseCase
    @Inject lateinit var stateHolder: TranslationStateHolder

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var translationJob: Job? = null
    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    companion object {
        const val CHANNEL_ID = "translation_channel"
        const val NOTIFICATION_ID = 42

        const val EXTRA_SOURCE_LANG = "source_lang"
        const val EXTRA_TARGET_LANG = "target_lang"
        const val EXTRA_MODEL_ID    = "model_id"
        const val EXTRA_JOB_ID = "job_id"

        // Static slot to pass the SubtitleFile (too large for Intent extras)
        var pendingFile: SubtitleFile? = null

        fun createChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Translation",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Subtitle translation progress" }
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sourceLang = intent?.getStringExtra(EXTRA_SOURCE_LANG) ?: "en"
        val jobId = intent?.getLongExtra(EXTRA_JOB_ID, -1L) ?: -1L
        val targetLang = intent?.getStringExtra(EXTRA_TARGET_LANG) ?: "he"
        val modelId = TranslationModelCatalog.normalize(
            intent?.getStringExtra(EXTRA_MODEL_ID),
        )
        val file       = pendingFile ?: run { stopSelf(); return START_NOT_STICKY }
        pendingFile = null
        translationJob?.cancel()

        startForeground(NOTIFICATION_ID, buildNotification("Starting\u2026", 0, 0))

        translationJob = serviceScope.launch {
            var lastNotificationAt = 0L
            var lastPercent = -1
            translateUseCase(
                subtitleFile = file,
                sourceLang   = sourceLang,
                targetLang   = targetLang,
                modelId      = modelId
            ).onEach { progress ->
                stateHolder.update(progress, jobId)
                val msg = when (progress.status) {
                    TranslationStatus.TRANSLATING ->
                        "${progress.translatedEntries}/${progress.totalEntries} subtitles"
                    TranslationStatus.COMPLETE -> "Translation complete \u2713"
                    TranslationStatus.ERROR    -> "Translation failed"
                    else -> "Translating\u2026"
                }
                val pct = if (progress.totalEntries > 0) (progress.translatedEntries * 100 / progress.totalEntries) else 0
                val terminal = progress.status == TranslationStatus.COMPLETE || progress.status == TranslationStatus.ERROR
                val now = SystemClock.elapsedRealtime()
                if (terminal || (pct != lastPercent && now - lastNotificationAt >= 500)) {
                    notificationManager.notify(NOTIFICATION_ID, buildNotification(msg, pct, 100))
                    lastNotificationAt = now
                    lastPercent = pct
                }
                if (progress.status == TranslationStatus.COMPLETE || progress.status == TranslationStatus.ERROR) {
                    // Keep notification briefly then stop
                    delay(3000)
                    stopSelf()
                }
            }.catch { e ->
                if (e is CancellationException) throw e
                stateHolder.update(
                    stateHolder.progress.value.copy(
                        status = TranslationStatus.ERROR,
                        errorMessage = e.message
                    ), jobId
                )
                stopSelf()
            }.collect {}
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun buildNotification(text: String, progress: Int, max: Int): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Translating subtitles")
            .setContentText(text)
            .setProgress(max, progress, max == 0)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}

package com.subtranslate.domain.usecase

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.subtranslate.data.parser.AssParser
import com.subtranslate.data.parser.SrtParser
import com.subtranslate.data.parser.VttParser
import com.subtranslate.domain.model.SubtitleFile
import com.subtranslate.domain.model.SubtitleFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class SaveSubtitleUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    operator fun invoke(subtitleFile: SubtitleFile, fileName: String): Result<File> = runCatching {
        val parser = when (subtitleFile.format) {
            SubtitleFormat.SRT, SubtitleFormat.SUB -> SrtParser()
            SubtitleFormat.VTT -> VttParser()
            SubtitleFormat.ASS, SubtitleFormat.SSA -> AssParser()
        }
        val content = parser.write(subtitleFile)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // API 29+: use MediaStore so the file appears in the system Downloads
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("MediaStore insert failed")
            resolver.openOutputStream(uri)?.use { it.writer(Charsets.UTF_8).apply { write(content); flush() } }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            // Return a pseudo-File with the visible Downloads path
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
        } else {
            // API < 29: write directly to the public Downloads folder
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            val file = File(dir, fileName)
            file.writeText(content, Charsets.UTF_8)
            file
        }
    }
}

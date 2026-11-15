package com.subtranslate.domain.usecase

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.subtranslate.data.local.datastore.SettingsDataStore
import com.subtranslate.data.parser.AssParser
import com.subtranslate.data.parser.SrtParser
import com.subtranslate.data.parser.VttParser
import com.subtranslate.domain.model.SubtitleFile
import com.subtranslate.domain.model.SubtitleFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class SaveSubtitleUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsDataStore,
) {
    operator fun invoke(subtitleFile: SubtitleFile, fileName: String): Result<File> = runCatching {
        val parser = when (subtitleFile.format) {
            SubtitleFormat.SRT, SubtitleFormat.SUB -> SrtParser()
            SubtitleFormat.VTT -> VttParser()
            SubtitleFormat.ASS, SubtitleFormat.SSA -> AssParser()
        }

        val content = parser.write(subtitleFile)
        val mimeType = when (subtitleFile.format) {
            SubtitleFormat.SRT, SubtitleFormat.SUB -> "application/x-subrip"
            SubtitleFormat.VTT -> "text/vtt"
            SubtitleFormat.ASS, SubtitleFormat.SSA -> "text/x-ass"
        }

        if (settings.saveDestinationMode == SAVE_DESTINATION_CUSTOM &&
            !settings.customSaveFolderUri.isNullOrBlank()
        ) {
            return@runCatching saveToCustomFolder(
                treeUri = settings.customSaveFolderUri!!,
                fileName = fileName,
                mimeType = mimeType,
                content = content,
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToMoviesFolder(fileName, mimeType, content)
        } else {
            saveToLegacyMoviesFolder(fileName, content)
        }
    }

    private fun saveToCustomFolder(
        treeUri: String,
        fileName: String,
        mimeType: String,
        content: String,
    ): File {
        val uri = Uri.parse(treeUri)
        val tree = DocumentFile.fromTreeUri(context, uri)
            ?: error("Selected folder is no longer available.")
        val document = tree.createFile(mimeType, fileName)
            ?: error("Failed to create file in selected folder.")

        context.contentResolver.openOutputStream(document.uri)?.use { stream ->
            stream.write(content.toByteArray(Charsets.UTF_8))
        } ?: error("Failed to write subtitle file.")

        return File(fileName)
    }

    private fun saveToMoviesFolder(fileName: String, mimeType: String, content: String): File {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, defaultSubtitleRelativePath())
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val uri = resolver.insert(
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            values,
        ) ?: error("MediaStore insert failed")

        resolver.openOutputStream(uri)?.use { stream ->
            stream.write(content.toByteArray(Charsets.UTF_8))
        } ?: error("Failed to write subtitle file.")

        resolver.update(
            uri,
            ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
            null,
            null,
        )

        return File(defaultSubtitleFolderLabel(), fileName)
    }

    private fun saveToLegacyMoviesFolder(fileName: String, content: String): File {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            DEFAULT_SUBTITLE_FOLDER_NAME,
        )
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val file = File(dir, fileName)
        file.writeText(content)
        return file
    }
}

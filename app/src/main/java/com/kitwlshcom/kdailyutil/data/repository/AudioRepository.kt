package com.kitwlshcom.kdailyutil.data.repository

import android.content.Context
import com.kitwlshcom.kdailyutil.data.model.AudioItem
import java.io.File

class AudioRepository(private val context: Context) {

    private val captureDir: File
        get() = File(context.filesDir, "captures").apply { if (!exists()) mkdirs() }

    fun getRecordedFiles(): List<AudioItem> {
        val retriever = android.media.MediaMetadataRetriever()
        return captureDir.listFiles()
            ?.filter { it.isFile && (it.extension == "m4a" || it.extension == "wav") }
            ?.map { file ->
                var duration = 0L
                try {
                    retriever.setDataSource(file.absolutePath)
                    duration = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
                } catch (e: Exception) {
                    // Ignore errors for individual files
                }
                AudioItem(
                    name = file.name,
                    path = file.absolutePath,
                    duration = duration,
                    size = file.length(),
                    dateAdded = file.lastModified()
                )
            }
            ?.sortedByDescending { it.dateAdded }
            ?: emptyList()
    }

    fun renameFile(item: AudioItem, newName: String): Boolean {
        val extension = item.file.extension
        val cleanName = if (newName.endsWith(".$extension")) newName else "$newName.$extension"
        val newFile = File(captureDir, cleanName)
        return item.file.renameTo(newFile)
    }

    fun deleteFile(item: AudioItem): Boolean {
        return item.file.delete()
    }

    fun getNewFilePath(extension: String = "m4a"): String {
        val timestamp = System.currentTimeMillis()
        return File(captureDir, "capture_$timestamp.$extension").absolutePath
    }
}

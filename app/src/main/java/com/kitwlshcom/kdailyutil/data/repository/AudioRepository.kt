package com.kitwlshcom.kdailyutil.data.repository

import android.content.Context
import com.kitwlshcom.kdailyutil.data.model.AudioItem
import java.io.File

class AudioRepository(private val context: Context) {

    private val captureDir: File
        get() = File(context.filesDir, "captures").apply { if (!exists()) mkdirs() }

    private val hiddenDir: File
        get() = File(captureDir, "hidden").apply { if (!exists()) mkdirs() }

    fun getRecordedFiles(): List<AudioItem> {
        val retriever = android.media.MediaMetadataRetriever()
        val supportedExtensions = listOf("m4a", "wav", "mp3", "mp4", "mkv", "aac", "3gp")
        return captureDir.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in supportedExtensions }
            ?.map { file ->
                mapToFileItem(file, retriever)
            }
            ?.sortedByDescending { it.dateAdded }
            ?: emptyList()
    }

    fun getHiddenFiles(): List<AudioItem> {
        val retriever = android.media.MediaMetadataRetriever()
        val supportedExtensions = listOf("m4a", "wav", "mp3", "mp4", "mkv", "aac", "3gp")
        return hiddenDir.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in supportedExtensions }
            ?.map { file ->
                mapToFileItem(file, retriever)
            }
            ?.sortedByDescending { it.dateAdded }
            ?: emptyList()
    }

    private fun mapToFileItem(file: File, retriever: android.media.MediaMetadataRetriever): AudioItem {
        var duration = 0L
        try {
            retriever.setDataSource(file.absolutePath)
            duration = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
        } catch (e: Exception) {
            // Ignore errors for individual files
        }
        return AudioItem(
            name = file.name,
            path = file.absolutePath,
            duration = duration,
            size = file.length(),
            dateAdded = file.lastModified()
        )
    }

    fun hideFile(item: AudioItem): Boolean {
        val target = File(hiddenDir, item.name)
        return item.file.renameTo(target)
    }

    fun restoreFile(item: AudioItem): Boolean {
        val target = File(captureDir, item.name)
        return item.file.renameTo(target)
    }

    fun importFile(uri: android.net.Uri): Boolean {
        return try {
            val contentResolver = context.contentResolver
            val fileName = getFileName(uri) ?: "imported_${System.currentTimeMillis()}"
            val destinationFile = File(captureDir, fileName)
            
            contentResolver.openInputStream(uri)?.use { inputStream ->
                destinationFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun getFileName(uri: android.net.Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = it.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
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

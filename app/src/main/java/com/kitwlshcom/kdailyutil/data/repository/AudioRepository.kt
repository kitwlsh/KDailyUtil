package com.kitwlshcom.kdailyutil.data.repository

import android.content.Context
import com.kitwlshcom.kdailyutil.data.model.AudioItem
import java.io.File

class AudioRepository(private val context: Context) {

    private val captureDir: File
        get() = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "KDailyUtil").apply { if (!exists()) mkdirs() }

    private val hiddenDir: File
        get() = File(captureDir, "hidden").apply { if (!exists()) mkdirs() }

    private val trashDir: File
        get() = File(captureDir, "trash").apply { if (!exists()) mkdirs() }

    private val oldCaptureDir: File
        get() = File(context.filesDir, "captures")

    private val previousExternalDir: File
        get() = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC), "captures")

    private val orderFile: File
        get() = File(context.filesDir, "audio_order.txt")

    init {
        migrateInternalFiles()
    }

    private fun migrateInternalFiles() {
        // 1. 내부 저장소(filesDir)에서 이동
        migrateFrom(oldCaptureDir)
        // 2. 이전 외부 저장소(Android/data)에서 이동
        migrateFrom(previousExternalDir)
    }

    private fun migrateFrom(sourceDir: File) {
        if (sourceDir.exists() && sourceDir.isDirectory) {
            val files = sourceDir.listFiles()
            files?.forEach { file ->
                if (file.isFile) {
                    val target = File(captureDir, file.name)
                    if (!target.exists()) {
                        try {
                            file.copyTo(target, overwrite = true)
                            file.delete()
                        } catch (e: Exception) { e.printStackTrace() }
                    } else {
                        file.delete() // 이미 존재하면 삭제
                    }
                } else if (file.isDirectory && file.name == "hidden") {
                    file.listFiles()?.forEach { hiddenFile ->
                        if (hiddenFile.isFile) {
                            val target = File(hiddenDir, hiddenFile.name)
                            if (!target.exists()) {
                                try {
                                    hiddenFile.copyTo(target, overwrite = true)
                                    hiddenFile.delete()
                                } catch (e: Exception) { e.printStackTrace() }
                            } else {
                                hiddenFile.delete()
                            }
                        }
                    }
                    sourceDir.delete()
                }
            }
            sourceDir.delete()
        }
    }

    fun getRecordedFiles(): List<AudioItem> {
        val retriever = android.media.MediaMetadataRetriever()
        val supportedExtensions = listOf("m4a", "wav", "mp3", "mp4", "mkv", "aac", "3gp")
        val files = captureDir.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in supportedExtensions }
            ?: return emptyList()

        val items = files.map { mapToFileItem(it, retriever) }
        
        // 순서 정보 불러오기
        val order = getSavedOrder()
        return if (order.isEmpty()) {
            items.sortedByDescending { it.dateAdded }
        } else {
            // 저장된 순서대로 정렬, 없는 파일은 뒤로 보냄
            items.sortedWith(compareBy({ 
                val idx = order.indexOf(it.name)
                if (idx == -1) Int.MAX_VALUE else idx 
            }, { -it.dateAdded }))
        }
    }

    private fun getSavedOrder(): List<String> {
        return if (orderFile.exists()) {
            orderFile.readLines().filter { it.isNotBlank() }
        } else {
            emptyList()
        }
    }

    fun saveOrder(items: List<AudioItem>) {
        orderFile.writeText(items.joinToString("\n") { it.name })
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

    fun getTrashFiles(): List<AudioItem> {
        val retriever = android.media.MediaMetadataRetriever()
        val supportedExtensions = listOf("m4a", "wav", "mp3", "mp4", "mkv", "aac", "3gp")
        return trashDir.listFiles()
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

    fun importFiles(uris: List<android.net.Uri>): Int {
        var count = 0
        uris.forEach { uri ->
            if (importFile(uri)) count++
        }
        return count
    }

    fun importFile(uri: android.net.Uri): Boolean {
        return try {
            val contentResolver = context.contentResolver
            var fileName = getFileName(uri) ?: "imported_${System.currentTimeMillis()}"
            
            // 파일명이 중복되면 타임스탬프 추가
            var destinationFile = File(captureDir, fileName)
            if (destinationFile.exists()) {
                val name = fileName.substringBeforeLast(".")
                val ext = fileName.substringAfterLast(".", "")
                fileName = "${name}_${System.currentTimeMillis()}.$ext"
                destinationFile = File(captureDir, fileName)
            }
            
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
        val target = File(trashDir, item.name)
        return item.file.renameTo(target)
    }

    fun restoreFromTrash(item: AudioItem): Boolean {
        val target = File(captureDir, item.name)
        return item.file.renameTo(target)
    }

    fun permanentlyDelete(item: AudioItem): Boolean {
        return item.file.delete()
    }

    fun emptyTrash() {
        trashDir.listFiles()?.forEach { it.delete() }
    }

    fun getNewFilePath(extension: String = "m4a"): String {
        val timestamp = System.currentTimeMillis()
        return File(captureDir, "capture_$timestamp.$extension").absolutePath
    }
}

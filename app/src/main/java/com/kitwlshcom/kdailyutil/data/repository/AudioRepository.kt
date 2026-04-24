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

    private val playlistsDir: File
        get() = File(captureDir, "playlists").apply { if (!exists()) mkdirs() }

    private val importsDir: File
        get() = File(captureDir, "imports").apply { if (!exists()) mkdirs() }

    private val oldCaptureDir: File
        get() = File(context.filesDir, "captures")

    private val previousExternalDir: File
        get() = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC), "captures")

    private val oldPlaylistsDir: File
        get() = File(context.filesDir, "playlists")

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
        // 3. 재생목록 이동
        migratePlaylists()
    }

    private fun migratePlaylists() {
        if (oldPlaylistsDir.exists() && oldPlaylistsDir.isDirectory) {
            oldPlaylistsDir.listFiles()?.forEach { file ->
                if (file.isFile && file.extension == "plt") {
                    val target = File(playlistsDir, file.name)
                    if (!target.exists()) {
                        try {
                            file.copyTo(target, overwrite = true)
                            file.delete()
                        } catch (e: Exception) { e.printStackTrace() }
                    } else {
                        file.delete()
                    }
                }
            }
            oldPlaylistsDir.delete()
        }
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

    fun getPlaylists(): List<String> {
        return playlistsDir.listFiles()
            ?.filter { it.isFile && it.extension == "plt" }
            ?.map { it.nameWithoutExtension }
            ?.sorted()
            ?: emptyList()
    }

    fun getRecordedFiles(playlistName: String? = null): List<AudioItem> {
        val retriever = android.media.MediaMetadataRetriever()
        val supportedExtensions = listOf("m4a", "wav", "mp3", "mp4", "mkv", "aac", "3gp")
        
        if (playlistName == null) {
            // 전체 리스트 (루트 폴더 + imports 폴더 파일들)
            // File API로 먼저 시도 (앱이 생성한 파일들)
            val rootFiles = captureDir.listFiles()?.filter { it.isFile && it.extension.lowercase() in supportedExtensions } ?: emptyList()
            val importedFiles = importsDir.listFiles()?.filter { it.isFile && it.extension.lowercase() in supportedExtensions } ?: emptyList()
            
            // MediaStore로 추가 시도 (사용자가 수동으로 옮긴 파일들)
            val mediaStoreFiles = queryMediaStoreFiles(supportedExtensions)
            
            val allFiles = (rootFiles + importedFiles + mediaStoreFiles).distinctBy { it.absolutePath }
            val items = allFiles.map { mapToFileItem(it, retriever) }
            
            val order = getSavedOrder()
            return if (order.isEmpty()) {
                items.sortedByDescending { it.dateAdded }
            } else {
                items.sortedWith(compareBy({ 
                    val idx = order.indexOf(it.name)
                    if (idx == -1) Int.MAX_VALUE else idx 
                }, { -it.dateAdded }))
            }
        } else {
            // 재생목록 파일 로드
            val playlistFile = File(playlistsDir, "$playlistName.plt")
            if (!playlistFile.exists()) return emptyList()
            
            val paths = playlistFile.readLines().filter { it.isNotBlank() }
            val items = paths.mapNotNull { originalPath ->
                var file = File(originalPath)
                if (!file.exists()) {
                    // 경로가 유효하지 않으면 현재 캡처/임포트 폴더에서 파일명으로 찾아봄
                    val fileName = file.name
                    val resolvedInCapture = File(captureDir, fileName)
                    val resolvedInImports = File(importsDir, fileName)
                    
                    file = when {
                        resolvedInCapture.exists() -> resolvedInCapture
                        resolvedInImports.exists() -> resolvedInImports
                        else -> {
                            // MediaStore에서 파일명으로 전역 검색 시도
                            val foundViaMediaStore = findFileGlobally(fileName)
                            foundViaMediaStore ?: file
                        }
                    }
                }
                
                if (file.exists()) {
                    mapToFileItem(file, retriever)
                } else null
            }
            return items
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

    private fun findFileGlobally(fileName: String): File? {
        val projection = arrayOf(android.provider.MediaStore.Audio.Media.DATA)
        val selection = "${android.provider.MediaStore.Audio.Media.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)
        
        try {
            // 오디오 쿼리
            context.contentResolver.query(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return File(cursor.getString(0))
                }
            }
            
            // 비디오 쿼리
            context.contentResolver.query(
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return File(cursor.getString(0))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun queryMediaStoreFiles(extensions: List<String>): List<File> {
        val files = mutableListOf<File>()
        val projection = arrayOf(
            android.provider.MediaStore.Audio.Media.DATA,
            android.provider.MediaStore.Audio.Media.DISPLAY_NAME
        )
        
        // KDailyUtil 폴더 내의 파일들 쿼리 (RELATIVE_PATH는 API 29+)
        val selection = "${android.provider.MediaStore.Audio.Media.DATA} LIKE ?"
        val selectionArgs = arrayOf("%/KDailyUtil/%")
        
        try {
            context.contentResolver.query(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val dataIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA)
                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataIndex)
                    val file = File(path)
                    if (file.exists() && file.extension.lowercase() in extensions) {
                        files.add(file)
                    }
                }
            }
            
            // 비디오 파일도 쿼리 (mp4, mkv 등)
            context.contentResolver.query(
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val dataIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA)
                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataIndex)
                    val file = File(path)
                    if (file.exists() && file.extension.lowercase() in extensions) {
                        files.add(file)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return files
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
            var destinationFile = File(importsDir, fileName)
            if (destinationFile.exists()) {
                val name = fileName.substringBeforeLast(".")
                val ext = fileName.substringAfterLast(".", "")
                fileName = "${name}_${System.currentTimeMillis()}.$ext"
                destinationFile = File(importsDir, fileName)
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

    fun importPlaylist(uri: android.net.Uri): String? {
        return try {
            val fileName = getFileName(uri) ?: "playlist_${System.currentTimeMillis()}.plt"
            val cleanName = if (fileName.endsWith(".plt")) fileName else "$fileName.plt"
            
            val destinationFile = File(playlistsDir, cleanName)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                destinationFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            cleanName.substringBeforeLast(".plt")
        } catch (e: Exception) {
            e.printStackTrace()
            null
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

    fun createPlaylist(name: String): Boolean {
        val file = File(playlistsDir, "$name.plt")
        return if (!file.exists()) file.createNewFile() else false
    }

    fun deletePlaylist(name: String): Boolean {
        val file = File(playlistsDir, "$name.plt")
        return file.delete()
    }

    fun renamePlaylist(oldName: String, newName: String): Boolean {
        val oldFile = File(playlistsDir, "$oldName.plt")
        val newFile = File(playlistsDir, "$newName.plt")
        return if (oldFile.exists() && !newFile.exists()) {
            oldFile.renameTo(newFile)
        } else false
    }

    fun addItemToPlaylist(item: AudioItem, playlistName: String): Boolean {
        val file = File(playlistsDir, "$playlistName.plt")
        if (!file.exists()) return false
        
        val lines = file.readLines()
        if (item.path in lines) return true // 이미 존재하면 중복 추가 안함
        
        file.appendText("${item.path}\n")
        return true
    }

    fun removeItemFromPlaylist(item: AudioItem, playlistName: String): Boolean {
        val file = File(playlistsDir, "$playlistName.plt")
        if (!file.exists()) return false
        
        val lines = file.readLines().filter { it != item.path }
        file.writeText(lines.joinToString("\n") + if (lines.isNotEmpty()) "\n" else "")
        return true
    }

    fun moveToPlaylist(item: AudioItem, folderName: String?): Boolean {
        // 이 기능은 이제 물리적 이동이 아닌 '목록 추가'로 대체될 수 있지만,
        // 호환성을 위해 남겨두거나addItemToPlaylist로 유도할 수 있음.
        // 여기서는 기존 로직(물리적 폴더 이동)은 유지하되 사용하지 않도록 함.
        val targetDir = if (folderName == null) captureDir else File(captureDir, folderName)
        if (!targetDir.exists()) targetDir.mkdirs()
        
        val targetFile = File(targetDir, item.name)
        return item.file.renameTo(targetFile)
    }
}

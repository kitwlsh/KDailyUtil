package com.kitwlshcom.kdailyutil.data.repository

import android.content.Context
import com.kitwlshcom.kdailyutil.data.model.AudioItem
import java.io.File

class AudioRepository(private val context: Context) {

    private val captureDir: File
        get() = File(context.getExternalFilesDir(null), "KDailyUtil").apply { if (!exists()) mkdirs() }

    private val hiddenDir: File
        get() = File(captureDir, "hidden").apply { if (!exists()) mkdirs() }

    private val trashDir: File
        get() = File(captureDir, "trash").apply { if (!exists()) mkdirs() }

    private val playlistsDir: File
        get() = File(context.filesDir, "playlists").apply { if (!exists()) mkdirs() }

    private val importsDir: File
        get() = File(captureDir, "imports").apply { if (!exists()) mkdirs() }

    private val legacyPublicDir: File
        get() = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "KDailyUtil")

    private val legacyPublicPlaylistsDir: File
        get() = File(legacyPublicDir, "playlists")

    private val oldCaptureDir: File
        get() = File(context.filesDir, "captures")

    private val previousExternalDir: File
        get() = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC), "captures")

    private val oldPlaylistsDir: File
        get() = File(context.filesDir, "playlists")

    private val orderFile: File
        get() = File(context.filesDir, "audio_order.txt")

    init {
        // Migration call moved to initializeStorage()
    }

    fun initializeStorage() {
        try {
            migrateInternalFiles()
        } catch (e: Exception) {
            android.util.Log.e("AudioRepository", "Storage init error", e)
        }
    }

    private fun migrateInternalFiles() {
        // 1. 내부 저장소(filesDir)에서 이동
        migrateFrom(oldCaptureDir)
        // 2. 이전 외부 저장소(Android/data/Music)에서 이동
        migrateFrom(previousExternalDir)
        // 3. 공용 Download 폴더의 이전 데이터에서 이동 (권한이 있는 경우만 작동)
        migrateFrom(legacyPublicDir)
        
        // 4. 재생목록 이동
        migratePlaylists()
    }

    private fun migratePlaylists() {
        // 내부 저장소의 이전 재생목록 위치 확인 (있을 경우 그대로 두거나 새 위치로 이동)
        if (oldPlaylistsDir.exists() && oldPlaylistsDir.isDirectory && oldPlaylistsDir != playlistsDir) {
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
        
        // 공용 Download 폴더에 있던 재생목록 이동 시도 (권한 이슈로 일부 실패할 수 있음)
        if (legacyPublicPlaylistsDir.exists() && legacyPublicPlaylistsDir.isDirectory) {
            legacyPublicPlaylistsDir.listFiles()?.forEach { file ->
                if (file.isFile && file.extension == "plt") {
                    val target = File(playlistsDir, file.name)
                    if (!target.exists()) {
                        try {
                            file.copyTo(target, overwrite = true)
                            // 원본 삭제는 하지 않음 (권한에 따라 에러 날 수 있으므로)
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
            }
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
                            if (file.delete()) {
                                android.util.Log.d("AudioRepository", "Migrated and deleted: ${file.name}")
                            } else {
                                android.util.Log.w("AudioRepository", "Migrated but failed to delete source: ${file.name}")
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    } else {
                        // 이미 존재하면 (사이즈 비교 후 같으면) 삭제
                        if (file.length() == target.length()) {
                            file.delete()
                        }
                    }
                } else if (file.isDirectory && file.name == "hidden") {
                    // hidden 폴더 내 파일 이동
                    file.listFiles()?.forEach { hiddenFile ->
                        if (hiddenFile.isFile) {
                            val target = File(hiddenDir, hiddenFile.name)
                            if (!target.exists()) {
                                try {
                                    hiddenFile.copyTo(target, overwrite = true)
                                    hiddenFile.delete()
                                } catch (e: Exception) { e.printStackTrace() }
                            } else {
                                if (hiddenFile.length() == target.length()) hiddenFile.delete()
                            }
                        }
                    }
                }
            }
            // 폴더가 비어있으면 삭제
            if (sourceDir.listFiles()?.isEmpty() == true) {
                sourceDir.delete()
            }
        }
    }

    fun getPlaylists(): List<String> {
        val dir = playlistsDir
        if (!dir.exists()) dir.mkdirs()
        
        val files = dir.listFiles()
        android.util.Log.d("AudioRepository", "Found ${files?.size ?: 0} files in $dir")
        
        return files
            ?.filter { it.isFile && it.extension == "plt" }
            ?.map { it.nameWithoutExtension }
            ?.sorted()
            ?: emptyList()
    }

    fun getRecordedFiles(playlistName: String? = null): List<AudioItem> {
        val supportedExtensions = listOf("m4a", "wav", "mp3", "mp4", "mkv", "aac", "3gp")
        
        if (playlistName == null) {
            // 전체 리스트 (루트 폴더 + imports 폴더 파일들)
            // File API로 먼저 시도 (앱이 생성한 파일들)
            val rootFiles = captureDir.listFiles()?.filter { it.isFile && it.extension.lowercase() in supportedExtensions } ?: emptyList()
            val importedFiles = importsDir.listFiles()?.filter { it.isFile && it.extension.lowercase() in supportedExtensions } ?: emptyList()
            
            // MediaStore로 추가 시도 (사용자가 수동으로 옮긴 파일들)
            val mediaStoreFiles = queryMediaStoreFiles(supportedExtensions)
            
            val allFiles = (rootFiles + importedFiles + mediaStoreFiles).distinctBy { 
                try { it.canonicalPath } catch (e: Exception) { it.absolutePath }
            }
            val items = allFiles.map { mapToFileItem(it) }
            
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
                    mapToFileItem(file)
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
        val supportedExtensions = listOf("m4a", "wav", "mp3", "mp4", "mkv", "aac", "3gp")
        return hiddenDir.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in supportedExtensions }
            ?.map { file ->
                mapToFileItem(file)
            }
            ?.sortedByDescending { it.dateAdded }
            ?: emptyList()
    }

    fun getTrashFiles(): List<AudioItem> {
        val supportedExtensions = listOf("m4a", "wav", "mp3", "mp4", "mkv", "aac", "3gp")
        return trashDir.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in supportedExtensions }
            ?.map { file ->
                mapToFileItem(file)
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

    private fun mapToFileItem(file: File): AudioItem {
        return AudioItem(
            name = file.name,
            path = file.absolutePath,
            duration = 0L, // Duration will be fetched by service during playback
            size = file.length(),
            dateAdded = file.lastModified()
        )
    }

    private fun scanFile(file: File) {
        android.media.MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            null,
            null
        )
    }

    fun hideFile(item: AudioItem): Boolean {
        val target = File(hiddenDir, item.name)
        val success = item.file.renameTo(target)
        if (success) {
            scanFile(item.file)
            scanFile(target)
        }
        return success
    }

    fun restoreFile(item: AudioItem): Boolean {
        val target = File(captureDir, item.name)
        val success = item.file.renameTo(target)
        if (success) {
            scanFile(item.file)
            scanFile(target)
        }
        return success
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
            scanFile(destinationFile)
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
        val success = item.file.renameTo(newFile)
        if (success) {
            scanFile(item.file)
            scanFile(newFile)
        }
        return success
    }

    fun deleteFile(item: AudioItem): Boolean {
        val target = File(trashDir, item.name)
        if (target.exists()) target.delete() // 기존 동일 이름 있으면 삭제
        
        val success = item.file.renameTo(target)
        if (success) {
            scanFile(item.file)
            scanFile(target)
        } else {
            // renameTo 실패 시 복사 후 삭제 시도
            try {
                item.file.copyTo(target, overwrite = true)
                if (item.file.delete()) {
                    scanFile(item.file)
                    scanFile(target)
                    return true
                }
            } catch (e: Exception) {
                android.util.Log.e("AudioRepository", "Delete failed: ${item.path}", e)
            }
        }
        return success
    }

    fun restoreFromTrash(item: AudioItem): Boolean {
        val target = File(captureDir, item.name)
        val success = item.file.renameTo(target)
        if (success) {
            scanFile(item.file)
            scanFile(target)
        }
        return success
    }

    fun permanentlyDelete(item: AudioItem): Boolean {
        val success = item.file.delete()
        if (success) scanFile(item.file)
        return success
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

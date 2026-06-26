package com.kitwlshcom.kdailyutil.data.repository

import android.content.Context
import com.kitwlshcom.kdailyutil.data.model.AudioItem
import java.io.File

class AudioRepository(private val context: Context) {

    companion object {
        /** 목록/스캔/복구에서 인식하는 오디오 확장자 (소문자). */
        val SUPPORTED_AUDIO_EXTENSIONS = listOf("m4a", "wav", "mp3", "mp4", "mkv", "aac", "3gp")
    }

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
        val supportedExtensions = SUPPORTED_AUDIO_EXTENSIONS
        
        if (playlistName == null) {
            // 전체 리스트 (루트 폴더 + imports 폴더 파일들)
            // File API로 먼저 시도 (앱이 생성한 파일들)
            val rootFiles = captureDir.listFiles()?.filter { it.isFile && it.extension.lowercase() in supportedExtensions } ?: emptyList()
            val importedFiles = importsDir.listFiles()?.filter { it.isFile && it.extension.lowercase() in supportedExtensions } ?: emptyList()

            // 앱 전용 폴더(외부 앱별 저장소 + 내부 imports)만 스캔 — 권한 불필요.
            val allFiles = (rootFiles + importedFiles).distinctBy {
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
                // 앱 전용 폴더(캡처/임포트)의 복사본을 우선 사용한다.
                // 옛 공용 저장소 경로는 권한이 없어 stat은 되더라도 재생(read)이 막히므로,
                // 동일 파일명이 앱 전용 폴더에 있으면 그쪽을 재생 대상으로 삼는다.
                val fileName = File(originalPath).name
                val resolvedInCapture = File(captureDir, fileName)
                val resolvedInImports = File(importsDir, fileName)
                val original = File(originalPath)

                val file = when {
                    resolvedInCapture.exists() -> resolvedInCapture
                    resolvedInImports.exists() -> resolvedInImports
                    original.exists() -> original
                    else -> null
                }

                file?.let { mapToFileItem(it) }
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
        val supportedExtensions = SUPPORTED_AUDIO_EXTENSIONS
        return hiddenDir.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in supportedExtensions }
            ?.map { file ->
                mapToFileItem(file)
            }
            ?.sortedByDescending { it.dateAdded }
            ?: emptyList()
    }

    fun getTrashFiles(): List<AudioItem> {
        val supportedExtensions = SUPPORTED_AUDIO_EXTENSIONS
        return trashDir.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in supportedExtensions }
            ?.map { file ->
                mapToFileItem(file)
            }
            ?.sortedByDescending { it.dateAdded }
            ?: emptyList()
    }

    // 기기 전체 MediaStore 스캔(findFileGlobally/queryMediaStoreFiles)은 제거됨.
    // Google Play 사진·동영상 권한 정책 준수를 위해 READ_MEDIA_* 권한을 더 이상 사용하지 않음.
    // 녹음/가져온 파일은 앱 전용 폴더(captureDir/importsDir)에서만 관리한다.

    /**
     * SAF(Storage Access Framework)로 사용자가 직접 선택한 폴더(트리)에서
     * 오디오 파일을 앱 전용 폴더(captureDir)로 복사해 복구한다.
     *
     * 옛 버전(v1.0)이 공용 Download/KDailyUtil 폴더에 저장한 녹음을, READ_MEDIA_* 권한 없이
     * (사용자가 부여한 폴더 URI 권한만으로) 되살리기 위한 경로. 하위 폴더까지 재귀 순회한다.
     *
     * @return 새로 복사된 파일 수
     */
    fun recoverFromTreeUri(treeUri: android.net.Uri): Int {
        val supportedExtensions = SUPPORTED_AUDIO_EXTENSIONS
        val root = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri) ?: return 0
        var count = 0
        val stack = ArrayDeque<androidx.documentfile.provider.DocumentFile>()
        stack.addLast(root)
        while (stack.isNotEmpty()) {
            val dir = stack.removeLast()
            for (child in dir.listFiles()) {
                if (child.isDirectory) {
                    stack.addLast(child)
                } else if (child.isFile) {
                    val name = child.name ?: continue
                    if (name.substringAfterLast('.', "").lowercase() !in supportedExtensions) continue
                    if (copyDocumentToCapture(child, name)) count++
                }
            }
        }
        return count
    }

    private fun copyDocumentToCapture(doc: androidx.documentfile.provider.DocumentFile, name: String): Boolean {
        var target = File(captureDir, name)
        // 동일 이름+크기 파일이 이미 있으면 중복 복구로 보고 스킵.
        if (target.exists() && target.length() == doc.length()) return false
        if (target.exists()) {
            val base = name.substringBeforeLast(".")
            val ext = name.substringAfterLast(".", "")
            target = File(captureDir, "${base}_${doc.lastModified()}.$ext")
            if (target.exists()) return false
        }
        return try {
            var copied = false
            context.contentResolver.openInputStream(doc.uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
                copied = true
            }
            if (copied) {
                scanFile(target)
            } else if (target.exists()) {
                target.delete() // 스트림 열기 실패로 빈 파일이 생겼으면 정리
            }
            copied
        } catch (e: Exception) {
            android.util.Log.e("AudioRepository", "Recover copy failed: $name", e)
            if (target.exists() && target.length() == 0L) target.delete()
            false
        }
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

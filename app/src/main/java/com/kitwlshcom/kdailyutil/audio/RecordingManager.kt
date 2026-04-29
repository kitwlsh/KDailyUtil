package com.kitwlshcom.kdailyutil.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class RecordingManager(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var currentOutputFile: File? = null

    enum class RecordType(val folderName: String) {
        SHADOWING("뉴스_쉐도잉"),
        AI_COMMAND("AI_Commands")
    }

    /**
     * 녹음을 시작합니다.
     */
    fun startRecording(fileName: String, type: RecordType = RecordType.SHADOWING) {
        try {
            val rootDir = File(
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                "KDailyUtil"
            )
            if (!rootDir.exists()) rootDir.mkdirs()

            var externalDir = File(rootDir, type.folderName)
            if (!externalDir.exists()) {
                val created = externalDir.mkdirs()
                if (!created && !externalDir.exists()) {
                    android.util.Log.e("RecordingManager", "Failed to create subfolder, falling back to root: ${externalDir.absolutePath}")
                    externalDir = rootDir
                }
            }

            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
            val safeFileName = fileName.replace(Regex("[^a-zA-Z0-9ㄱ-ㅎㅏ-ㅣ가-힣]"), "_")
            val truncatedName = if (safeFileName.length > 50) safeFileName.substring(0, 50) else safeFileName
            
            val prefix = when(type) {
                RecordType.SHADOWING -> if (externalDir.name == "KDailyUtil") "Shadowing_Practice_" else "Shadowing_"
                RecordType.AI_COMMAND -> "AI_Command_"
            }
            
            val outputFile = File(externalDir, "${prefix}${timestamp}_${truncatedName}.m4a")
            currentOutputFile = outputFile

            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            android.util.Log.e("RecordingManager", "Error starting recording", e)
            recorder?.release()
            recorder = null
        }
    }

    /**
     * 특정 경로의 오디오나 파일 객체를 재생합니다.
     */
    fun playAudio(path: String? = null, onComplete: () -> Unit = {}) {
        val targetPath = path ?: currentOutputFile?.absolutePath ?: return
        
        try {
            stopPlayback()
            player = MediaPlayer().apply {
                setDataSource(targetPath)
                prepare()
                start()
                setOnCompletionListener {
                    onComplete()
                    releasePlayer()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("RecordingManager", "Error playing audio: $targetPath", e)
        }
    }

    fun stopRecording() {
        try {
            recorder?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    resume() // 이전에 pause 상태였으면 stop 시 예외 발생 방지를 위해 resume 호출 (일부 기기)
                }
                stop()
                release()
            }
        } catch (e: Exception) {
            android.util.Log.e("RecordingManager", "Error stopping recorder", e)
            recorder?.release()
        }
        recorder = null
    }

    fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                recorder?.pause()
            } catch (e: Exception) {
                android.util.Log.e("RecordingManager", "Error pausing recording", e)
            }
        }
    }

    fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                recorder?.resume()
            } catch (e: Exception) {
                android.util.Log.e("RecordingManager", "Error resuming recording", e)
            }
        }
    }

    /**
     * 방금 녹음한 내용을 재생합니다.
     */
    fun playRecordedAudio(onComplete: () -> Unit = {}) {
        playAudio(null, onComplete)
    }

    fun getCurrentRecordingPath(): String? = currentOutputFile?.absolutePath

    fun stopPlayback() {
        player?.stop()
        releasePlayer()
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }
}

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

    /**
     * 녹음을 시작합니다.
     * 이제 폰의 'Download/KDailyUtil' 폴더에 저장되어 오디오 캡처 리스트에서도 보입니다.
     */
    fun startRecording(fileName: String) {
        try {
            val rootDir = File(
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                "KDailyUtil"
            )
            if (!rootDir.exists()) rootDir.mkdirs()

            var externalDir = File(rootDir, "뉴스_쉐도잉")
            if (!externalDir.exists()) {
                val created = externalDir.mkdirs()
                if (!created && !externalDir.exists()) {
                    android.util.Log.e("RecordingManager", "Failed to create subfolder, falling back to root: ${externalDir.absolutePath}")
                    externalDir = rootDir // 폴더 생성 실패 시 루트에라도 저장
                }
            }

            // 파일명에 타임스탬프를 포함하여 유니크하게 생성 (파일 이름 길이 제한 고려)
            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
            val safeFileName = fileName.replace(Regex("[^a-zA-Z0-9ㄱ-ㅎㅏ-ㅣ가-힣]"), "_")
            val truncatedName = if (safeFileName.length > 50) safeFileName.substring(0, 50) else safeFileName
            
            val prefix = if (externalDir.name == "KDailyUtil") "Shadowing_Practice_" else "Shadowing_"
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

    fun stopRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            android.util.Log.e("RecordingManager", "Error stopping recorder", e)
            recorder?.release()
        }
        recorder = null
    }

    /**
     * 방금 녹음한 내용을 재생합니다.
     */
    fun playRecordedAudio(onComplete: () -> Unit = {}) {
        currentOutputFile?.let {
            player = MediaPlayer().apply {
                setDataSource(it.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    onComplete()
                    releasePlayer()
                }
            }
        }
    }

    fun stopPlayback() {
        player?.stop()
        releasePlayer()
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }
}

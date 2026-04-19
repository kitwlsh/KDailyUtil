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
     * 녹음을 시작합니다. 문장 단위로 각기 다른 파일에 저장할 수도 있고, 
     * 한 세션을 통째로 녹음할 수도 있습니다.
     */
    fun startRecording(fileName: String) {
        val outputFile = File(context.cacheDir, "shadowing_$fileName.m4a")
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
    }

    fun stopRecording() {
        recorder?.apply {
            stop()
            release()
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

package com.kitwlshcom.kdailyutil.audio

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.KOREAN
            isInitialized = true
        }
    }

    fun speak(text: String, playBgm: Boolean = true, onComplete: () -> Unit = {}) {
        if (!isInitialized) return

        if (playBgm) {
            startBgm()
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                stopBgm()
                onComplete()
            }
            override fun onError(utteranceId: String?) {
                stopBgm()
            }
        })

        val params = android.os.Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "briefing")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "briefing")
    }

    fun stop() {
        tts?.stop()
        stopBgm()
    }

    private fun startBgm() {
        // 실제 BGM 파일 연동 전까지 로그로 대체하거나 
        // MediaPlayer가 준비되면 아래 주석을 해제합니다.
        // mediaPlayer = MediaPlayer.create(context, ...)
        // mediaPlayer?.isLooping = true
        // mediaPlayer?.start()
    }

    fun stopBgm() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
    }
}

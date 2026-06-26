package com.kitwlshcom.kdailyutil.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitwlshcom.kdailyutil.audio.RecordingManager
import com.kitwlshcom.kdailyutil.audio.TtsManager
import com.kitwlshcom.kdailyutil.domain.util.TextSplitter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ShadowingViewModel(application: Application) : AndroidViewModel(application) {

    private val ttsManager = TtsManager(application)
    private val recordingManager = RecordingManager(application)
    private val textSplitter = TextSplitter()

    private val _currentSentences = MutableStateFlow<List<String>>(emptyList())
    val currentSentences = _currentSentences.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex = _currentIndex.asStateFlow()

    private val _isShadowingActive = MutableStateFlow(false)
    val isShadowingActive = _isShadowingActive.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused = _isPaused.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _isAiSpeaking = MutableStateFlow(false)
    val isAiSpeaking = _isAiSpeaking.asStateFlow()

    private val _currentTitle = MutableStateFlow("")
    val currentTitle = _currentTitle.asStateFlow()

    /**
     * 배움터(속독)에서 사용자가 직접 입력했거나 책/지문을 촬영(OCR)해 얻은 텍스트로 쉐도잉을 구성한다.
     * 저작권 보호: 외부 기사 본문을 스크랩하지 않고, 사용자가 제공한 텍스트만 사용한다.
     * @param body 쉐도잉할 본문, @param title 표시용 제목(미지정 시 본문 앞부분에서 생성)
     */
    fun setText(body: String, title: String? = null) {
        _currentTitle.value = title?.takeIf { it.isNotBlank() }
            ?: body.trim().take(24).replace("\n", " ").ifBlank { "쉐도잉 연습" }

        val sentences = textSplitter.splitIntoSentences(body.trim())
        _currentSentences.value = sentences.ifEmpty { listOf(_currentTitle.value) }
        _currentIndex.value = 0
        stopShadowing()
    }

    fun startShadowing() {
        if (_currentSentences.value.isEmpty()) return
        
        _isShadowingActive.value = true
        _isPaused.value = false
        
        recordingManager.stopRecording()
        val fileName = "${_currentTitle.value}_전체"
        recordingManager.startRecording(fileName)
        recordingManager.pauseRecording() // AI가 먼저 읽어야 하므로 마이크 일시정지
        
        playCurrentSentence()
    }

    private fun playCurrentSentence() {
        if (!_isShadowingActive.value || _isPaused.value) return
        
        val sentences = _currentSentences.value
        if (_currentIndex.value >= sentences.size) {
            stopShadowing()
            return
        }
        val sentence = sentences[_currentIndex.value]

        // 1. 사용자 녹음 일시정지 및 AI 읽기 상태
        recordingManager.pauseRecording()
        _isRecording.value = false
        _isAiSpeaking.value = true

        // 2. TTS 낭독 시작
        ttsManager.speak(sentence, playBgm = false) {
            viewModelScope.launch {
                // 완료되었을 때 여전히 활성화 상태이고 일시정지가 아니면 사용자 녹음 모드로 전환
                if (_isShadowingActive.value && !_isPaused.value) {
                    _isAiSpeaking.value = false
                    _isRecording.value = true
                    recordingManager.resumeRecording()
                } else {
                    _isAiSpeaking.value = false
                }
            }
        }
    }

    fun pauseShadowing() {
        _isPaused.value = true
        _isAiSpeaking.value = false
        ttsManager.stop()
        recordingManager.pauseRecording()
        _isRecording.value = false
    }

    fun resumeShadowing() {
        _isPaused.value = false
        // 현재 문장부터 다시 재생
        playCurrentSentence()
    }

    fun skipToNext() {
        if (_currentIndex.value < _currentSentences.value.size - 1) {
            _currentIndex.value++
            if (_isShadowingActive.value) {
                playCurrentSentence()
            }
        } else {
            stopShadowing()
        }
    }

    fun skipToPrevious() {
        if (_currentIndex.value > 0) {
            _currentIndex.value--
            if (_isShadowingActive.value) {
                playCurrentSentence()
            }
        }
    }

    fun stopShadowing() {
        _isShadowingActive.value = false
        _isPaused.value = false
        _isRecording.value = false
        _isAiSpeaking.value = false
        ttsManager.stop()
        recordingManager.stopRecording()
    }

    fun playLastRecording() {
        recordingManager.playRecordedAudio()
    }

    override fun onCleared() {
        super.onCleared()
        stopShadowing()
        ttsManager.shutdown()
    }
}

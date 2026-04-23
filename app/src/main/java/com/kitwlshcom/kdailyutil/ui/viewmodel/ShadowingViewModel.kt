package com.kitwlshcom.kdailyutil.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitwlshcom.kdailyutil.audio.RecordingManager
import com.kitwlshcom.kdailyutil.audio.TtsManager
import com.kitwlshcom.kdailyutil.data.model.NewsItem
import com.kitwlshcom.kdailyutil.data.repository.NewsRepository
import com.kitwlshcom.kdailyutil.domain.util.TextSplitter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ShadowingViewModel(application: Application) : AndroidViewModel(application) {

    private val newsRepository = NewsRepository(application)
    private val ttsManager = TtsManager(application)
    private val recordingManager = RecordingManager(application)
    private val textSplitter = TextSplitter()

    private val _editorials = MutableStateFlow<List<NewsItem>>(emptyList())
    val editorials = _editorials.asStateFlow()

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

    private val _currentTitle = MutableStateFlow("")
    val currentTitle = _currentTitle.asStateFlow()

    private var shadowingJob: Job? = null

    fun loadEditorials() {
        viewModelScope.launch {
            val list = newsRepository.getEditorials()
            _editorials.value = list
            // 현재 선택된 기사가 없는 경우에만 첫 번째 기사 자동 선택
            if (list.isNotEmpty() && _currentTitle.value.isBlank()) {
                selectArticle(list[0])
            }
        }
    }

    fun selectArticle(item: NewsItem) {
        _currentTitle.value = item.title
        
        // 1. 현재 사용 가능한 텍스트(전문 우선, 없으면 요약)로 즉시 화면 구성
        val initialContent = item.fullContent.ifBlank { item.description }
        val initialSentences = mutableListOf(item.title)
        initialSentences.addAll(textSplitter.splitIntoSentences(initialContent))
        
        _currentSentences.value = initialSentences
        _currentIndex.value = 0
        stopShadowing()

        // 2. 만약 전문(fullContent)이 없다면 백그라운드에서 가져와서 업데이트
        if (item.fullContent.isBlank()) {
            viewModelScope.launch {
                val fullText = newsRepository.fetchFullContent(item)
                if (fullText.isNotBlank() && fullText != item.description) {
                    item.fullContent = fullText
                    
                    // 현재 보기가 아직 이 기사인 경우에만 문장 리스트 업데이트
                    if (_currentTitle.value == item.title) {
                        val updatedSentences = mutableListOf(item.title)
                        updatedSentences.addAll(textSplitter.splitIntoSentences(fullText))
                        _currentSentences.value = updatedSentences
                    }
                }
            }
        }
    }

    fun startShadowing() {
        if (_currentSentences.value.isEmpty()) return
        
        _isShadowingActive.value = true
        _isPaused.value = false
        
        shadowingJob?.cancel()
        shadowingJob = viewModelScope.launch {
            while (_currentIndex.value < _currentSentences.value.size && _isShadowingActive.value) {
                if (_isPaused.value) {
                    delay(500)
                    continue
                }

                // 안전한 인덱스 체크
                val sentences = _currentSentences.value
                if (_currentIndex.value >= sentences.size) break
                val sentence = sentences[_currentIndex.value]
                
                // 1. TTS로 문장 읽기
                ttsManager.speak(sentence, playBgm = false)
                
                // 음성 재생 대기 (단축된 체크 루프)
                val speakDuration = (sentence.length * 200L).coerceAtLeast(1500L)
                waitForPeriod(speakDuration)
                if (!_isShadowingActive.value || _isPaused.value) continue

                // 2. 대기 시간 시작 및 녹음 시작
                val waitTime = textSplitter.calculateWaitTime(sentence)
                _isRecording.value = true
                val fileName = "${_currentTitle.value}_문장${_currentIndex.value + 1}"
                
                // 녹음 시작 시 예외가 발생하더라도 루프가 멈추지 않게 함
                try {
                    recordingManager.startRecording(fileName)
                } catch (e: Exception) {
                    android.util.Log.e("ShadowingViewModel", "Failed to start recording for $fileName", e)
                }
                
                waitForPeriod(waitTime)
                
                // 3. 녹음 중단
                recordingManager.stopRecording()
                _isRecording.value = false
                
                if (_isShadowingActive.value && !_isPaused.value) {
                    _currentIndex.value++
                }
            }
            if (_currentIndex.value >= _currentSentences.value.size) {
                _isShadowingActive.value = false
            }
        }
    }

    private suspend fun waitForPeriod(duration: Long) {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < duration) {
            if (!_isShadowingActive.value || _isPaused.value) break
            delay(100)
        }
    }

    fun pauseShadowing() {
        _isPaused.value = true
        ttsManager.stop()
        recordingManager.stopRecording()
        _isRecording.value = false
    }

    fun resumeShadowing() {
        _isPaused.value = false
        // 현재 문장부터 다시 시작
        startShadowing()
    }

    fun skipToNext() {
        if (_currentIndex.value < _currentSentences.value.size - 1) {
            _currentIndex.value++
            if (_isShadowingActive.value) startShadowing()
        }
    }

    fun skipToPrevious() {
        if (_currentIndex.value > 0) {
            _currentIndex.value--
            if (_isShadowingActive.value) startShadowing()
        }
    }

    fun stopShadowing() {
        _isShadowingActive.value = false
        _isPaused.value = false
        _isRecording.value = false
        shadowingJob?.cancel()
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

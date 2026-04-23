package com.kitwlshcom.kdailyutil.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitwlshcom.kdailyutil.audio.RecordingManager
import com.kitwlshcom.kdailyutil.audio.TtsManager
import com.kitwlshcom.kdailyutil.data.model.NewsItem
import com.kitwlshcom.kdailyutil.data.remote.GeminiManager
import com.kitwlshcom.kdailyutil.data.repository.NewsRepository
import com.kitwlshcom.kdailyutil.data.repository.SettingsRepository
import com.kitwlshcom.kdailyutil.domain.util.SttManager
import com.kitwlshcom.kdailyutil.scheduler.BriefingScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BriefingViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val newsRepository = NewsRepository(application)
    private val scheduler = BriefingScheduler(application)
    private val ttsManager = TtsManager(application)
    private val recordingManager = RecordingManager(application)
    private val sttManager = SttManager(application)

    companion object {
        private const val TAG = "BriefingViewModel"
    }

    val keywords = settingsRepository.keywordsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptySet())
    val briefingTime = settingsRepository.briefingTimeFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), Pair(7, 0))
    val isBriefingEnabled = settingsRepository.isBriefingEnabledFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)
    val geminiApiKey = settingsRepository.geminiApiKeyFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
    
    val aiBriefingCommand = settingsRepository.aiBriefingCommandFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")
    val aiCommandAudioPath = settingsRepository.aiCommandAudioPathFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")

    val categories = combine(
        settingsRepository.categoriesFlow,
        aiBriefingCommand
    ) { cats, command ->
        if (command.isNotBlank()) cats + "AI" else cats
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), setOf("전체"))

    private val _selectedCategory = MutableStateFlow("전체")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedNewsItem = MutableStateFlow<NewsItem?>(null)
    val selectedNewsItem: StateFlow<NewsItem?> = _selectedNewsItem.asStateFlow()

    private val _newsItems = MutableStateFlow<List<NewsItem>>(emptyList())
    val newsItems: StateFlow<List<NewsItem>> = _newsItems.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isBriefingPlaying = MutableStateFlow(false)
    val isBriefingPlaying: StateFlow<Boolean> = _isBriefingPlaying.asStateFlow()

    private val _isRecordingCommand = MutableStateFlow(false)
    val isRecordingCommand: StateFlow<Boolean> = _isRecordingCommand.asStateFlow()

    private val _isAiAnalysisLoading = MutableStateFlow(false)
    val isAiAnalysisLoading: StateFlow<Boolean> = _isAiAnalysisLoading.asStateFlow()

    // STT 실시간 피드백 및 타이핑 최적화용
    private val _sttPartialText = MutableStateFlow("")
    val sttPartialText: StateFlow<String> = _sttPartialText.asStateFlow()

    private val _isLoadingDetail = MutableStateFlow(false)
    val isLoadingDetail: StateFlow<Boolean> = _isLoadingDetail.asStateFlow()

    fun updateKeywords(newKeywords: Set<String>) {
        viewModelScope.launch { settingsRepository.updateKeywords(newKeywords) }
    }

    fun updateBriefingTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.updateBriefingTime(hour, minute)
            if (isBriefingEnabled.value) {
                scheduler.scheduleBriefing(hour, minute)
            }
        }
    }

    fun toggleBriefing(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBriefingEnabled(enabled)
            if (enabled) {
                scheduler.scheduleBriefing(briefingTime.value.first, briefingTime.value.second)
            } else {
                scheduler.cancelBriefing()
            }
        }
    }

    fun updateApiKey(key: String) {
        viewModelScope.launch { settingsRepository.updateGeminiApiKey(key) }
    }

    fun updateCategories(newCategories: Set<String>) {
        viewModelScope.launch { settingsRepository.updateCategories(newCategories) }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        fetchNews()
    }

    fun setSelectedNewsItem(item: NewsItem?) {
        _selectedNewsItem.value = item
    }

    fun fetchNews() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val currentCategory = _selectedCategory.value
            
            if (currentCategory == "AI") {
                generateAiCustomBriefing()
            } else {
                val allNews = if (currentCategory == "전체") {
                    val topNews = newsRepository.getTopNews(10)
                    val keywordNews = newsRepository.getAllNews(keywords.value)
                    (topNews + keywordNews).distinctBy { it.link }
                } else {
                    newsRepository.getNewsByKeyword(currentCategory, 20)
                }
                _newsItems.value = allNews
            }
            _isRefreshing.value = false
        }
    }

    private suspend fun generateAiCustomBriefing() {
        val command = aiBriefingCommand.value
        val apiKey = geminiApiKey.value
        
        if (command.isBlank() || apiKey.isNullOrBlank()) {
            _newsItems.value = listOf(
                NewsItem(
                    title = "AI 브리핑 안내",
                    link = "",
                    description = "설정에서 명령어를 등록하고 API 키를 확인해 주세요.",
                    pubDate = "-",
                    source = "System"
                )
            )
            return
        }

        _isAiAnalysisLoading.value = true
        try {
            // 분석을 위해 경제/종합 뉴스 20개 정도를 수집
            val referenceNews = newsRepository.getTopNews(20)
            val gemini = GeminiManager(apiKey)
            val analysis = gemini.processAiCustomBriefing(command, referenceNews)
            
            _newsItems.value = listOf(
                NewsItem(
                    title = "✨ AI 맞춤 분석: $command",
                    link = "ai_analysis",
                    description = analysis,
                    pubDate = "현재",
                    source = "Gemini AI",
                    fullContent = analysis
                )
            )
        } catch (e: Exception) {
            _newsItems.value = listOf(
                NewsItem(
                    title = "분석 오류",
                    link = "error",
                    description = "AI 분석 중 오류가 발생했습니다: ${e.message}",
                    pubDate = "-",
                    source = "Error"
                )
            )
        } finally {
            _isAiAnalysisLoading.value = false
        }
    }

    fun startCommandRecording() {
        viewModelScope.launch {
            _isRecordingCommand.value = true
            _sttPartialText.value = ""
            recordingManager.startRecording("AI_Request", RecordingManager.RecordType.AI_COMMAND)
            sttManager.startListening(
                onResult = { text ->
                    _sttPartialText.value = text
                    updateAiCommand(text)
                    stopCommandRecording()
                },
                onError = { _ -> stopCommandRecording() },
                onPartialResult = { partial ->
                    _sttPartialText.value = partial
                }
            )
        }
    }

    fun stopCommandRecording() {
        _isRecordingCommand.value = false
        recordingManager.stopRecording()
        sttManager.stopListening()
        
        // 최종 텍스트가 있다면 저장
        if (_sttPartialText.value.isNotBlank()) {
            updateAiCommand(_sttPartialText.value)
        }
        
        // 오디오 경로 저장
        val path = recordingManager.getCurrentRecordingPath() ?: ""
        viewModelScope.launch { settingsRepository.updateAiCommandAudioPath(path) }
    }

    fun playCommandAudio() {
        val path = aiCommandAudioPath.value
        if (path.isNotBlank()) {
            recordingManager.playAudio(path)
        }
    }

    fun updateAiCommand(command: String) {
        viewModelScope.launch { settingsRepository.updateAiBriefingCommand(command) }
    }

    fun loadFullContent(item: NewsItem) {
        viewModelScope.launch {
            _isLoadingDetail.value = true
            
            // 1. 일반 추출 시도
            var fullText = newsRepository.fetchFullContent(item)
            var isRawDump = fullText.startsWith("RAW_DUMP:")
            if (isRawDump) {
                fullText = fullText.removePrefix("RAW_DUMP:")
                Log.d(TAG, "🔍 Standard extraction failed, using raw dump for AI analysis.")
            }
            
            // 2. AI 추출 (조건 만족 시: 덤프 데이터거나, 텍스트가 너무 짧거나, 문단 구분이 없는 경우)
            val apiKey = geminiApiKey.value
            val needsAi = isRawDump || fullText.length < 500 || fullText.count { it == '\n' } < 2
            
            if (!apiKey.isNullOrBlank() && needsAi) {
                Log.d(TAG, "🤖 Requesting Gemini AI to extract content for: ${item.title}")
                val gemini = GeminiManager(apiKey)
                // 덤프 데이터일 경우 더 명확하게 요청
                val promptPrefix = if (isRawDump) "[전체 텍스트 덤프 분석] " else ""
                val aiExtracted = gemini.extractArticleContent("$promptPrefix$fullText".take(10000))
                
                if (aiExtracted.isNotBlank() && aiExtracted.length > 50) {
                    Log.d(TAG, "✨ Gemini AI extraction successful! (Length: ${aiExtracted.length})")
                    fullText = aiExtracted
                } else {
                    Log.w(TAG, "❌ Gemini AI failed to extract content.")
                    if (isRawDump) fullText = "" // 덤프 데이터를 그대로 보여주지 않음
                }
            }

            if (fullText.isNotBlank() && (fullText.length > item.fullContent.length || item.fullContent.isBlank())) {
                item.fullContent = fullText
                _selectedNewsItem.value = item.copy()
                _newsItems.value = _newsItems.value.map { if (it.link == item.link) item.copy() else it }
            }
            _isLoadingDetail.value = false
        }
    }

    fun startLiveBriefing() {
        if (_isBriefingPlaying.value) {
            stopBriefing()
            return
        }

        viewModelScope.launch {
            _isBriefingPlaying.value = true
            val briefingText = StringBuilder("오늘의 주요 뉴스 브리핑을 시작합니다.\n\n")
            newsItems.value.forEachIndexed { index, item ->
                briefingText.append("${index + 1}번 뉴스, ${item.title}입니다.\n")
                val content = item.fullContent.ifBlank { item.summary.ifBlank { item.description } }
                briefingText.append("$content\n\n")
            }
            briefingText.append("이상으로 오늘의 뉴스를 모두 마치겠습니다. 감사합니다.")
            ttsManager.speak(briefingText.toString()) {
                _isBriefingPlaying.value = false
            }
        }
    }

    fun startSingleNewsBriefing(item: NewsItem) {
        stopBriefing()
        viewModelScope.launch {
            _isBriefingPlaying.value = true
            if (item.fullContent.isBlank()) {
                item.fullContent = newsRepository.fetchFullContent(item)
                _selectedNewsItem.value = item.copy()
            }
            val content = item.fullContent.ifBlank { item.summary.ifBlank { item.description } }
            val textToSpeak = "${item.title}. $content"
            ttsManager.speak(textToSpeak) {
                _isBriefingPlaying.value = false
            }
        }
    }

    fun stopBriefing() {
        ttsManager.stop()
        _isBriefingPlaying.value = false
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
        recordingManager.stopPlayback()
        sttManager.destroy()
    }
}

package com.kitwlshcom.kdailyutil.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitwlshcom.kdailyutil.audio.TtsManager
import com.kitwlshcom.kdailyutil.data.model.NewsItem
import com.kitwlshcom.kdailyutil.data.remote.GeminiManager
import com.kitwlshcom.kdailyutil.data.repository.NewsRepository
import com.kitwlshcom.kdailyutil.data.repository.SettingsRepository
import com.kitwlshcom.kdailyutil.scheduler.BriefingScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BriefingViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val newsRepository = NewsRepository()
    private val scheduler = BriefingScheduler(application)
    private val ttsManager = TtsManager(application)

    val keywords = settingsRepository.keywordsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptySet())
    val briefingTime = settingsRepository.briefingTimeFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), Pair(7, 0))
    val isBriefingEnabled = settingsRepository.isBriefingEnabledFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)
    val geminiApiKey = settingsRepository.geminiApiKeyFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    private val _newsItems = MutableStateFlow<List<NewsItem>>(emptyList())
    val newsItems: StateFlow<List<NewsItem>> = _newsItems.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isBriefingPlaying = MutableStateFlow(false)
    val isBriefingPlaying: StateFlow<Boolean> = _isBriefingPlaying.asStateFlow()

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

    fun fetchNews() {
        viewModelScope.launch {
            _isRefreshing.value = true
            
            // 1. 주요 뉴스 가져오기
            val topNews = newsRepository.getTopNews(5)
            
            // 2. 키워드 기반 뉴스 가져오기
            val keywordNews = newsRepository.getAllNews(keywords.value)
            
            // 3. 중복 제거 및 합치기 (링크 기준)
            val allNews = (topNews + keywordNews).distinctBy { it.link }
            
            // 4. 각 뉴스 전문 미리 가져오기 (비동기 병렬 처리)
            allNews.forEach { item ->
                if (item.fullContent.isBlank()) {
                    item.fullContent = newsRepository.fetchFullContent(item)
                }
            }
            
            _newsItems.value = allNews
            _isRefreshing.value = false
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
                // 전문이 있으면 전문을, 없으면 요약을, 둘 다 없으면 설명을 사용
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
            
            // 전문이 아직 없으면 가져옴
            if (item.fullContent.isBlank()) {
                item.fullContent = newsRepository.fetchFullContent(item)
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
    }
}

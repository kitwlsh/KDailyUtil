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
            _newsItems.value = newsRepository.getAllNews(keywords.value)
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
            val gemini = GeminiManager(geminiApiKey.value)
            val summary = gemini.summarizeNews(newsItems.value)
            ttsManager.speak(summary) {
                _isBriefingPlaying.value = false
            }
        }
    }

    fun startSingleNewsBriefing(item: NewsItem) {
        stopBriefing()
        viewModelScope.launch {
            _isBriefingPlaying.value = true
            val gemini = GeminiManager(geminiApiKey.value)
            val summary = gemini.summarizeNews(listOf(item)) // 한 건만 요약
            ttsManager.speak(summary) {
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

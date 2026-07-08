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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val dartApiKey = settingsRepository.dartApiKeyFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), com.kitwlshcom.kdailyutil.BuildConfig.DART_DEFAULT_KEY)
    
    val aiBriefingCommand = settingsRepository.aiBriefingCommandFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")
    val aiBriefingCommands = settingsRepository.aiBriefingCommandsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptySet())
    val stockKeywords = settingsRepository.stockKeywordsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptySet())
    // 📈 증시 대시보드 관심종목(시세·차트/실적 뉴스·전망). 뉴스탭 증시 필터(stockKeywords)와 별개 저장소.
    val watchStockKeywords = settingsRepository.watchStockKeywordsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptySet())
    val aiCommandAudioPath = settingsRepository.aiCommandAudioPathFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")
    val isApiKeyValidated = settingsRepository.isApiKeyValidatedFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)
    val autoRefreshIntervalHours = settingsRepository.autoRefreshIntervalFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 2)
    val newsLimit = settingsRepository.newsLimitFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 20)
    val splashTheme = settingsRepository.splashThemeFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "shimmer")

    val categories = settingsRepository.categoriesFlow.map { cats ->
        val fixed = listOf("전체", "증시", "AI")
        val userCats = cats.filter { it !in fixed }
        (fixed + userCats).toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), setOf("전체", "증시", "AI"))

    private val _selectedAiCommand = MutableStateFlow<String?>(null)
    val selectedAiCommand: StateFlow<String?> = _selectedAiCommand.asStateFlow()

    private val _selectedStockKeyword = MutableStateFlow<String?>(null)
    val selectedStockKeyword: StateFlow<String?> = _selectedStockKeyword.asStateFlow()

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

    private val _isBriefingPaused = MutableStateFlow(false)
    val isBriefingPaused: StateFlow<Boolean> = _isBriefingPaused.asStateFlow()

    private val _isRecordingCommand = MutableStateFlow(false)
    val isRecordingCommand: StateFlow<Boolean> = _isRecordingCommand.asStateFlow()

    private val _isAiAnalysisLoading = MutableStateFlow(false)
    val isAiAnalysisLoading: StateFlow<Boolean> = _isAiAnalysisLoading.asStateFlow()

    // STT 실시간 피드백 및 타이핑 최적화용
    private val _sttPartialText = MutableStateFlow("")
    val sttPartialText: StateFlow<String> = _sttPartialText.asStateFlow()
    
    private val _apiKeyStatus = MutableStateFlow<ApiKeyStatus>(ApiKeyStatus.Idle)
    val apiKeyStatus: StateFlow<ApiKeyStatus> = _apiKeyStatus.asStateFlow()

    init {
        // 앱 시작 시 API 키 검증 상태 복구
        viewModelScope.launch {
            combine(geminiApiKey, isApiKeyValidated) { key, isValidated ->
                Pair(key, isValidated)
            }.collect { (key, isValidated) ->
                if (!key.isNullOrBlank() && isValidated) {
                    _apiKeyStatus.value = ApiKeyStatus.Valid("✅ API 키가 유효합니다!")
                } else if (_apiKeyStatus.value is ApiKeyStatus.Valid) {
                    _apiKeyStatus.value = ApiKeyStatus.Idle
                }
            }
        }
    }

    private val _isLoadingDetail = MutableStateFlow(false)
    val isLoadingDetail: StateFlow<Boolean> = _isLoadingDetail.asStateFlow()

    private val aiAnalysisCache = mutableMapOf<String, NewsItem>()
    private val generalNewsCache = java.util.concurrent.ConcurrentHashMap<String, List<NewsItem>>()

    fun updateAutoRefreshInterval(hours: Int) {
        viewModelScope.launch { settingsRepository.updateAutoRefreshInterval(hours) }
    }

    fun updateNewsLimit(limit: Int) {
        viewModelScope.launch { settingsRepository.updateNewsLimit(limit) }
    }

    fun updateSplashTheme(theme: String) {
        viewModelScope.launch { settingsRepository.updateSplashTheme(theme) }
    }

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
        viewModelScope.launch {
            val trimmedKey = key.trim()
            settingsRepository.updateGeminiApiKey(trimmedKey)
            settingsRepository.setApiKeyValidated(false)
            _apiKeyStatus.value = ApiKeyStatus.Idle // 키 변경 시 상태 초기화
        }
    }

    fun updateDartApiKey(key: String) {
        viewModelScope.launch {
            settingsRepository.updateDartApiKey(key.trim())
        }
    }

    fun validateApiKey() {
        val key = geminiApiKey.value
        if (key.isNullOrBlank()) {
            _apiKeyStatus.value = ApiKeyStatus.Invalid("API 키를 먼저 입력해 주세요.")
            return
        }

        viewModelScope.launch(Dispatchers.Main) {
            _apiKeyStatus.value = ApiKeyStatus.Validating
            try {
                // 사용자 제안 코드 적용: "Say Hello" 테스트
                val gemini = GeminiManager(key)
                val response = withContext(Dispatchers.IO) {
                    gemini.processAiCustomBriefing("Say 'Hello' briefly.", emptyList<NewsItem>())
                }
                
                if (response.isNotBlank() && !response.contains("오류")) {
                    settingsRepository.setApiKeyValidated(true)
                    _apiKeyStatus.value = ApiKeyStatus.Valid("✅ API 키가 유효합니다! 응답: $response")
                } else {
                    _apiKeyStatus.value = ApiKeyStatus.Invalid("⚠️ 응답이 비어있거나 올바르지 않습니다.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ API Key Validation Fail: ${e.message}", e)
                _apiKeyStatus.value = ApiKeyStatus.Invalid("❌ 문제가 있습니다: ${e.message}")
            }
        }
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

    fun selectAiCommand(command: String?) {
        _selectedAiCommand.value = command
        if (_selectedCategory.value == "AI") {
            fetchNews()
        }
    }

    fun selectStockKeyword(keyword: String?) {
        _selectedStockKeyword.value = keyword
        if (_selectedCategory.value == "증시") {
            fetchNews()
        }
    }

    fun fetchNews(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val currentCategory = _selectedCategory.value
            
            // 1. AI 카테고리 특수 처리
            if (currentCategory == "AI") {
                _isRefreshing.value = true
                try {
                    generateAiCustomBriefing(forceRefresh)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error fetching AI briefing: ${e.message}", e)
                } finally {
                    _isRefreshing.value = false
                }
                return@launch
            }

            // 2. 일반 뉴스 카테고리 캐시 키 계산
            val cacheKey = if (currentCategory == "증시") {
                val targetKeyword = selectedStockKeyword.value ?: stockKeywords.value.firstOrNull() ?: "증시"
                "증시_$targetKeyword"
            } else {
                currentCategory
            }

            // 3. 캐시가 존재하고 강제 새로고침이 아니라면 즉시 데이터 반환
            if (!forceRefresh) {
                // 3-1. RAM 캐시에 있는 경우
                if (generalNewsCache.containsKey(cacheKey)) {
                    _newsItems.value = generalNewsCache[cacheKey] ?: emptyList()
                    Log.d(TAG, "✅ Loaded $cacheKey news from RAM cache")
                    return@launch
                }
                // 3-2. 로컬 파일 영구 캐시에 있는 경우 로드하여 복원
                val persistentCached = newsRepository.loadCachedNews(cacheKey)
                if (persistentCached.isNotEmpty()) {
                    val lastModified = newsRepository.getCacheLastModified(cacheKey)
                    val intervalHours = autoRefreshIntervalHours.value
                    
                    // intervalHours가 0이면 자동 새로고침 비활성화(안 함)
                    var isExpired = if (intervalHours > 0 && lastModified > 0L) {
                        val diffMillis = System.currentTimeMillis() - lastModified
                        val diffHours = diffMillis.toDouble() / (1000 * 60 * 60)
                        diffHours >= intervalHours
                    } else {
                        false
                    }

                    // 예약 브리핑 기준 추가 만료 판정
                    if (!isExpired && isBriefingEnabled.value && lastModified > 0L) {
                        val (bHour, bMinute) = briefingTime.value
                        val now = java.util.Calendar.getInstance()
                        
                        // 오늘 설정된 예약 시각 Calendar 객체 생성
                        val scheduledToday = java.util.Calendar.getInstance().apply {
                            set(java.util.Calendar.HOUR_OF_DAY, bHour)
                            set(java.util.Calendar.MINUTE, bMinute)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }
                        
                        // 가장 최근에 지나간 예약 시각 결정
                        val mostRecentSchedule = if (now.before(scheduledToday)) {
                            // 현재 시각이 오늘 예약 시각보다 이전이라면, 가장 최근 예약 시각은 '어제' 예약 시각
                            (scheduledToday.clone() as java.util.Calendar).apply {
                                add(java.util.Calendar.DATE, -1)
                            }
                        } else {
                            // 현재 시각이 오늘 예약 시각을 지났다면, 가장 최근 예약 시각은 '오늘' 예약 시각
                            scheduledToday
                        }
                        
                        // 마지막 캐시 수정 시각이 가장 최근 예약 시각 이전이라면 만료로 판단
                        if (lastModified < mostRecentSchedule.timeInMillis) {
                            Log.d(TAG, "⏰ Cache for $cacheKey is older than the most recent briefing schedule. Expiring cache to refresh.")
                            isExpired = true
                        }
                    }

                    if (isExpired) {
                        Log.d(TAG, "⏰ Cache for $cacheKey is expired (interval or schedule). Automatically refreshing...")
                        // 캐시가 만료되었으므로 아래 실제 크롤링으로 넘어가도록 계속 진행
                    } else {
                        generalNewsCache[cacheKey] = persistentCached
                        _newsItems.value = persistentCached
                        Log.d(TAG, "✅ Loaded $cacheKey news from persistent file cache (Valid cache)")
                        return@launch
                    }
                }
            }

            // 4. 캐시가 없거나 강제 새로고침 시 실제 크롤링 수행
            _isRefreshing.value = true
            try {
                val limitCount = newsLimit.value
                val allNews = if (currentCategory == "증시") {
                    val targetKeyword = selectedStockKeyword.value ?: stockKeywords.value.firstOrNull() ?: "증시"
                    newsRepository.getNewsByKeyword(targetKeyword, limitCount)
                } else if (currentCategory == "전체") {
                    val topNewsLimit = (limitCount / 2).coerceAtLeast(10)
                    val topNews = newsRepository.getTopNews(topNewsLimit)
                    val keywordNews = newsRepository.getAllNews(keywords.value)
                    (topNews + keywordNews).distinctBy { it.link }
                } else {
                    newsRepository.getNewsByKeyword(currentCategory, limitCount)
                }
                
                _newsItems.value = allNews
                // 캐시 업데이트
                if (allNews.isNotEmpty()) {
                    generalNewsCache[cacheKey] = allNews
                    // 파일 캐시도 영구 저장
                    newsRepository.saveCachedNews(cacheKey, allNews)
                }
                Log.d(TAG, "🌐 Successfully fetched fresh $cacheKey news")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error fetching news: ${e.message}", e)
                _newsItems.value = listOf(
                    NewsItem("뉴스 로드 오류", "", "뉴스를 불러오는 중 오류가 발생했습니다: ${e.message}", "-", "Error")
                )
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun generateAiCustomBriefing(forceRefresh: Boolean) {
        val commands = aiBriefingCommands.value
        val fallbackCommand = aiBriefingCommand.value
        
        // 여러 개가 있으면 selectedAiCommand 혹은 첫 번째 선택, 없으면 하위 호환용 1개 사용
        val targetCommand = selectedAiCommand.value ?: commands.firstOrNull() ?: fallbackCommand
        
        val apiKey = geminiApiKey.value
        
        if (targetCommand.isBlank() || apiKey.isNullOrBlank()) {
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

        val fileCacheKey = "AI_$targetCommand"

        if (!forceRefresh) {
            // 1. RAM 캐시 우선 체크
            if (aiAnalysisCache.containsKey(targetCommand)) {
                _newsItems.value = listOf(aiAnalysisCache[targetCommand]!!)
                Log.d(TAG, "✅ Loaded AI Analysis from RAM cache")
                return
            }
            // 2. 파일 캐시 체크 및 복원
            val persistentCached = newsRepository.loadCachedNews(fileCacheKey)
            if (persistentCached.isNotEmpty()) {
                val cachedItem = persistentCached.first()
                aiAnalysisCache[targetCommand] = cachedItem
                _newsItems.value = listOf(cachedItem)
                Log.d(TAG, "✅ Loaded AI Analysis from persistent file cache")
                return
            }
        }

        _isAiAnalysisLoading.value = true
        try {
            // 분석을 위해 경제/종합 뉴스 20개 정도를 수집
            // 저작권 보호: 'AI 이용 금지'로 감지된 매체(도메인/스니펫)는 AI 분석 입력에서 제외한다.
            val referenceNews = newsRepository.getTopNews(20).filterNot {
                NewsRepository.detectAiRestrictionNotice(it.description) ||
                    NewsRepository.isAiRestrictedDomain(it.link)
            }
            val gemini = GeminiManager(apiKey)
            val analysis = gemini.processAiCustomBriefing(targetCommand, referenceNews)
            
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            val timeStr = sdf.format(java.util.Date())
            
            val aiNewsItem = NewsItem(
                title = "✨ AI 맞춤 분석: $targetCommand",
                link = "ai_analysis",
                description = analysis,
                pubDate = "분석 시각: $timeStr",
                source = "Gemini AI",
                fullContent = analysis,
                fullContentHtml = "<div>${analysis.replace("\n", "<br>")}</div>"
            )
            
            aiAnalysisCache[targetCommand] = aiNewsItem
            _newsItems.value = listOf(aiNewsItem)
            
            // 파일 캐시로 영구 저장
            newsRepository.saveCachedNews(fileCacheKey, listOf(aiNewsItem))

            Log.d(TAG, "✅ AI Analysis successful")
        } catch (e: Exception) {
            Log.e(TAG, "❌ AI Analysis Error: ${e.message}", e)
            _newsItems.value = listOf(
                NewsItem(
                    title = "분석 오류",
                    link = "error",
                    description = "AI 분석 중 오류가 발생했습니다. API 키가 정확한지, 인터넷 연결이 되어 있는지 확인해 주세요.\n(상세: ${e.message})",
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
            updateAiCommands(aiBriefingCommands.value + _sttPartialText.value)
            _sttPartialText.value = "" // 중복 방지
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

    fun updateAiCommands(commands: Set<String>) {
        viewModelScope.launch {
            settingsRepository.updateAiBriefingCommands(commands)
            // 지워졌을 때 selectedAiCommand 갱신
            if (_selectedAiCommand.value != null && !commands.contains(_selectedAiCommand.value)) {
                _selectedAiCommand.value = commands.firstOrNull()
            }
        }
    }

    // 📈 증시 대시보드 관심종목 갱신(증시탭 시세·차트/실적 뉴스·전망에 반영). 뉴스탭 필터와 별개.
    fun updateWatchStockKeywords(keywords: Set<String>) {
        viewModelScope.launch {
            settingsRepository.updateWatchStockKeywords(keywords)
        }
    }

    fun updateStockKeywords(keywords: Set<String>) {
        viewModelScope.launch {
            settingsRepository.updateStockKeywords(keywords)
            if (_selectedStockKeyword.value != null && !keywords.contains(_selectedStockKeyword.value)) {
                _selectedStockKeyword.value = keywords.firstOrNull()
            }
        }
    }

    fun loadFullContent(item: NewsItem) {
        if (!item.link.startsWith("http")) return // AI 분석 등 웹 링크가 아닌 경우 무시

        viewModelScope.launch {
            _isLoadingDetail.value = true
            // 저작권 보호: 본문 전문을 스크랩하거나 Gemini로 보내지 않는다.
            // 읽기는 WebView 원문으로 제공하므로, 표시용 원본 URL만 해석한다.
            // (브리핑/쉐도잉은 언론사가 신디케이션용으로 배포한 RSS 스니펫만 사용)
            if (item.resolvedUrl.isBlank() || item.resolvedUrl.contains("google.com")) {
                newsRepository.resolveArticleUrl(item)
                _selectedNewsItem.value = item.copy()
                _newsItems.value = _newsItems.value.map { if (it.link == item.link) item.copy() else it }
            }
            _isLoadingDetail.value = false
        }
    }

    private var currentBriefingIndex = -1

    fun startLiveBriefing() {
        if (_isBriefingPlaying.value) {
            stopBriefing()
            return
        }

        _isBriefingPlaying.value = true
        _isBriefingPaused.value = false
        currentBriefingIndex = -1
        playNextBriefingPart()
    }

    /**
     * 브리핑을 일시정지합니다. 현재 읽던 뉴스 항목 위치(currentBriefingIndex)를 유지하여,
     * 재개 시 처음이 아닌 멈춘 항목부터 다시 읽습니다. (Android TTS는 단어 단위 정지를 지원하지 않아 항목 단위로 처리)
     */
    fun pauseBriefing() {
        if (!_isBriefingPlaying.value || _isBriefingPaused.value) return
        ttsManager.stop()
        _isBriefingPaused.value = true
    }

    /** 일시정지된 브리핑을 멈춘 항목부터 다시 재생합니다. */
    fun resumeBriefing() {
        if (!_isBriefingPlaying.value || !_isBriefingPaused.value) return
        _isBriefingPaused.value = false
        playNextBriefingPart()
    }

    private fun playNextBriefingPart() {
        if (!_isBriefingPlaying.value || _isBriefingPaused.value) return

        val items = newsItems.value

        when {
            currentBriefingIndex == -1 -> {
                ttsManager.speak("오늘의 주요 뉴스 브리핑을 시작합니다.") {
                    if (_isBriefingPlaying.value) {
                        currentBriefingIndex++
                        playNextBriefingPart()
                    }
                }
            }
            currentBriefingIndex < items.size -> {
                val item = items[currentBriefingIndex]
                // 저작권 보호: 본문 전문이 아니라 RSS 스니펫(요약)만 낭독.
                // 'AI 이용 금지' 매체는 그조차 생략하고 원문 보기를 안내한다.
                val content = if (item.aiRestricted) "원문 보기로 확인해 주세요."
                              else stripHtml(item.summary.ifBlank { item.description })
                val text = "${currentBriefingIndex + 1}번 뉴스, ${item.title}입니다.\n\n$content"
                
                ttsManager.speak(text) {
                    if (_isBriefingPlaying.value) {
                        currentBriefingIndex++
                        playNextBriefingPart()
                    }
                }
            }
            else -> {
                ttsManager.speak("이상으로 오늘의 뉴스를 모두 마치겠습니다. 감사합니다.") {
                    _isBriefingPlaying.value = false
                }
            }
        }
    }

    fun skipToNextNews() {
        if (!_isBriefingPlaying.value) return
        
        ttsManager.stop()
        
        if (currentBriefingIndex < newsItems.value.size) {
            currentBriefingIndex++
        }
        playNextBriefingPart()
    }

    fun startSingleNewsBriefing(item: NewsItem) {
        stopBriefing()
        viewModelScope.launch {
            _isBriefingPlaying.value = true
            // 'AI 이용 금지' 매체는 낭독하지 않고 제목 + 원문 보기 안내만 읽는다.
            if (item.aiRestricted) {
                ttsManager.speak("${item.title}. 이 매체는 AI 및 음성 낭독 이용을 제한하여, 원문 보기로 안내합니다.") {
                    _isBriefingPlaying.value = false
                }
                return@launch
            }
            // 저작권 보호: 본문 전문이 아니라 언론사가 배포한 RSS 스니펫(요약)만 낭독한다.
            val snippet = stripHtml(item.summary.ifBlank { item.description })
            val textToSpeak = if (snippet.isBlank()) item.title else "${item.title}. $snippet"
            ttsManager.speak(textToSpeak) {
                _isBriefingPlaying.value = false
            }
        }
    }

    /** RSS 스니펫에 섞여 있을 수 있는 HTML 태그를 제거해 낭독용 텍스트로 정리 */
    private fun stripHtml(text: String): String =
        text.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()

    fun stopBriefing() {
        ttsManager.stop()
        _isBriefingPlaying.value = false
        _isBriefingPaused.value = false
    }

    /**
     * 상세 화면 WebView가 원문 페이지에서 'AI 학습·이용 금지' 고지를 감지했을 때 호출.
     * 현재 기사를 제한 매체로 표시해 낭독 버튼·쉐도잉을 숨기고, 진행 중인 낭독은 중단한다.
     */
    fun markSelectedAsRestricted() {
        val item = _selectedNewsItem.value ?: return
        if (item.aiRestricted) return
        item.aiRestricted = true
        _selectedNewsItem.value = item.copy()
        _newsItems.value = _newsItems.value.map {
            if (it.link == item.link) it.copy(aiRestricted = true) else it
        }
        if (_isBriefingPlaying.value) stopBriefing()
        Log.i(TAG, "🚫 원문 페이지에서 AI 이용 금지 고지 감지 → 낭독·쉐도잉 비활성: ${item.title}")
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
        recordingManager.stopPlayback()
        sttManager.destroy()
    }
}

sealed class ApiKeyStatus {
    object Idle : ApiKeyStatus()
    object Validating : ApiKeyStatus()
    data class Valid(val message: String) : ApiKeyStatus()
    data class Invalid(val error: String) : ApiKeyStatus()
}

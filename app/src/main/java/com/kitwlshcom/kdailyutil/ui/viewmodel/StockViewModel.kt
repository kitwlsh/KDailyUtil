package com.kitwlshcom.kdailyutil.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitwlshcom.kdailyutil.data.model.ChartRange
import com.kitwlshcom.kdailyutil.data.model.EarningsDisclosure
import com.kitwlshcom.kdailyutil.data.model.ExpectedEarnings
import com.kitwlshcom.kdailyutil.data.model.StockChartData
import com.kitwlshcom.kdailyutil.data.model.StockPriceItem
import com.kitwlshcom.kdailyutil.data.remote.GeminiManager
import com.kitwlshcom.kdailyutil.data.repository.SettingsRepository
import com.kitwlshcom.kdailyutil.data.repository.StockRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StockViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val stockRepository = StockRepository(application)
    private val newsRepository = com.kitwlshcom.kdailyutil.data.repository.NewsRepository(application)

    companion object {
        private const val TAG = "StockViewModel"
    }

    // 실적 뉴스·전망: 종목별 '실적' 관련 뉴스
    private val _earningsNews = MutableStateFlow<List<com.kitwlshcom.kdailyutil.data.model.NewsItem>>(emptyList())
    val earningsNews: StateFlow<List<com.kitwlshcom.kdailyutil.data.model.NewsItem>> = _earningsNews.asStateFlow()
    private val _earningsNewsTitle = MutableStateFlow("")
    val earningsNewsTitle: StateFlow<String> = _earningsNewsTitle.asStateFlow()
    private val _earningsNewsLoading = MutableStateFlow(false)
    val earningsNewsLoading: StateFlow<Boolean> = _earningsNewsLoading.asStateFlow()

    // 과거 실적 조회 + 회사 이름 검색
    private val _financialHistory = MutableStateFlow<List<com.kitwlshcom.kdailyutil.data.model.FinancialPeriod>>(emptyList())
    val financialHistory: StateFlow<List<com.kitwlshcom.kdailyutil.data.model.FinancialPeriod>> = _financialHistory.asStateFlow()
    private val _financialHistoryTitle = MutableStateFlow("")
    val financialHistoryTitle: StateFlow<String> = _financialHistoryTitle.asStateFlow()
    private val _financialHistoryLoading = MutableStateFlow(false)
    val financialHistoryLoading: StateFlow<Boolean> = _financialHistoryLoading.asStateFlow()

    private val _corpSearchResults = MutableStateFlow<List<com.kitwlshcom.kdailyutil.data.model.CorpEntry>>(emptyList())
    val corpSearchResults: StateFlow<List<com.kitwlshcom.kdailyutil.data.model.CorpEntry>> = _corpSearchResults.asStateFlow()
    private val _corpSearchLoading = MutableStateFlow(false)
    val corpSearchLoading: StateFlow<Boolean> = _corpSearchLoading.asStateFlow()

    private val _stockPrices = MutableStateFlow<List<StockPriceItem>>(emptyList())
    val stockPrices: StateFlow<List<StockPriceItem>> = _stockPrices.asStateFlow()

    private val _disclosures = MutableStateFlow<List<EarningsDisclosure>>(emptyList())
    val disclosures: StateFlow<List<EarningsDisclosure>> = _disclosures.asStateFlow()

    private val _expectedEarnings = MutableStateFlow<List<ExpectedEarnings>>(emptyList())
    val expectedEarnings: StateFlow<List<ExpectedEarnings>> = _expectedEarnings.asStateFlow()

    private val _isPricesLoading = MutableStateFlow(false)
    val isPricesLoading: StateFlow<Boolean> = _isPricesLoading.asStateFlow()

    private val _isDisclosuresLoading = MutableStateFlow(false)
    val isDisclosuresLoading: StateFlow<Boolean> = _isDisclosuresLoading.asStateFlow()

    private val _isExpectedLoading = MutableStateFlow(false)
    val isExpectedLoading: StateFlow<Boolean> = _isExpectedLoading.asStateFlow()

    private val _selectedDateFilterDays = MutableStateFlow(3) // 기본 최근 3일 조회
    val selectedDateFilterDays: StateFlow<Int> = _selectedDateFilterDays.asStateFlow()

    private val _isAiSummarizing = MutableStateFlow(false)
    val isAiSummarizing: StateFlow<Boolean> = _isAiSummarizing.asStateFlow()

    private val _activeAiSummaryDisclosure = MutableStateFlow<EarningsDisclosure?>(null)
    val activeAiSummaryDisclosure: StateFlow<EarningsDisclosure?> = _activeAiSummaryDisclosure.asStateFlow()

    // 즐겨찾기 / 숨김 상태
    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    private val _hiddenIds = MutableStateFlow<Set<String>>(emptySet())
    val hiddenIds: StateFlow<Set<String>> = _hiddenIds.asStateFlow()

    private val _showHidden = MutableStateFlow(false)
    val showHidden: StateFlow<Boolean> = _showHidden.asStateFlow()

    // 실적 공시 탭이 현재 화면에 보이는지 (false면 분석 완료 시 알림/배너로 안내)
    @Volatile private var disclosureTabActive = false
    fun setDisclosureTabActive(active: Boolean) { disclosureTabActive = active }

    // 실적 예정 일정(사전 전망) 탭이 보이는지
    @Volatile private var expectedTabActive = false
    fun setExpectedTabActive(active: Boolean) { expectedTabActive = active }

    // 앱이 포그라운드(화면에 보이는 상태)인지 — 백그라운드면 시스템 알림, 포그라운드면 인앱 배너
    @Volatile private var appInForeground = true
    fun setAppForeground(active: Boolean) { appInForeground = active }

    // 인앱 완료 배너 이벤트 (메시지, 이동할 증시 서브탭 인덱스)
    private val _analysisCompletedEvent = kotlinx.coroutines.flow.MutableSharedFlow<Pair<String, Int>>(extraBufferCapacity = 4)
    val analysisCompletedEvent = _analysisCompletedEvent.asSharedFlow()

    // 알림/배너에서 요청한 증시 서브탭 (StockDashboardScreen이 소비)
    private val _requestedStockSubTab = MutableStateFlow<Int?>(null)
    val requestedStockSubTab: StateFlow<Int?> = _requestedStockSubTab.asStateFlow()
    fun requestStockSubTab(index: Int) { _requestedStockSubTab.value = index }
    fun consumeRequestedSubTab() { _requestedStockSubTab.value = null }

    private var pollingJob: kotlinx.coroutines.Job? = null

    // ─────────────────────────────────────────────
    // 차트 상세 바텀시트 관련 StateFlow
    // ─────────────────────────────────────────────
    private val _selectedStock = MutableStateFlow<StockPriceItem?>(null)
    val selectedStock: StateFlow<StockPriceItem?> = _selectedStock.asStateFlow()

    private val _chartRange = MutableStateFlow(ChartRange.TODAY)
    val chartRange: StateFlow<ChartRange> = _chartRange.asStateFlow()

    private val _chartData = MutableStateFlow<StockChartData?>(null)
    val chartData: StateFlow<StockChartData?> = _chartData.asStateFlow()

    private val _isChartLoading = MutableStateFlow(false)
    val isChartLoading: StateFlow<Boolean> = _isChartLoading.asStateFlow()

    // 종목 편집 모드 현재 키워드 목록
    private val _stockKeywords = MutableStateFlow<List<String>>(emptyList())
    val stockKeywords: StateFlow<List<String>> = _stockKeywords.asStateFlow()

    init {
        loadStockPrices(showLoading = true)
        loadStockKeywords()
        loadDisclosures()
        loadExpectedEarnings()
    }

    /**
     * 관심 종목 시세 정보를 로드합니다.
     * @param showLoading 중앙 프로그레스 바 노출 여부 (자동 폴링 시에는 false)
     */
    fun loadStockPrices(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _isPricesLoading.value = true
            }
            try {
                // ⚠️ watchStockKeywordsFlow 사용 — 뉴스탭 필터(stockKeywordsFlow)와 엄감히 분리
                val keywords = settingsRepository.watchStockKeywordsFlow.first()
                val list = keywords.map { keyword ->
                    stockRepository.getStockPrice(keyword, forceRefresh = showLoading)
                }
                _stockPrices.value = list
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to load stock prices: ${e.message}")
            } finally {
                if (showLoading) {
                    _isPricesLoading.value = false
                }
            }
        }
    }

    /**
     * 장중 주기적인 자동 새로고침(Auto Polling)을 시작합니다.
     */
    fun startPricePolling() {
        if (pollingJob != null) return
        pollingJob = viewModelScope.launch {
            // 30초 간격으로 주가 정보 백그라운드 갱신
            while (true) {
                delay(30000L)
                loadStockPrices(showLoading = false)
            }
        }
        Log.d(TAG, "🚀 Started Stock Price Auto-Polling (30s interval)")
    }

    /**
     * 자동 새로고침 폴링을 중단합니다.
     */
    fun stopPricePolling() {
        pollingJob?.cancel()
        pollingJob = null
        Log.d(TAG, "⏹️ Stopped Stock Price Auto-Polling")
    }

    // ─────────────────────────────────────────────
    // 차트 상세 제어
    // ─────────────────────────────────────────────
    /** 특정 종목을 선택하여 차트 바텀시트를 열고, 현재 단일 데이터를 로드 */
    fun openStockDetail(item: StockPriceItem) {
        _selectedStock.value = item
        _chartRange.value = ChartRange.TODAY
        loadChartData(item.name, ChartRange.TODAY)
    }

    /** 차트 바텀시트 닫기 */
    fun closeStockDetail() {
        _selectedStock.value = null
        _chartData.value = null
    }

    /** 차트 기간 변경 */
    fun setChartRange(range: ChartRange) {
        val current = _selectedStock.value ?: return
        _chartRange.value = range
        loadChartData(current.name, range)
    }

    /** Yahoo Finance에서 기간별 차트 데이터 로드 */
    private fun loadChartData(name: String, range: ChartRange) {
        viewModelScope.launch {
            _isChartLoading.value = true
            try {
                val data = stockRepository.getChartData(name, range)
                _chartData.value = data
            } catch (e: Exception) {
                Log.e(TAG, "❌ loadChartData error: ${e.message}")
            } finally {
                _isChartLoading.value = false
            }
        }
    }

    // ─────────────────────────────────────────────
    // 📈 증시탭 관심종목 관리 (WATCH_STOCK_KEYWORDS DataStore)
    //    특집논에: 뉴스탭 증시 필터(STOCK_KEYWORDS)와 완전히 분리되어 독립 저장소를 사용
    // ─────────────────────────────────────────────
    /** 증시탭 관심종목 목록을 DataStore에서 실시간 수집 */
    fun loadStockKeywords() {
        viewModelScope.launch {
            settingsRepository.watchStockKeywordsFlow.collect { keywords ->
                _stockKeywords.value = keywords.toList()
            }
        }
    }

    /** 증시탭 관심종목에 종목을 추가 */
    fun addStockKeyword(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val current = settingsRepository.watchStockKeywordsFlow.first().toMutableSet()
            if (current.add(trimmed)) {
                settingsRepository.updateWatchStockKeywords(current)
                loadStockPrices(showLoading = true)
            }
        }
    }

    /** 증시탭 관심종목에서 종목을 제거 */
    fun removeStockKeyword(name: String) {
        viewModelScope.launch {
            val current = settingsRepository.watchStockKeywordsFlow.first().toMutableSet()
            if (current.remove(name)) {
                settingsRepository.updateWatchStockKeywords(current)
                _stockPrices.value = _stockPrices.value.filter { it.name != name }
            }
        }
    }

    /**
     * DART 실적 공시 리스트를 로드합니다.
     */
    fun loadDisclosures(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isDisclosuresLoading.value = true
            try {
                val apiKey = settingsRepository.dartApiKeyFlow.first()
                val days = _selectedDateFilterDays.value
                
                val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                val cal = Calendar.getInstance()
                val endDe = sdf.format(cal.time) // 오늘
                
                cal.add(Calendar.DATE, -days)
                val bgnDe = sdf.format(cal.time) // 시작일
                
                val raw = stockRepository.fetchRecentDisclosures(bgnDe, endDe, apiKey).toMutableList()
                val favorites = stockRepository.loadFavorites()
                val favIds = favorites.map { it.rcept_no }.toSet()
                val hidden = stockRepository.loadHidden()
                _favoriteIds.value = favIds
                _hiddenIds.value = hidden

                // 즐겨찾기 항목이 조회 기간 밖이라 목록에 없으면 추가 (계속 보이도록)
                val ids = raw.map { it.rcept_no }.toSet()
                favorites.forEach { if (it.rcept_no !in ids) raw.add(it) }

                // 이미 분석한 AI 결과(요약/뱃지)를 rcept_no로 복원 — 조회기간 변경 등으로 새로 가져와도 초기화되지 않도록.
                // (AI 재분석은 '새로고침' 또는 재분석 버튼에서만 일어남)
                val cachedById = stockRepository.loadCachedDisclosures().associateBy { it.rcept_no }
                var merged = raw.map { item ->
                    val cached = cachedById[item.rcept_no]
                    val cachedSummary = cached?.aiSummary
                    val restored = if (item.aiSummary.isNullOrBlank() && !cachedSummary.isNullOrBlank()) {
                        item.copy(aiSummary = cachedSummary, isSurprise = cached!!.isSurprise, isTurnaround = cached.isTurnaround)
                    } else item
                    restored.copy(isFavorite = restored.rcept_no in favIds)
                }
                if (!_showHidden.value) merged = merged.filter { it.rcept_no !in hidden }
                // 즐겨찾기 우선 → 최신 날짜순
                merged = merged.sortedWith(
                    compareByDescending<EarningsDisclosure> { it.isFavorite }.thenByDescending { it.rcept_dt }
                )
                _disclosures.value = merged
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to load disclosures: ${e.message}")
            } finally {
                _isDisclosuresLoading.value = false
            }
        }
    }

    /**
     * 실적 발표 예정 일정을 크롤링합니다.
     */
    fun loadExpectedEarnings() {
        viewModelScope.launch {
            _isExpectedLoading.value = true
            try {
                val keywords = settingsRepository.watchStockKeywordsFlow.first()
                val list = stockRepository.fetchExpectedEarnings(keywords.toList())
                _expectedEarnings.value = list
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to load expected earnings: ${e.message}")
            } finally {
                _isExpectedLoading.value = false
            }
        }
    }

    /** 종목의 '실적' 관련 뉴스를 불러온다. (예상·전망 기사 확인용) */
    fun loadEarningsNews(corpName: String) {
        viewModelScope.launch {
            _earningsNewsTitle.value = corpName
            _earningsNewsLoading.value = true
            _earningsNews.value = emptyList()
            try {
                _earningsNews.value = newsRepository.getNewsByKeyword("$corpName 실적", 15)
            } catch (e: Exception) {
                Log.e(TAG, "❌ 실적 뉴스 로드 실패: ${e.message}")
            } finally {
                _earningsNewsLoading.value = false
            }
        }
    }

    fun clearEarningsNews() {
        _earningsNews.value = emptyList()
        _earningsNewsTitle.value = ""
    }

    /** 특정 회사의 과거 실적(최근 정기보고서)을 조회한다. */
    fun loadFinancialHistory(corpCode: String, corpName: String, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _financialHistoryTitle.value = corpName
            _financialHistoryLoading.value = true
            if (forceRefresh) _financialHistory.value = emptyList()
            try {
                val apiKey = settingsRepository.dartApiKeyFlow.first()
                _financialHistory.value = stockRepository.fetchFinancialHistory(corpCode, apiKey, maxPeriods = 8, forceRefresh = forceRefresh)
            } catch (e: Exception) {
                Log.e(TAG, "❌ 과거 실적 로드 실패: ${e.message}")
            } finally {
                _financialHistoryLoading.value = false
            }
        }
    }

    fun clearFinancialHistory() {
        _financialHistory.value = emptyList()
        _financialHistoryTitle.value = ""
    }

    private var searchJob: kotlinx.coroutines.Job? = null

    /**
     * 회사 이름으로 상장사를 검색한다(corpCode.xml, 최초 1회 다운로드).
     * 입력마다 즉시 실행하지 않고 디바운스(350ms)+최소 2글자 — 대용량 다운로드 중복/과부하 방지.
     */
    fun searchCompany(query: String) {
        searchJob?.cancel()
        val q = query.trim()
        if (q.length < 2) {
            _corpSearchResults.value = emptyList()
            _corpSearchLoading.value = false
            return
        }
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(350)
            _corpSearchLoading.value = true
            try {
                val apiKey = settingsRepository.dartApiKeyFlow.first()
                _corpSearchResults.value = stockRepository.searchCorpByName(q, apiKey, limit = 30)
            } catch (e: Exception) {
                Log.e(TAG, "❌ 회사 검색 실패: ${e.message}")
            } finally {
                _corpSearchLoading.value = false
            }
        }
    }

    fun clearCorpSearch() {
        _corpSearchResults.value = emptyList()
    }

    /**
     * 조회 기간 필터를 수정하고 공시 리스트를 재조회합니다.
     */
    fun setDateFilter(days: Int) {
        _selectedDateFilterDays.value = days
        loadDisclosures()
    }

    /** 공시 즐겨찾기 토글 (즐겨찾기 항목은 조회 기간과 무관하게 계속 표시) */
    fun toggleFavorite(item: EarningsDisclosure) {
        viewModelScope.launch {
            val favs = stockRepository.loadFavorites().toMutableList()
            val nowFav: Boolean
            if (favs.any { it.rcept_no == item.rcept_no }) {
                favs.removeAll { it.rcept_no == item.rcept_no }
                nowFav = false
            } else {
                favs.add(item.copy(isFavorite = true))
                nowFav = true
            }
            stockRepository.saveFavorites(favs)
            _favoriteIds.value = favs.map { it.rcept_no }.toSet()
            _disclosures.value = _disclosures.value
                .map { if (it.rcept_no == item.rcept_no) it.copy(isFavorite = nowFav) else it }
                .sortedWith(compareByDescending<EarningsDisclosure> { it.isFavorite }.thenByDescending { it.rcept_dt })
        }
    }

    /** 공시 숨김 토글 */
    fun toggleHidden(item: EarningsDisclosure) {
        viewModelScope.launch {
            val hidden = stockRepository.loadHidden().toMutableSet()
            if (item.rcept_no in hidden) hidden.remove(item.rcept_no) else hidden.add(item.rcept_no)
            stockRepository.saveHidden(hidden)
            _hiddenIds.value = hidden
            if (!_showHidden.value) {
                _disclosures.value = _disclosures.value.filter { it.rcept_no !in hidden }
            }
        }
    }

    /** 숨긴 항목 표시 여부 토글 */
    fun toggleShowHidden() {
        _showHidden.value = !_showHidden.value
        loadDisclosures()
    }

    /**
     * AI 분석이 백그라운드(앱이 화면에 없을 때)에서 끝났을 때 시스템 알림 표시.
     * 알림 탭 시 앱을 새로 실행하지 않고 기존 인스턴스로 복귀(singleTask) + 해당 서브탭으로 이동.
     */
    private fun notifyAnalysisDone(corpName: String, subTab: Int) {
        val ctx = getApplication<Application>()
        val channelId = "ai_analysis_channel"
        val nm = ctx.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                android.app.NotificationChannel(channelId, "AI 실적 분석", android.app.NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "백그라운드 AI 실적 분석 완료 알림" }
            )
        }
        // CLEAR_TASK 제거 → 기존 액티비티(singleTask)를 그대로 앞으로 가져오고 onNewIntent로 전달
        val intent = android.content.Intent(ctx, com.kitwlshcom.kdailyutil.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("NAVIGATE_TO", "stock")
            putExtra("STOCK_SUBTAB", subTab)
        }
        val pi = android.app.PendingIntent.getActivity(
            ctx, 3001, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val noti = androidx.core.app.NotificationCompat.Builder(ctx, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("AI 분석 완료 📊")
            .setContentText("$corpName 분석이 완료되었습니다. 터치하여 확인하세요.")
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        try {
            nm.notify(3001, noti)
        } catch (e: SecurityException) {
            Log.e(TAG, "알림 권한 없음: ${e.message}")
        }
    }

    /**
     * 특정 실적 공시건에 대해 AI 3줄 요약 분석을 수행하고, 캐싱 및 상태를 갱신합니다.
     * 429 트래픽 한도 초과 오류 발생 시 지수 백오프 기반 재시도 처리를 적용합니다.
     */
    fun summarizeDisclosure(disclosure: EarningsDisclosure, forceRefresh: Boolean = false, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            // 1. 이미 캐시된 내용이 있다면 로컬 데이터를 즉시 UI에 반영 (로딩 속도 0.1초 미만)
            //    단, 강제 재분석(forceRefresh) 시에는 캐시를 무시하고 새로 분석.
            if (!forceRefresh && disclosure.aiSummary != null) {
                _activeAiSummaryDisclosure.value = disclosure
                onComplete(true, disclosure.aiSummary!!)
                return@launch
            }

            _isAiSummarizing.value = true
            _activeAiSummaryDisclosure.value = null

            val geminiKey = settingsRepository.geminiApiKeyFlow.first()
            val dartKey = settingsRepository.dartApiKeyFlow.first()

            if (geminiKey.isNullOrBlank()) {
                _isAiSummarizing.value = false
                onComplete(false, "설정에서 Gemini API Key를 등록해 주세요.")
                return@launch
            }

            var attempts = 0
            val maxAttempts = 3
            var delayMillis = 1500L
            var success = false
            var finalResult = ""

            try {
                // DART에서 정형 재무 데이터 JSON 추출
                val financialJson = stockRepository.fetchCompanyFinancialJson(
                    disclosure.corp_code,
                    disclosure.rcept_dt,
                    dartKey,
                    disclosure.report_nm
                )

                val gemini = GeminiManager(geminiKey)

                // 429 지수 백오프 기반 재시도 루프
                while (attempts < maxAttempts && !success) {
                    try {
                        val responseJsonStr = gemini.verifyEarningsDisclosure(financialJson)
                        if (responseJsonStr.isNotBlank() && responseJsonStr.startsWith("{")) {
                            val jsonObj = JSONObject(responseJsonStr)
                            
                            val isSurprise = if (jsonObj.has("isSurprise") && !jsonObj.isNull("isSurprise")) {
                                jsonObj.getBoolean("isSurprise")
                            } else null
                            
                            val isTurnaround = jsonObj.optBoolean("isTurnaround", false)
                            val summary = jsonObj.optString("summary", "요약을 가져올 수 없습니다.")

                            disclosure.aiSummary = summary
                            // 코틀린 객체 복사본을 만들어 속성 덮어쓰기 우회
                            val updatedDisclosure = disclosure.copy(
                                aiSummary = summary,
                                isSurprise = isSurprise,
                                isTurnaround = isTurnaround
                            )

                            // 캐시 파일에 반영 및 저장
                            val cachedList = stockRepository.loadCachedDisclosures().toMutableList()
                            cachedList.removeAll { it.rcept_no == disclosure.rcept_no }
                            cachedList.add(updatedDisclosure)
                            stockRepository.saveCachedDisclosures(cachedList)

                            // UI 리스트도 갱신
                            _disclosures.value = _disclosures.value.map {
                                if (it.rcept_no == disclosure.rcept_no) updatedDisclosure else it
                            }

                            _activeAiSummaryDisclosure.value = updatedDisclosure
                            finalResult = summary
                            success = true
                        } else {
                            throw Exception("올바르지 않은 AI 응답 규격입니다.")
                        }
                    } catch (e: Exception) {
                        attempts++
                        val isRateLimitError = e.message?.contains("429") == true || e.message?.contains("Quota") == true
                        if (isRateLimitError && attempts < maxAttempts) {
                            Log.w(TAG, "⚠️ Gemini API Rate Limit (429) detected. Retrying in $delayMillis ms... (Attempt $attempts/$maxAttempts)")
                            delay(delayMillis)
                            delayMillis *= 2 // 지수 백오프 적용
                        } else {
                            throw e // 다른 치명적 에러는 즉시 탈출
                        }
                    }
                }

                if (success) {
                    // 백그라운드면 시스템 알림, 앱 내 다른 메뉴면 인앱 배너, 해당 탭이면 콜백(다이얼로그)로 표시
                    when {
                        !appInForeground -> notifyAnalysisDone(disclosure.corp_name, 1)
                        !disclosureTabActive -> _analysisCompletedEvent.tryEmit("${disclosure.corp_name} 실적 분석 완료" to 1)
                    }
                    onComplete(true, finalResult)
                } else {
                    onComplete(false, "실적 분석 생성에 실패했습니다. 잠시 후 다시 시도해 주세요.")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to summarize disclosure: ${e.message}", e)
                onComplete(false, "분석 오류 발생: ${e.message}")
            } finally {
                _isAiSummarizing.value = false
            }
        }
    }

    /**
     * 공시 예정 종목에 대한 AI 사전 전망 리포트를 생성합니다.
     */
    fun generatePreReport(item: ExpectedEarnings, forceRefresh: Boolean = false, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            // 캐시된 리포트가 있으면 즉시 반환 (강제 재분석 시 제외)
            if (!forceRefresh && item.aiReport != null) {
                onComplete(item.aiReport!!)
                return@launch
            }

            val geminiKey = settingsRepository.geminiApiKeyFlow.first()
            if (geminiKey.isNullOrBlank()) {
                onComplete("설정에서 Gemini API Key를 먼저 입력해야 사전 리포트를 생성할 수 있습니다.")
                return@launch
            }

            try {
                val gemini = GeminiManager(geminiKey)
                val report = gemini.generateExpectedEarningsReport(
                    item.corp_name,
                    item.consensus_revenue,
                    item.consensus_profit
                )
                
                // 인메모리 + 파일 영속 저장 (앱 재시작 후에도 유지)
                item.aiReport = report
                stockRepository.saveExpectedReport(item.corp_name, report)
                _expectedEarnings.value = _expectedEarnings.value.map {
                    if (it.corp_name == item.corp_name) item else it
                }

                // 백그라운드면 시스템 알림, 앱 내 다른 메뉴면 인앱 배너 (예정 일정 탭 = 서브탭 2)
                when {
                    !appInForeground -> notifyAnalysisDone(item.corp_name, 2)
                    !expectedTabActive -> _analysisCompletedEvent.tryEmit("${item.corp_name} 사전 전망 완료" to 2)
                }

                onComplete(report)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to generate pre-report: ${e.message}")
                onComplete("사전 리포트 분석 중 오류가 발생했습니다: ${e.message}")
            }
        }
    }
}

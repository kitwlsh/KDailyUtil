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

    companion object {
        private const val TAG = "StockViewModel"
    }

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
                
                val list = stockRepository.fetchRecentDisclosures(bgnDe, endDe, apiKey)
                _disclosures.value = list
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
                val list = stockRepository.fetchExpectedEarnings()
                _expectedEarnings.value = list
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to load expected earnings: ${e.message}")
            } finally {
                _isExpectedLoading.value = false
            }
        }
    }

    /**
     * 조회 기간 필터를 수정하고 공시 리스트를 재조회합니다.
     */
    fun setDateFilter(days: Int) {
        _selectedDateFilterDays.value = days
        loadDisclosures()
    }

    /**
     * 특정 실적 공시건에 대해 AI 3줄 요약 분석을 수행하고, 캐싱 및 상태를 갱신합니다.
     * 429 트래픽 한도 초과 오류 발생 시 지수 백오프 기반 재시도 처리를 적용합니다.
     */
    fun summarizeDisclosure(disclosure: EarningsDisclosure, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            // 1. 이미 캐시된 내용이 있다면 로컬 데이터를 즉시 UI에 반영 (로딩 속도 0.1초 미만)
            if (disclosure.aiSummary != null) {
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
                    dartKey
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
    fun generatePreReport(item: ExpectedEarnings, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            if (item.aiReport != null) {
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
                
                // 인메모리 저장
                item.aiReport = report
                _expectedEarnings.value = _expectedEarnings.value.map {
                    if (it.corp_name == item.corp_name) item else it
                }
                
                onComplete(report)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to generate pre-report: ${e.message}")
                onComplete("사전 리포트 분석 중 오류가 발생했습니다: ${e.message}")
            }
        }
    }
}

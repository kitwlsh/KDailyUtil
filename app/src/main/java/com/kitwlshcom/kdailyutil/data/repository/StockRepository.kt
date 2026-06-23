package com.kitwlshcom.kdailyutil.data.repository

import android.content.Context
import android.util.Log
import com.kitwlshcom.kdailyutil.data.model.ChartRange
import com.kitwlshcom.kdailyutil.data.model.CurrencyType
import com.kitwlshcom.kdailyutil.data.model.EarningsDisclosure
import com.kitwlshcom.kdailyutil.data.model.ExpectedEarnings
import com.kitwlshcom.kdailyutil.data.model.StockChartData
import com.kitwlshcom.kdailyutil.data.model.StockPriceItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.File
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap

class StockRepository(private val context: Context) {

    companion object {
        private const val TAG = "StockRepository"

        // 주요 한국 상장기업 DART 고유번호 매핑 (Zero-latency 로컬 매핑)
        private val CORP_CODE_MAP = mapOf(
            "삼성전자" to "00126380",
            "SK하이닉스" to "00164779",
            "현대자동차" to "00164742",
            "현대차" to "00164742",
            "기아" to "00123532",
            "네이버" to "00266961",
            "NAVER" to "00266961",
            "카카오" to "00258847",
            "LG에너지솔루션" to "01540060",
            "삼성바이오로직스" to "00877085",
            "셀트리온" to "00155285",
            "POSCO홀딩스" to "00130985",
            "포스코홀딩스" to "00130985",
            "에코프로" to "00713788",
            "에코프로비엠" to "01138241",
            "켄코아에어로스페이스" to "01087462"
        )

        // 주식 이름 -> Yahoo Finance 심볼 매핑
        private val SYMBOL_MAP = mapOf(
            "삼성전자" to "005930.KS",
            "SK하이닉스" to "000660.KS",
            "현대차" to "005380.KS",
            "현대자동차" to "005380.KS",
            "기아" to "000270.KS",
            "네이버" to "035420.KS",
            "NAVER" to "035420.KS",
            "카카오" to "035720.KS",
            "LG에너지솔루션" to "373220.KS",
            "에코프로" to "086520.KQ",
            "테슬라" to "TSLA",
            "애플" to "AAPL",
            "엔비디아" to "NVDA",
            "마이크로소프트" to "MSFT",
            "구글" to "GOOGL",
            "비트코인" to "BTC-USD",
            "나스닥" to "^IXIC",
            "코스피" to "^KS11",
            "코스닥" to "^KQ11"
        )
    }

    private val cacheFile: File
        get() = File(context.filesDir, "earnings_disclosures_cache.json")

    // ── 시세 데이터 로컬 파일 및 인메모리 캐시 ──────────────────
    private val priceCacheFile: File
        get() = File(context.filesDir, "stock_prices_cache.json")

    private val cachedPrices = ConcurrentHashMap<String, StockPriceItem>()

    // 앱 기동 후 각 종목별 최초 1회 갱신을 추적하기 위한 인메모리 셋
    private val fetchedOnLaunch = ConcurrentHashMap.newKeySet<String>()

    init {
        loadCachedPrices()
    }

    @Synchronized
    private fun loadCachedPrices() {
        if (!priceCacheFile.exists()) return
        try {
            val jsonStr = priceCacheFile.readText(StandardCharsets.UTF_8)
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val symbol = obj.getString("symbol")
                val name = obj.getString("name")
                val price = obj.getDouble("price")
                val change = obj.getDouble("change")
                val changeAmount = obj.optDouble("changeAmount", 0.0)
                val updateTime = obj.optString("updateTime", "")
                val delayInfo = obj.optString("delayInfo", "")
                val currencyTypeStr = obj.optString("currencyType", "USD")
                val currencyType = try { CurrencyType.valueOf(currencyTypeStr) } catch(e: Exception) { CurrencyType.USD }
                
                val sparkline = mutableListOf<Float>()
                val sparkArray = obj.optJSONArray("sparkline")
                if (sparkArray != null) {
                    for (j in 0 until sparkArray.length()) {
                        sparkline.add(sparkArray.getDouble(j).toFloat())
                    }
                }
                
                val item = StockPriceItem(
                    symbol = symbol,
                    name = name,
                    price = price,
                    change = change,
                    changeAmount = changeAmount,
                    sparkline = sparkline,
                    updateTime = updateTime,
                    delayInfo = delayInfo,
                    currencyType = currencyType
                )
                cachedPrices[name] = item
            }
            Log.d(TAG, "💾 Loaded ${cachedPrices.size} stock prices from local cache.")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load cached prices: ${e.message}")
        }
    }

    @Synchronized
    private fun saveCachedPrices() {
        try {
            val array = JSONArray()
            for ((_, item) in cachedPrices) {
                val obj = JSONObject().apply {
                    put("symbol", item.symbol)
                    put("name", item.name)
                    put("price", item.price)
                    put("change", item.change)
                    put("changeAmount", item.changeAmount)
                    put("updateTime", item.updateTime)
                    put("delayInfo", item.delayInfo)
                    put("currencyType", item.currencyType.name)
                    
                    val sparkArray = JSONArray()
                    item.sparkline.forEach { sparkArray.put(it.toDouble()) }
                    put("sparkline", sparkArray)
                }
                array.put(obj)
            }
            priceCacheFile.writeText(array.toString(), StandardCharsets.UTF_8)
            Log.d(TAG, "💾 Saved ${cachedPrices.size} stock prices to local cache.")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save cached prices: ${e.message}")
        }
    }

    /**
     * 해당 종목의 심볼을 기준으로 현재 거래소가 장중(영업시간)인지 판별합니다.
     */
    fun isMarketOpen(symbol: String): Boolean {
        val isCrypto = symbol.contains("USD") || symbol.contains("=F") // 비트코인 등 암호화폐
        val isKorean = symbol.endsWith(".KS") || symbol.endsWith(".KQ") || symbol == "^KS11" || symbol == "^KQ11"
        
        return when {
            isCrypto -> true // 암호화폐는 24시간 365일 개장
            isKorean -> {
                // 한국 시간(KST) 기준 월~금 09:00 ~ 15:30
                val tz = TimeZone.getTimeZone("Asia/Seoul")
                val cal = Calendar.getInstance(tz)
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                    false
                } else {
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    val minute = cal.get(Calendar.MINUTE)
                    val timeInMinutes = hour * 60 + minute
                    timeInMinutes in (9 * 60)..(15 * 60 + 30)
                }
            }
            else -> {
                // 미국 시간(EST/EDT) 기준 월~금 09:30 ~ 16:00
                // TimeZone "America/New_York" 설정 시 자바 Calendar 내부에서 미국 서머타임을 자동 계산함
                val tz = TimeZone.getTimeZone("America/New_York")
                val cal = Calendar.getInstance(tz)
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                    false
                } else {
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    val minute = cal.get(Calendar.MINUTE)
                    val timeInMinutes = hour * 60 + minute
                    timeInMinutes in (9 * 60 + 30)..(16 * 60)
                }
            }
        }
    }

    /**
     * Yahoo Finance API를 통해 실시간 주가 시세 및 스파크라인 포인트를 조회합니다.
     * 장중이 아닐 경우(장마감 상태) 캐싱된 로컬 데이터를 즉시 반환하여 네트워크 사용을 억제하되,
     * 앱 최초 기동 시 또는 강제 새로고침(forceRefresh=true) 시에는 1회 업데이트를 수행합니다.
     */
    suspend fun getStockPrice(name: String, forceRefresh: Boolean = false): StockPriceItem = withContext(Dispatchers.IO) {
        val symbol = SYMBOL_MAP[name] ?: name
        val isOpen = isMarketOpen(symbol)
        val isFirstFetch = !fetchedOnLaunch.contains(name)

        // ──────────────────────────────────────────────────
        // 1. 장중이 아니고, 기존 캐시 데이터가 있으며, 최초 기동시 1회 조회도 완료했고, 강제 갱신이 아닌 경우 캐시 리턴
        // ──────────────────────────────────────────────────
        val existingCache = cachedPrices[name]
        if (!isOpen && existingCache != null && !isFirstFetch && !forceRefresh) {
            Log.d(TAG, "⏭️ Market CLOSED for $name ($symbol). Returning cached price.")
            return@withContext existingCache
        }

        try {
            // ──────────────────────────────────────────────────
            // 2. 시세(현재가 & 전일 종가 기준 등락률) 조회
            // ──────────────────────────────────────────────────
            val quoteUrl = "https://query1.finance.yahoo.com/v8/finance/chart/$symbol?interval=1m&range=1d"
            val quoteJson = Jsoup.connect(quoteUrl)
                .ignoreContentType(true)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(8000)
                .execute()
                .body()

            val root = JSONObject(quoteJson)
            val chart = root.getJSONObject("chart")
            val resultList = chart.getJSONArray("result")

            if (resultList.length() > 0) {
                val resultObj = resultList.getJSONObject(0)
                val meta = resultObj.getJSONObject("meta")

                val price = meta.optDouble("regularMarketPrice", 0.0)

                // ✅ 전일 종가(regularMarketPreviousClose)로 당일 등락률 계산
                val prevClose = meta.optDouble("regularMarketPreviousClose", 0.0)
                    .takeIf { it != 0.0 }
                    ?: meta.optDouble("previousClose", 0.0)

                val change = if (prevClose != 0.0) {
                    ((price - prevClose) / prevClose) * 100.0
                } else {
                    0.0
                }

                // 주가 증감 금액 계산
                val changeAmount = if (prevClose != 0.0) {
                    price - prevClose
                } else {
                    0.0
                }

                // API 데이터의 기준 시간 (Unix epoch in seconds)
                val marketTimeEpoch = meta.optLong("regularMarketTime", 0L)
                val updateTimeStr = if (marketTimeEpoch > 0L) {
                    val sdf = SimpleDateFormat("MM.dd HH:mm", Locale.KOREA)
                    sdf.format(Date(marketTimeEpoch * 1000L))
                } else {
                    SimpleDateFormat("MM.dd HH:mm", Locale.KOREA).format(Date())
                }

                // 실시간 여부 판별 (지수 및 암호화폐는 실시간 수준, 개별 주식은 15~20분 지연)
                val isCryptoOrIndex = symbol.startsWith("^") || symbol.contains("USD") || symbol.contains("=F")
                val delayInfoStr = when {
                    isCryptoOrIndex -> "실시간급"
                    symbol.endsWith(".KS") || symbol.endsWith(".KQ") -> "20분 지연"
                    else -> "15분 지연"
                }

                val currencyType = when {
                    symbol.startsWith("^") -> CurrencyType.INDEX
                    symbol.endsWith(".KS") || symbol.endsWith(".KQ") -> CurrencyType.KRW
                    else -> CurrencyType.USD
                }

                // ──────────────────────────────────────────────────
                // 3. 당일 장중 분봉(1m)으로 스파크라인 데이터 추출
                // ──────────────────────────────────────────────────
                val sparklinePoints = mutableListOf<Float>()
                val indicators = resultObj.optJSONObject("indicators")
                val quoteList = indicators?.optJSONArray("quote")
                if (quoteList != null && quoteList.length() > 0) {
                    val quoteData = quoteList.getJSONObject(0)
                    val closeArray = quoteData.optJSONArray("close")
                    if (closeArray != null) {
                        for (i in 0 until closeArray.length()) {
                            val v = closeArray.optDouble(i)
                            if (!v.isNaN() && v > 0.0) {
                                sparklinePoints.add(v.toFloat())
                            }
                        }
                    }
                }

                val updatedItem = StockPriceItem(
                    symbol = symbol,
                    name = name,
                    price = price,
                    change = change,
                    changeAmount = changeAmount,
                    sparkline = sparklinePoints,
                    updateTime = updateTimeStr,
                    delayInfo = delayInfoStr,
                    currencyType = currencyType
                )

                // ──────────────────────────────────────────────────
                // 4. 캐시 및 로컬 파일 갱신
                // ──────────────────────────────────────────────────
                cachedPrices[name] = updatedItem
                saveCachedPrices()
                fetchedOnLaunch.add(name) // 최초 1회 업데이트 완료 처리

                return@withContext updatedItem
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to fetch stock price for $name ($symbol): ${e.message}")
        }

        // ──────────────────────────────────────────────────
        // 5. 에러 발생 시 Fallback: 기존 캐시 반환, 정 캐시조차 없으면 0.0 리턴
        // ──────────────────────────────────────────────────
        if (existingCache != null) {
            Log.w(TAG, "⚠️ Connection failed for $name. Returning cached price as fallback.")
            return@withContext existingCache
        }

        val currentLocalTime = SimpleDateFormat("MM.dd HH:mm", Locale.KOREA).format(Date())
        val fallbackCurrency = when {
            symbol.startsWith("^") -> CurrencyType.INDEX
            symbol.endsWith(".KS") || symbol.endsWith(".KQ") -> CurrencyType.KRW
            else -> CurrencyType.USD
        }
        return@withContext StockPriceItem(
            symbol = symbol,
            name = name,
            price = 0.0,
            change = 0.0,
            updateTime = currentLocalTime,
            delayInfo = "연결 실패",
            currencyType = fallbackCurrency
        )
    }

    /**
     * 기간(ChartRange)에 따른 차트 데이터를 조회합니다.
     * 커드 카드의 기간 버튼 환경 시 호출됨
     */
    suspend fun getChartData(name: String, range: ChartRange): StockChartData = withContext(Dispatchers.IO) {
        val symbol = SYMBOL_MAP[name] ?: name
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/$symbol?interval=${range.interval}&range=${range.range}"

        try {
            val responseJson = Jsoup.connect(url)
                .ignoreContentType(true)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(10000)
                .execute()
                .body()

            val root = JSONObject(responseJson)
            val chart = root.getJSONObject("chart")
            val resultList = chart.getJSONArray("result")

            if (resultList.length() > 0) {
                val resultObj = resultList.getJSONObject(0)
                val timestampsArray = resultObj.optJSONArray("timestamp")
                val indicators = resultObj.optJSONObject("indicators")
                val quoteList = indicators?.optJSONArray("quote")

                val prices = mutableListOf<Float>()
                val timestamps = mutableListOf<Long>()

                if (quoteList != null && quoteList.length() > 0) {
                    val quote = quoteList.getJSONObject(0)
                    val closeArray = quote.optJSONArray("close")
                    if (closeArray != null && timestampsArray != null) {
                        for (i in 0 until closeArray.length()) {
                            val v = closeArray.optDouble(i)
                            if (!v.isNaN() && v > 0.0) {
                                prices.add(v.toFloat())
                                timestamps.add(timestampsArray.optLong(i, 0L))
                            }
                        }
                    }
                }
                return@withContext StockChartData(symbol, name, prices, timestamps, range)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to fetch chart data for $name ($symbol) range=$range: ${e.message}")
        }
        return@withContext StockChartData(symbol, name, emptyList(), emptyList(), range)
    }

    /**
     * @param bgnDe 조회 시작일 (YYYYMMDD)
     * @param endDe 조회 종료일 (YYYYMMDD)
     * @param apiKey DART API Key (비어있으면 공용 키 사용)
     */
    suspend fun fetchRecentDisclosures(
        bgnDe: String,
        endDe: String,
        apiKey: String
    ): List<EarningsDisclosure> = withContext(Dispatchers.IO) {
        val resolvedKey = apiKey.ifBlank { "9c9196d12df614324f10184b78ca26707bd5a9da" }
        // 전체 시장 공시를 한꺼번에 가져오기 위해 corp_code 파라미터를 생략합니다.
        val url = "https://opendart.fss.or.kr/api/list.json?crtfc_key=$resolvedKey&bgn_de=$bgnDe&end_de=$endDe&pblntf_ty=I&page_count=100"

        try {
            val responseJson = Jsoup.connect(url)
                .ignoreContentType(true)
                .timeout(10000)
                .execute()
                .body()

            val root = JSONObject(responseJson)
            val status = root.optString("status", "000")
            if (status == "000") {
                val list = root.optJSONArray("list") ?: JSONArray()
                val disclosures = mutableListOf<EarningsDisclosure>()
                val cachedDisclosures = loadCachedDisclosures()

                for (i in 0 until list.length()) {
                    val obj = list.getJSONObject(i)
                    val reportNm = obj.optString("report_nm", "")
                    
                    // 실적 관련 공시인지 제목 키워드 필터링 (잠정실적, 연결재무제표, 매출액또는손익구조 등)
                    val isEarningsReport = reportNm.contains("실적") || 
                                           reportNm.contains("분기보고서") || 
                                           reportNm.contains("반기보고서") || 
                                           reportNm.contains("사업보고서") || 
                                           reportNm.contains("매출액") || 
                                           reportNm.contains("손익구조")

                    if (isEarningsReport) {
                        val rceptNo = obj.optString("rcept_no", "")
                        val cached = cachedDisclosures.find { it.rcept_no == rceptNo }
                        
                        disclosures.add(
                            EarningsDisclosure(
                                rcept_no = rceptNo,
                                corp_code = obj.optString("corp_code", ""),
                                corp_name = obj.optString("corp_name", ""),
                                report_nm = reportNm,
                                flr_nm = obj.optString("flr_nm", ""),
                                rcept_dt = obj.optString("rcept_dt", ""),
                                aiSummary = cached?.aiSummary,
                                isSurprise = cached?.isSurprise,
                                isTurnaround = cached?.isTurnaround ?: false
                            )
                        )
                    }
                }
                return@withContext disclosures
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Open DART API fetch failed: ${e.message}")
        }
        return@withContext emptyList()
    }

    /**
     * DART 단일회사 주요계정 API를 호출하여 매출액, 영업이익, 당기순이익을 추출 후 정형 JSON으로 가공합니다.
     */
    suspend fun fetchCompanyFinancialJson(
        corpCode: String,
        rceptDt: String,
        apiKey: String
    ): String = withContext(Dispatchers.IO) {
        val resolvedKey = apiKey.ifBlank { "9c9196d12df614324f10184b78ca26707bd5a9da" }
        
        // 공시 제출 날짜 기준으로 사업연도(bsns_year)와 보고서 구분(reprt_code)을 추정
        val year = rceptDt.take(4)
        val month = rceptDt.substring(4, 6).toIntOrNull() ?: 6
        
        // 보고서 매핑: 1분기(4~5월): 11013, 반기(7~8월): 11012, 3분기(10~11월): 11014, 사업보고서(기타/3월): 11011
        val (bsnsYear, reprtCode, reportName) = when (month) {
            in 4..5 -> Triple(year, "11013", "1분기보고서")
            in 7..8 -> Triple(year, "11012", "반기보고서")
            in 10..11 -> Triple(year, "11014", "3분기보고서")
            in 1..3 -> Triple((year.toInt() - 1).toString(), "11011", "사업보고서")
            else -> Triple(year, "11012", "분기/반기보고서") // 기본값 반기
        }

        val url = "https://opendart.fss.or.kr/api/fnlttSinglAcntAll.json?crtfc_key=$resolvedKey&corp_code=$corpCode&bsns_year=$bsnsYear&reprt_code=$reprtCode&fs_div=OFS"

        try {
            val responseJson = Jsoup.connect(url)
                .ignoreContentType(true)
                .timeout(8000)
                .execute()
                .body()

            val root = JSONObject(responseJson)
            val list = root.optJSONArray("list")
            if (list != null && list.length() > 0) {
                var revenueCurrent = 0L
                var revenuePrevious = 0L
                var opProfitCurrent = 0L
                var opProfitPrevious = 0L
                var netIncomeCurrent = 0L
                var netIncomePrevious = 0L
                var companyName = ""

                for (i in 0 until list.length()) {
                    val row = list.getJSONObject(i)
                    companyName = row.optString("corp_name", "")
                    val accName = row.optString("account_nm", "").trim().replace(" ", "")
                    val currentVal = row.optString("thstrm_amount", "0").replace(",", "").toLongOrNull() ?: 0L
                    val previousVal = row.optString("frmtrm_amount", "0").replace(",", "").toLongOrNull() ?: 0L

                    // 1. 매출액 매핑 (동의어 대응)
                    if (accName in setOf("매출액", "매출", "영업수익", "영업매출", "매출수익")) {
                        revenueCurrent = currentVal
                        revenuePrevious = previousVal
                    }
                    // 2. 영업이익 매핑
                    else if (accName in setOf("영업이익", "영업이익(손실)", "영업손실")) {
                        opProfitCurrent = currentVal
                        opProfitPrevious = previousVal
                    }
                    // 3. 당기순이익 매핑
                    else if (accName in setOf("당기순이익", "당기순이익(손실)", "분기순이익", "반기순이익")) {
                        netIncomeCurrent = currentVal
                        netIncomePrevious = previousVal
                    }
                }

                val resultObj = JSONObject().apply {
                    put("company", companyName.ifBlank { corpCode })
                    put("year", bsnsYear)
                    put("report", reportName)
                    put("revenue", JSONObject().put("current", revenueCurrent).put("previous", revenuePrevious))
                    put("operating_profit", JSONObject().put("current", opProfitCurrent).put("previous", opProfitPrevious))
                    put("net_income", JSONObject().put("current", netIncomeCurrent).put("previous", netIncomePrevious))
                }
                return@withContext resultObj.toString()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to parse financial accounts for corpCode $corpCode: ${e.message}")
        }

        // 정형 파싱 실패 시: 잠정실적 등 비정형 공시는 재무 API에 데이터가 없을 수 있음
        // Gemini에게 데이터 없음을 알리는 JSON 반환
        return@withContext JSONObject().apply {
            put("corp_code", corpCode)
            put("rcept_dt", rceptDt)
            put("error", "재무제표 API데이터 없음 - 잠정실적 등 비정형 공시일 수 있음")
            put("hint", "직접 DART 공시 원문에서 실적 내용 파악 요청")
        }.toString()
    }

    /**
     * 네이버 페이 증권 실적 발표 일정표를 Jsoup으로 크롤링하여 예정된 실적 공시 일정을 반환합니다.
     */
    suspend fun fetchExpectedEarnings(): List<ExpectedEarnings> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ExpectedEarnings>()
        try {
            // 네이버 금융 국내 증시 주요 일정 페이지 크롤링 시도
            val url = "https://finance.naver.com/disclosure/disclosure_list.naver"
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .timeout(8000)
                .get()

            // 실제 페이지에 일정이 노출되는 테이블 파싱
            val rows = doc.select(".type_5 tr")
            for (row in rows) {
                val cols = row.select("td")
                if (cols.size >= 4) {
                    val company = cols[1].text().trim()
                    val title = cols[2].text().trim()
                    val date = cols[0].text().trim()
                    if (title.contains("매출액") || title.contains("실적") || title.contains("보고서")) {
                        list.add(ExpectedEarnings(company, date, "-", "-"))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Naver Finance Expected Earnings Scrape Failed: ${e.message}")
        }

        // 웹 크롤링 결과가 없는 경우 Fallback — [예시] 표기로 더미임을 명확히 알림
        if (list.isEmpty()) {
            val cal = Calendar.getInstance()
            val sdf = SimpleDateFormat("MM.dd", Locale.getDefault())

            fun getFutureDate(daysAhead: Int): String {
                val c = cal.clone() as Calendar
                c.add(Calendar.DATE, daysAhead)
                return sdf.format(c.time)
            }

            // ⚠️ 아래는 네이버 크롤링 실패 시 표시되는 예시 데이터입니다.
            list.add(ExpectedEarnings("[예시] 삼성전자", getFutureDate(2), "74.2조원", "8.9조원"))
            list.add(ExpectedEarnings("[예시] SK하이닉스", getFutureDate(4), "12.4조원", "1.2조원"))
            list.add(ExpectedEarnings("[예시] 현대차", getFutureDate(5), "41.5조원", "3.8조원"))
            list.add(ExpectedEarnings("[예시] 카카오", getFutureDate(7), "2.1조원", "1,800억원"))
            list.add(ExpectedEarnings("[예시] 네이버", getFutureDate(9), "2.6조원", "3,900억원"))
            list.add(ExpectedEarnings("[예시] 켄코아에어로스페이스", getFutureDate(11), "240억원", "12억원"))
        }

        return@withContext list
    }


    /**
     * 로컬 파일로부터 캐시된 공시 AI 요약 내역을 가져옵니다.
     */
    @Synchronized
    fun loadCachedDisclosures(): List<EarningsDisclosure> {
        if (!cacheFile.exists()) return emptyList()
        try {
            val jsonStr = cacheFile.readText(StandardCharsets.UTF_8)
            val array = JSONArray(jsonStr)
            val list = mutableListOf<EarningsDisclosure>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    EarningsDisclosure(
                        rcept_no = obj.optString("rcept_no", ""),
                        corp_code = obj.optString("corp_code", ""),
                        corp_name = obj.optString("corp_name", ""),
                        report_nm = obj.optString("report_nm", ""),
                        flr_nm = obj.optString("flr_nm", ""),
                        rcept_dt = obj.optString("rcept_dt", ""),
                        aiSummary = obj.optString("aiSummary").takeIf { it.isNotBlank() },
                        isSurprise = if (obj.has("isSurprise")) obj.optBoolean("isSurprise") else null,
                        isTurnaround = obj.optBoolean("isTurnaround", false)
                    )
                )
            }
            return list
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load cached disclosures: ${e.message}")
        }
        return emptyList()
    }

    /**
     * 공시 AI 요약본 캐시를 로컬 파일에 영속화하고, 90일 지난 오래된 캐시(TTL)는 자동으로 정화합니다.
     */
    @Synchronized
    fun saveCachedDisclosures(disclosures: List<EarningsDisclosure>) {
        try {
            val array = JSONArray()
            val now = System.currentTimeMillis()
            val limitMillis = 90L * 24 * 60 * 60 * 1000 // 90일 밀리초

            // 1. 유효 기간(90일) 내의 공시만 필터링 (TTL Auto-purge)
            val sdf = SimpleDateFormat("yyyyMMDD", Locale.getDefault())
            val filtered = disclosures.filter { disclosure ->
                try {
                    val date = sdf.parse(disclosure.rcept_dt)
                    if (date != null) {
                        (now - date.time) < limitMillis
                    } else true
                } catch (e: Exception) {
                    true
                }
            }

            for (item in filtered) {
                val obj = JSONObject().apply {
                    put("rcept_no", item.rcept_no)
                    put("corp_code", item.corp_code)
                    put("corp_name", item.corp_name)
                    put("report_nm", item.report_nm)
                    put("flr_nm", item.flr_nm)
                    put("rcept_dt", item.rcept_dt)
                    put("aiSummary", item.aiSummary)
                    item.isSurprise?.let { put("isSurprise", it) }
                    put("isTurnaround", item.isTurnaround)
                }
                array.put(obj)
            }

            cacheFile.writeText(array.toString(), StandardCharsets.UTF_8)
            Log.d(TAG, "💾 Saved ${filtered.size} cached disclosures. (90-day TTL applied)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save disclosures cache: ${e.message}")
        }
    }
}

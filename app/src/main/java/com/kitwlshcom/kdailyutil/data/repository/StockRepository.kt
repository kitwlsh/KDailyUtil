package com.kitwlshcom.kdailyutil.data.repository

import android.content.Context
import android.util.Log
import com.kitwlshcom.kdailyutil.data.model.ChartRange
import com.kitwlshcom.kdailyutil.data.model.CurrencyType
import com.kitwlshcom.kdailyutil.data.model.EarningsDisclosure
import com.kitwlshcom.kdailyutil.data.model.ExpectedEarnings
import com.kitwlshcom.kdailyutil.data.model.FinancialPeriod
import com.kitwlshcom.kdailyutil.data.model.CorpEntry
import com.kitwlshcom.kdailyutil.data.model.StockChartData
import com.kitwlshcom.kdailyutil.data.model.StockPriceItem
import java.io.ByteArrayInputStream
import java.io.StringReader
import java.util.zip.ZipInputStream
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
        // 암호화폐(예: BTC-USD)와 선물(=F)만 24시간 개장으로 판별.
        // 단순 contains("USD")는 향후 환율(USD/KRW)·USDT 등을 잘못 잡을 수 있어 접미사로 정확히 매칭.
        val isCrypto = symbol.endsWith("-USD") || symbol.endsWith("=F")
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
                val volumes = mutableListOf<Long>()

                if (quoteList != null && quoteList.length() > 0) {
                    val quote = quoteList.getJSONObject(0)
                    val closeArray = quote.optJSONArray("close")
                    val volumeArray = quote.optJSONArray("volume")
                    if (closeArray != null && timestampsArray != null) {
                        for (i in 0 until closeArray.length()) {
                            val v = closeArray.optDouble(i)
                            if (!v.isNaN() && v > 0.0) {
                                prices.add(v.toFloat())
                                timestamps.add(timestampsArray.optLong(i, 0L))
                                volumes.add(volumeArray?.optLong(i, 0L) ?: 0L)
                            }
                        }
                    }
                }
                return@withContext StockChartData(symbol, name, prices, timestamps, range, volumes)
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
        val resolvedKey = apiKey.ifBlank { com.kitwlshcom.kdailyutil.BuildConfig.DART_DEFAULT_KEY }
        // pblntf_ty=A : 정기공시(사업/반기/분기보고서) — 실제 매출액/영업이익이 재무제표 API로 조회 가능한 공시만 수집.
        // (기존 pblntf_ty=I 거래소공시는 공급계약·소송 등 비실적 공시가 대부분이고 잠정실적은 재무 API에 데이터가 없었음)
        val url = "https://opendart.fss.or.kr/api/list.json?crtfc_key=$resolvedKey&bgn_de=$bgnDe&end_de=$endDe&pblntf_ty=A&page_count=100"

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
                    
                    // 정기보고서(사업/반기/분기)만 수집 — 재무제표 API로 매출액/영업이익 조회가 보장되는 공시.
                    val isEarningsReport = reportNm.contains("사업보고서") ||
                                           reportNm.contains("반기보고서") ||
                                           reportNm.contains("분기보고서")

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
        apiKey: String,
        reportNm: String = ""
    ): String = withContext(Dispatchers.IO) {
        val resolvedKey = apiKey.ifBlank { com.kitwlshcom.kdailyutil.BuildConfig.DART_DEFAULT_KEY }

        val year = rceptDt.take(4).toIntOrNull() ?: 2026
        val month = rceptDt.substring(4, 6).toIntOrNull() ?: 6

        // 보고서명에 포함된 (YYYY.MM)으로 사업연도/보고서코드를 정확히 판별.
        //   .03→1분기(11013) .06→반기(11012) .09→3분기(11014) .12→사업보고서(11011)
        val periodMatch = Regex("\\((\\d{4})[.,](\\d{2})\\)").find(reportNm)
        val (bsnsYear, reprtCode, reportName) = if (periodMatch != null) {
            val y = periodMatch.groupValues[1]
            when (periodMatch.groupValues[2].toIntOrNull() ?: 12) {
                3 -> Triple(y, "11013", "1분기보고서")
                6 -> Triple(y, "11012", "반기보고서")
                9 -> Triple(y, "11014", "3분기보고서")
                12 -> Triple(y, "11011", "사업보고서")
                else -> Triple(y, "11014", "분기보고서")
            }
        } else when {
            reportNm.contains("반기") -> Triple(year.toString(), "11012", "반기보고서")
            reportNm.contains("사업보고서") -> Triple((if (month <= 4) year - 1 else year).toString(), "11011", "사업보고서")
            reportNm.contains("3분기") -> Triple(year.toString(), "11014", "3분기보고서")
            reportNm.contains("분기") -> Triple(year.toString(), "11013", "1분기보고서")
            // 보고서명 단서가 전혀 없을 때 제출월 기반 최후 추정
            month in 4..5 -> Triple(year.toString(), "11013", "1분기보고서")
            month in 7..8 -> Triple(year.toString(), "11012", "반기보고서")
            month in 10..11 -> Triple(year.toString(), "11014", "3분기보고서")
            month in 1..3 -> Triple((year - 1).toString(), "11011", "사업보고서")
            else -> Triple(year.toString(), "11012", "분기/반기보고서")
        }

        // 연결재무제표(CFS) 우선 조회 후, 없으면 개별재무제표(OFS)로 폴백.
        for (fsDiv in listOf("CFS", "OFS")) {
            val parsed = parseFinancialAccounts(resolvedKey, corpCode, bsnsYear, reprtCode, reportName, fsDiv)
            if (parsed != null) return@withContext parsed
        }

        // 정형 파싱 실패 시: Gemini에게 데이터 없음을 알리는 JSON 반환
        return@withContext JSONObject().apply {
            put("corp_code", corpCode)
            put("rcept_dt", rceptDt)
            put("error", "재무제표 API데이터 없음 - 잠정실적 등 비정형 공시일 수 있음")
            put("hint", "직접 DART 공시 원문에서 실적 내용 파악 요청")
        }.toString()
    }

    /**
     * DART 단일회사 전체 재무제표 API를 fs_div(CFS/OFS)별로 조회·파싱합니다.
     * 매출액/영업이익/당기순이익이 모두 0이거나 데이터가 없으면 null을 반환(폴백 유도).
     */
    private fun parseFinancialAccounts(
        key: String,
        corpCode: String,
        bsnsYear: String,
        reprtCode: String,
        reportName: String,
        fsDiv: String
    ): String? {
        val url = "https://opendart.fss.or.kr/api/fnlttSinglAcntAll.json?crtfc_key=$key&corp_code=$corpCode&bsns_year=$bsnsYear&reprt_code=$reprtCode&fs_div=$fsDiv"
        try {
            val responseJson = Jsoup.connect(url).ignoreContentType(true).timeout(8000).execute().body()
            val root = JSONObject(responseJson)
            if (root.optString("status") != "000") return null
            val list = root.optJSONArray("list") ?: return null
            if (list.length() == 0) return null

            var revenueCurrent = 0L; var revenuePrevious = 0L
            var opProfitCurrent = 0L; var opProfitPrevious = 0L
            var netIncomeCurrent = 0L; var netIncomePrevious = 0L
            var companyName = ""

            for (i in 0 until list.length()) {
                val row = list.getJSONObject(i)
                companyName = row.optString("corp_name", "")
                val accName = row.optString("account_nm", "").trim().replace(" ", "")
                val currentVal = row.optString("thstrm_amount", "0").replace(",", "").toLongOrNull() ?: 0L
                val previousVal = row.optString("frmtrm_amount", "0").replace(",", "").toLongOrNull() ?: 0L

                if (accName in setOf("매출액", "매출", "영업수익", "영업매출", "매출수익", "수익(매출액)", "영업수익(매출액)")) {
                    revenueCurrent = currentVal; revenuePrevious = previousVal
                } else if (accName in setOf("영업이익", "영업이익(손실)", "영업손실")) {
                    opProfitCurrent = currentVal; opProfitPrevious = previousVal
                } else if (accName in setOf("당기순이익", "당기순이익(손실)", "분기순이익", "반기순이익")) {
                    netIncomeCurrent = currentVal; netIncomePrevious = previousVal
                }
            }

            // 핵심 3계정이 모두 0이면 의미 없는(매핑 실패) 데이터로 보고 폴백 유도
            if (revenueCurrent == 0L && opProfitCurrent == 0L && netIncomeCurrent == 0L) return null

            return JSONObject().apply {
                put("company", companyName.ifBlank { corpCode })
                put("year", bsnsYear)
                put("report", reportName)
                put("fs_div", if (fsDiv == "CFS") "연결" else "개별")
                put("revenue", JSONObject().put("current", revenueCurrent).put("previous", revenuePrevious))
                put("operating_profit", JSONObject().put("current", opProfitCurrent).put("previous", opProfitPrevious))
                put("net_income", JSONObject().put("current", netIncomeCurrent).put("previous", netIncomePrevious))
            }.toString()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Financial fetch failed ($fsDiv) for $corpCode: ${e.message}")
            return null
        }
    }

    // ===================== 과거 실적 조회 (A) =====================
    private val financialHistoryCacheFile: File
        get() = File(context.filesDir, "financial_history_cache.json")
    private val historyTtlMillis = 3L * 24 * 60 * 60 * 1000 // 3일

    /**
     * 특정 회사(corpCode)의 최근 정기보고서 실적을 최신→과거 순으로 최대 maxPeriods개 조회한다.
     * 아직 제출되지 않은(미래) 보고서나 데이터 없는 보고서는 자동으로 건너뛴다.
     * 분기·반기 수치는 DART상 누적(YTD)이며 라벨에 '(누적)'으로 표기한다. 결과는 캐시(3일).
     */
    suspend fun fetchFinancialHistory(
        corpCode: String,
        apiKey: String,
        maxPeriods: Int = 8,
        forceRefresh: Boolean = false
    ): List<FinancialPeriod> = withContext(Dispatchers.IO) {
        if (corpCode.isBlank()) return@withContext emptyList()
        if (!forceRefresh) {
            loadHistoryCache(corpCode)?.let { return@withContext it }
        }
        val resolvedKey = apiKey.ifBlank { com.kitwlshcom.kdailyutil.BuildConfig.DART_DEFAULT_KEY }
        val curYear = Calendar.getInstance().get(Calendar.YEAR)

        // (연도, reprt_code, 라벨) — 연도 내림차순 + 최신 보고서 우선
        val candidates = mutableListOf<Triple<String, String, String>>()
        for (y in curYear downTo (curYear - 2)) {
            candidates.add(Triple(y.toString(), "11011", "$y 사업(연간)"))
            candidates.add(Triple(y.toString(), "11014", "$y 3분기(누적)"))
            candidates.add(Triple(y.toString(), "11012", "$y 반기(누적)"))
            candidates.add(Triple(y.toString(), "11013", "$y 1분기"))
        }

        val result = mutableListOf<FinancialPeriod>()
        for ((year, reprt, label) in candidates) {
            if (result.size >= maxPeriods) break
            var parsed: String? = null
            for (fsDiv in listOf("CFS", "OFS")) {
                parsed = parseFinancialAccounts(resolvedKey, corpCode, year, reprt, label, fsDiv)
                if (parsed != null) break
            }
            if (parsed == null) continue
            try {
                val o = JSONObject(parsed)
                result.add(
                    FinancialPeriod(
                        year = year,
                        reprtCode = reprt,
                        reportLabel = label,
                        fsDiv = o.optString("fs_div", "연결"),
                        revenue = o.optJSONObject("revenue")?.optLong("current") ?: 0L,
                        operatingProfit = o.optJSONObject("operating_profit")?.optLong("current") ?: 0L,
                        netIncome = o.optJSONObject("net_income")?.optLong("current") ?: 0L,
                        revenuePrev = o.optJSONObject("revenue")?.optLong("previous") ?: 0L,
                        operatingProfitPrev = o.optJSONObject("operating_profit")?.optLong("previous") ?: 0L,
                        netIncomePrev = o.optJSONObject("net_income")?.optLong("previous") ?: 0L
                    )
                )
            } catch (e: Exception) { /* skip malformed */ }
        }
        if (result.isNotEmpty()) saveHistoryCache(corpCode, result)
        result
    }

    private fun loadHistoryCache(corpCode: String): List<FinancialPeriod>? {
        if (!financialHistoryCacheFile.exists()) return null
        return try {
            val root = JSONObject(financialHistoryCacheFile.readText(StandardCharsets.UTF_8))
            val entry = root.optJSONObject(corpCode) ?: return null
            if (System.currentTimeMillis() - entry.optLong("ts") > historyTtlMillis) return null
            val arr = entry.optJSONArray("periods") ?: return null
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                FinancialPeriod(
                    o.optString("year"), o.optString("reprtCode"), o.optString("reportLabel"), o.optString("fsDiv"),
                    o.optLong("revenue"), o.optLong("operatingProfit"), o.optLong("netIncome"),
                    o.optLong("revenuePrev"), o.optLong("operatingProfitPrev"), o.optLong("netIncomePrev")
                )
            }
        } catch (e: Exception) { null }
    }

    private fun saveHistoryCache(corpCode: String, periods: List<FinancialPeriod>) {
        try {
            val root = if (financialHistoryCacheFile.exists())
                JSONObject(financialHistoryCacheFile.readText(StandardCharsets.UTF_8)) else JSONObject()
            val arr = JSONArray()
            periods.forEach { p ->
                arr.put(JSONObject().apply {
                    put("year", p.year); put("reprtCode", p.reprtCode); put("reportLabel", p.reportLabel); put("fsDiv", p.fsDiv)
                    put("revenue", p.revenue); put("operatingProfit", p.operatingProfit); put("netIncome", p.netIncome)
                    put("revenuePrev", p.revenuePrev); put("operatingProfitPrev", p.operatingProfitPrev); put("netIncomePrev", p.netIncomePrev)
                })
            }
            root.put(corpCode, JSONObject().put("ts", System.currentTimeMillis()).put("periods", arr))
            financialHistoryCacheFile.writeText(root.toString(), StandardCharsets.UTF_8)
        } catch (e: Exception) { Log.e(TAG, "history 캐시 저장 실패: ${e.message}") }
    }

    // ===================== 회사 이름 검색 (C, corpCode.xml) =====================
    private val corpCodeCacheFile: File
        get() = File(context.filesDir, "corp_codes.json")

    /** DART 전체 고유번호 파일(zip)을 1회 받아 상장사(stock_code 존재)만 로컬 캐시로 저장. */
    suspend fun ensureCorpCodes(apiKey: String): Boolean = withContext(Dispatchers.IO) {
        if (corpCodeCacheFile.exists() && corpCodeCacheFile.length() > 100) return@withContext true
        val resolvedKey = apiKey.ifBlank { com.kitwlshcom.kdailyutil.BuildConfig.DART_DEFAULT_KEY }
        return@withContext try {
            val url = "https://opendart.fss.or.kr/api/corpCode.xml?crtfc_key=$resolvedKey"
            val bytes = Jsoup.connect(url).ignoreContentType(true).maxBodySize(0).timeout(30000).execute().bodyAsBytes()
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name.endsWith(".xml", ignoreCase = true)) {
                        val xml = zip.readBytes().toString(Charsets.UTF_8)
                        val listed = parseCorpCodeXml(xml)
                        val arr = JSONArray()
                        listed.forEach { arr.put(JSONObject().put("c", it.corpCode).put("n", it.corpName).put("s", it.stockCode)) }
                        corpCodeCacheFile.writeText(arr.toString(), StandardCharsets.UTF_8)
                        Log.d(TAG, "✅ corpCode 캐시 저장: ${listed.size}개 상장사")
                        return@withContext true
                    }
                    entry = zip.nextEntry
                }
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "❌ corpCode 다운로드 실패: ${e.message}")
            false
        }
    }

    private fun parseCorpCodeXml(xml: String): List<CorpEntry> {
        val result = ArrayList<CorpEntry>(4000)
        try {
            val parser = android.util.Xml.newPullParser()
            parser.setInput(StringReader(xml))
            var event = parser.eventType
            var cur = ""
            var corpCode = ""; var corpName = ""; var stockCode = ""
            while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                when (event) {
                    org.xmlpull.v1.XmlPullParser.START_TAG -> cur = parser.name
                    org.xmlpull.v1.XmlPullParser.TEXT -> {
                        val t = parser.text?.trim() ?: ""
                        when (cur) {
                            "corp_code" -> corpCode = t
                            "corp_name" -> corpName = t
                            "stock_code" -> stockCode = t
                        }
                    }
                    org.xmlpull.v1.XmlPullParser.END_TAG -> {
                        if (parser.name == "list") {
                            if (stockCode.isNotBlank() && corpName.isNotBlank()) result.add(CorpEntry(corpCode, corpName, stockCode))
                            corpCode = ""; corpName = ""; stockCode = ""
                        }
                        cur = ""
                    }
                }
                event = parser.next()
            }
        } catch (e: Exception) { Log.e(TAG, "corpCode XML 파싱 실패: ${e.message}") }
        return result
    }

    /** 캐시된 상장사 목록에서 회사명으로 검색. (정확/시작 일치 우선) */
    suspend fun searchCorpByName(query: String, apiKey: String, limit: Int = 30): List<CorpEntry> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isBlank()) return@withContext emptyList()
        if (!ensureCorpCodes(apiKey)) return@withContext emptyList()
        return@withContext try {
            val arr = JSONArray(corpCodeCacheFile.readText(StandardCharsets.UTF_8))
            val out = ArrayList<CorpEntry>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val name = o.optString("n")
                if (name.contains(q)) {
                    out.add(CorpEntry(o.optString("c"), name, o.optString("s")))
                }
            }
            out.sortedWith(
                compareByDescending<CorpEntry> { it.corpName == q }
                    .thenByDescending { it.corpName.startsWith(q) }
                    .thenBy { it.corpName.length }
            ).take(limit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 회사 검색 실패: ${e.message}")
            emptyList()
        }
    }

    /**
     * 정기보고서 법정 제출기한(12월 결산 기준)으로 역산한 "예상 실적 발표 일정"을 반환합니다.
     *
     * ⚠️ 한국은 미국과 달리 정확한 실적 발표 예정일·컨센서스를 무료 공개 API로 제공하지 않습니다.
     * 따라서 가짜 예시 대신, 법으로 정해진 보고서 제출기한(분기말+45일, 사업연도말+90일)을 기준으로
     * 다가오는 발표 시즌을 안내합니다. (실제 발표는 기한보다 이를 수 있음)
     *
     * @param watchNames 관심종목명 목록. DART 고유번호가 매핑된 한국 종목만 표시 대상이 됩니다.
     */
    suspend fun fetchExpectedEarnings(watchNames: List<String> = emptyList()): List<ExpectedEarnings> = withContext(Dispatchers.IO) {
        val (nextDate, reportType) = nextStatutoryDeadline()

        // 관심종목 중 DART에 매핑된 한국 종목만, 없으면 대표 종목으로 폴백
        val koreanWatch = watchNames.filter { CORP_CODE_MAP.containsKey(it) }.distinct()
        val targets = koreanWatch.ifEmpty {
            listOf("삼성전자", "SK하이닉스", "현대차", "기아", "네이버", "카카오")
        }

        val cachedReports = loadExpectedReports()
        return@withContext targets.map { name ->
            ExpectedEarnings(
                corp_name = name,
                release_date = nextDate,
                consensus_revenue = reportType,
                consensus_profit = "법정 제출기한",
                aiReport = cachedReports[name]
            )
        }
    }

    // ── AI 사전 전망 리포트 영속 캐시 (corp_name → report) ──────────────
    private val expectedReportCacheFile: File
        get() = File(context.filesDir, "expected_reports_cache.json")

    @Synchronized
    fun loadExpectedReports(): Map<String, String> {
        if (!expectedReportCacheFile.exists()) return emptyMap()
        return try {
            val obj = JSONObject(expectedReportCacheFile.readText(StandardCharsets.UTF_8))
            obj.keys().asSequence().associateWith { obj.getString(it) }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load expected reports cache: ${e.message}")
            emptyMap()
        }
    }

    @Synchronized
    fun saveExpectedReport(corpName: String, report: String) {
        try {
            val obj = if (expectedReportCacheFile.exists())
                JSONObject(expectedReportCacheFile.readText(StandardCharsets.UTF_8)) else JSONObject()
            obj.put(corpName, report)
            expectedReportCacheFile.writeText(obj.toString(), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save expected report: ${e.message}")
        }
    }

    // ── 즐겨찾기 / 숨김 공시 영속 저장 ──────────────────────────────
    private val favoritesFile: File get() = File(context.filesDir, "favorite_disclosures.json")
    private val hiddenFile: File get() = File(context.filesDir, "hidden_disclosures.json")

    private fun disclosureToJson(item: EarningsDisclosure): JSONObject = JSONObject().apply {
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

    private fun jsonToDisclosure(obj: JSONObject): EarningsDisclosure = EarningsDisclosure(
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

    @Synchronized
    fun loadFavorites(): List<EarningsDisclosure> {
        if (!favoritesFile.exists()) return emptyList()
        return try {
            val arr = JSONArray(favoritesFile.readText(StandardCharsets.UTF_8))
            (0 until arr.length()).map { jsonToDisclosure(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load favorites: ${e.message}"); emptyList()
        }
    }

    @Synchronized
    fun saveFavorites(list: List<EarningsDisclosure>) {
        try {
            val arr = JSONArray()
            list.forEach { arr.put(disclosureToJson(it)) }
            favoritesFile.writeText(arr.toString(), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save favorites: ${e.message}")
        }
    }

    @Synchronized
    fun loadHidden(): Set<String> {
        if (!hiddenFile.exists()) return emptySet()
        return try {
            val arr = JSONArray(hiddenFile.readText(StandardCharsets.UTF_8))
            (0 until arr.length()).map { arr.getString(it) }.toSet()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load hidden: ${e.message}"); emptySet()
        }
    }

    @Synchronized
    fun saveHidden(ids: Set<String>) {
        try {
            val arr = JSONArray()
            ids.forEach { arr.put(it) }
            hiddenFile.writeText(arr.toString(), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save hidden: ${e.message}")
        }
    }

    /**
     * 오늘 이후 가장 가까운 정기보고서 법정 제출기한과 보고서 종류를 반환합니다. (12월 결산 기준)
     */
    private fun nextStatutoryDeadline(): Pair<String, String> {
        val today = Calendar.getInstance()
        val sdf = SimpleDateFormat("MM.dd", Locale.getDefault())
        // (월, 일, 보고서명)
        val deadlines = listOf(
            Triple(3, 31, "사업보고서"),
            Triple(5, 15, "1분기보고서"),
            Triple(8, 14, "반기보고서"),
            Triple(11, 14, "3분기보고서")
        )
        val baseYear = today.get(Calendar.YEAR)
        val candidates = mutableListOf<Pair<Calendar, String>>()
        for (y in listOf(baseYear, baseYear + 1)) {
            for ((m, d, label) in deadlines) {
                val c = Calendar.getInstance()
                c.set(y, m - 1, d, 0, 0, 0)
                candidates.add(c to label)
            }
        }
        val next = candidates
            .filter { it.first.timeInMillis >= today.timeInMillis }
            .minByOrNull { it.first.timeInMillis }
            ?: candidates.first()
        return sdf.format(next.first.time) to next.second
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
            //    주의: 'dd'(일)이어야 함. 'DD'(연중 일수)면 날짜가 1월로 잘못 파싱돼 유효 캐시가 오삭제됨.
            val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
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

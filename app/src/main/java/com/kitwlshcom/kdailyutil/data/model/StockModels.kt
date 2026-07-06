package com.kitwlshcom.kdailyutil.data.model

/**
 * 차트 기간 선택 Enum
 * Yahoo Finance API interval/range 파라미터와 직접 대응
 */
enum class ChartRange(val label: String, val interval: String, val range: String) {
    TODAY("당일", "1m", "1d"),
    WEEK("1주", "15m", "5d"),
    MONTH("1개월", "1d", "1mo"),
    THREE_MONTHS("3개월", "1d", "3mo")
}

/**
 * 기간별 차트 데이터 모델
 */
data class StockChartData(
    val symbol: String,
    val name: String,
    val prices: List<Float>,
    val timestamps: List<Long>,  // Unix epoch (seconds)
    val range: ChartRange,
    val volumes: List<Long> = emptyList()  // 각 포인트의 거래량 (크로스헤어 표시용)
)

/**
 * 주가/지수 통화 타입
 *  - KRW   : 한국 원화 종목 (.KS / .KQ)         → ₩ 표시
 *  - USD   : 미국 달러 종목 (US 주식, 암호화폐)   → $ 표시
 *  - INDEX : 주가지수 (코스피, 나스닥 등 ^로 시작) → 단위 없이 숫자만 (pt 병기)
 */
enum class CurrencyType { KRW, USD, INDEX }

data class StockPriceItem(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val changeAmount: Double = 0.0,
    val sparkline: List<Float> = emptyList(),
    val updateTime: String = "",
    val delayInfo: String = "15분+ 지연",
    val currencyType: CurrencyType = CurrencyType.USD  // 기본값 USD (레거시 호환)
)


data class EarningsDisclosure(
    val rcept_no: String,
    val corp_code: String,
    val corp_name: String,
    val report_nm: String,
    val flr_nm: String,
    val rcept_dt: String,
    var aiSummary: String? = null,
    var isSurprise: Boolean? = null, // True: 서프라이즈 (상승), False: 쇼크 (하락), null: 부합/기타
    var isTurnaround: Boolean = false, // 적자 -> 흑자 전환 여부
    var isFavorite: Boolean = false   // 즐겨찾기 여부 (날짜창 밖에서도 계속 표시)
)

data class ExpectedEarnings(
    val corp_name: String,
    val release_date: String,
    val consensus_revenue: String = "-",
    val consensus_profit: String = "-",
    var aiReport: String? = null
)

/** 특정 회사의 한 보고서(분기/반기/사업) 실적. 분기·반기 수치는 DART상 누적(YTD)임. */
data class FinancialPeriod(
    val year: String,
    val reprtCode: String,
    val reportLabel: String,   // 예: "2025 반기(누적)"
    val fsDiv: String,         // "연결" | "개별"
    val revenue: Long,
    val operatingProfit: Long,
    val netIncome: Long,
    val revenuePrev: Long = 0L,
    val operatingProfitPrev: Long = 0L,
    val netIncomePrev: Long = 0L
)

/** DART 전체 고유번호(corpCode.xml)에서 검색된 상장사 항목. */
data class CorpEntry(
    val corpCode: String,
    val corpName: String,
    val stockCode: String
)

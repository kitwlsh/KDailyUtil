package com.kitwlshcom.kdailyutil.data.model

data class StockPriceItem(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val sparkline: List<Float> = emptyList()
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
    var isTurnaround: Boolean = false // 적자 -> 흑자 전환 여부
)

data class ExpectedEarnings(
    val corp_name: String,
    val release_date: String,
    val consensus_revenue: String = "-",
    val consensus_profit: String = "-",
    var aiReport: String? = null
)

package com.kitwlshcom.kdailyutil.data.disclosure

/**
 * 공시 갈래 — 제목(`report_nm`) 키워드로 나눈다. → [doc/FEATURE_DISCLOSURE_ALERT.md §3-1]
 *
 * 🔴 **갈래 이름에 «호재/악재»를 쓰지 않는다** — 같은 유상증자도 회사마다 뜻이 다르다.
 * 이 앱은 «사실을 전한다»(§8). 판단은 하지 않는다.
 *
 * 🟢 **P1은 키워드를 코드 상수로 둔다**(2026-11-11 사용자 결정 ④ — 원격 JSON은 나중).
 * 키워드는 2026-11-10 하루치 실제 DART 제목(약 300건)을 훑어 맞췄다.
 * 그날 가장 많았던 «임원ㆍ주요주주특정증권등소유상황보고서»(30건)·«주식등의대량보유상황보고서»(34건)는
 * **일부러 [OTHER]다** — 이것까지 알리면 알림이 소음이 된다(내부자 지분은 P3에서 따로 본다).
 *
 * 순서가 곧 우선순위다 — 위에서부터 처음 걸리는 갈래로 정한다(위험을 가장 먼저 본다).
 */
enum class DisclosureCategory(
    val label: String,
    val badge: String,
    /** 처음 켰을 때 알림을 받는 갈래인가. */
    val defaultOn: Boolean,
    /** 공백을 지운 제목에 이 중 하나라도 들어 있으면 이 갈래다. */
    private val keywords: List<String>
) {
    RISK(
        "희석·위험", "🔴", true,
        listOf(
            "유상증자", "전환사채권발행결정", "신주인수권부사채권발행결정", "교환사채권발행결정",
            "감자결정", "관리종목", "불성실공시", "상장폐지", "감사의견", "조회공시요구",
            "투자주의", "투자경고", "투자위험", "매매거래정지", "자본잠식", "회생절차", "파산",
            "횡령", "배임", "생산중단", "영업정지", "소송등의제기"
        )
    ),
    STRUCTURE(
        "구조 변화", "🔵", true,
        listOf(
            "최대주주변경", "타법인주식및출자증권처분", "타법인주식및출자증권취득",
            "유형자산양도", "유형자산처분", "유형자산양수", "영업양도", "영업양수",
            "합병", "분할", "공개매수", "주식교환", "대표이사변경"
        )
    ),
    RETURN(
        "주주환원", "🟢", true,
        listOf("자기주식취득", "자기주식소각", "주식소각결정", "배당결정")
    ),
    EARNINGS(
        "실적", "🟡", true,
        listOf("영업(잠정)실적", "매출액또는손익구조", "분기보고서", "반기보고서", "사업보고서")
    ),
    BUSINESS(
        "사업", "⚪", false,
        listOf("단일판매ㆍ공급계약", "단일판매·공급계약", "신규시설투자", "시설외투자", "투자판단관련주요경영사항")
    ),
    /** 위에 안 걸린 것 — 알림은 없고 기록에도 남기지 않는다. */
    OTHER("기타", "·", false, emptyList());

    companion object {
        /** 알림 대상이 될 수 있는 갈래(설정 화면의 체크 목록). */
        val selectable: List<DisclosureCategory> = entries.filter { it != OTHER }

        val defaults: Set<DisclosureCategory> = entries.filter { it.defaultOn }.toSet()

        fun classify(reportNm: String): DisclosureCategory {
            val t = normalize(reportNm)
            return entries.firstOrNull { c -> c.keywords.any { t.contains(it) } } ?: OTHER
        }

        /**
         * `[기재정정]`·`[첨부정정]`·`[발행조건확정]` 같은 머리말이 붙은 공시.
         * 원 공시의 갈래를 따르되, 받을지는 «정정공시» 설정이 따로 정한다(기본 끔).
         */
        fun isCorrection(reportNm: String): Boolean =
            CORRECTION_PREFIX.containsMatchIn(reportNm.trimStart())

        /** 화면·알림에 보일 제목 — DART 제목 끝의 긴 공백 패딩을 한 칸으로 줄인다. */
        fun displayTitle(reportNm: String): String = reportNm.trim().replace(Regex("\\s{2,}"), " ")

        fun fromName(name: String): DisclosureCategory? = entries.firstOrNull { it.name == name }

        private fun normalize(s: String) = s.replace(Regex("\\s+"), "")

        private val CORRECTION_PREFIX = Regex("^\\[[^\\]]*(정정|확정)\\]")
    }
}

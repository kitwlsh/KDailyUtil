package com.kitwlshcom.kdailyutil.data.disclosure

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** DART `list.json`의 한 줄 중 알림에 필요한 것만. */
data class DisclosureItem(
    val rceptNo: String,
    val corpCode: String,
    val corpName: String,
    val reportNm: String,
    /** YYYYMMDD */
    val rceptDt: String
)

/** 알림에 실릴 한 건(갈래가 정해진 뒤). */
data class AlertEntry(
    val item: DisclosureItem,
    val category: DisclosureCategory
)

/** 알림 한 장. [corpCode]가 null이면 여러 종목을 묶은 요약이다. */
data class AlertNotice(
    val corpCode: String?,
    val title: String,
    val text: String
)

/**
 * 관심종목 공시 알림의 **판단 규칙** — 안드로이드 API를 쓰지 않는 순수 함수만 둔다(단위 테스트 대상).
 * 워커([DisclosureAlertWorker])는 이 규칙대로 «부르고 · 저장하고 · 띄우기»만 한다.
 */
object DisclosureAlertPolicy {

    /**
     * 확인 시각 — 하루 4번 · **고정**(2026-11-11 사용자 결정 ⑤ · 요청이 있으면 연다).
     * 공시는 장 마감 뒤(15:30~18:00)에 몰린다 → 16:30 · 19:30이 그날 치를 대부분 잡는다.
     */
    val SLOTS: List<LocalTime> = listOf(
        LocalTime.of(8, 30), LocalTime.of(12, 30), LocalTime.of(16, 30), LocalTime.of(19, 30)
    )

    /**
     * 한 번에 조회하는 국내 종목 상한 — 공용 DART 키 보호(사용자 결정 ② = §7 B안).
     * 10종목 × 하루 4번 = 사용자당 하루 40회.
     */
    const val MAX_CORPS_PER_RUN = 10

    /** 조회 창(일). 주말을 건너 월요일 아침에도 금요일 저녁 공시를 잡도록 넉넉히 둔다 — 중복은 seen이 거른다. */
    const val LOOKBACK_DAYS = 4L

    /** «이미 본 공시»·알림 기록을 얼마나 들고 있나. 조회 창보다 길어야 같은 공시가 다시 오지 않는다. */
    const val KEEP_DAYS = 30L

    /** 한 회차에 종목이 이보다 많으면 한 장으로 묶는다(§4-5). */
    const val GROUP_OVER_CORPS = 3

    fun isBusinessDay(d: LocalDate): Boolean =
        d.dayOfWeek != DayOfWeek.SATURDAY && d.dayOfWeek != DayOfWeek.SUNDAY

    /** [now] 이전(같거나 앞)의 가장 최근 확인 시각. 주말이면 건너뛰고, 오늘 첫 시각 전이면 직전 영업일 마지막 시각. */
    fun latestSlot(now: LocalDateTime): LocalDateTime {
        var day = now.toLocalDate()
        SLOTS.lastOrNull { !it.isAfter(now.toLocalTime()) }
            ?.takeIf { isBusinessDay(day) }
            ?.let { return day.atTime(it) }
        do { day = day.minusDays(1) } while (!isBusinessDay(day))
        return day.atTime(SLOTS.last())
    }

    /**
     * 지금 확인할 차례인가 — **WorkManager가 30분마다 깨워도 실제 조회는 이 판정을 통과할 때만** 한다.
     *
     * 🔴 정해진 시각에 정확히 깨우는 대신 «지난 시각 이후로 아직 안 봤으면 본다»로 판정한다.
     * 그래야 Doze·재부팅·네트워크 대기로 몇 번 건너뛰어도 **다음에 깨는 순간 따라잡고**, 같은 시각을 두 번 보지 않는다.
     * 주말에는 조회하지 않는다(금요일 19:30 이후에 처음 깨면 그 시각 치를 한 번 본다).
     */
    fun isDue(now: LocalDateTime, lastCheck: LocalDateTime?): Boolean =
        lastCheck == null || lastCheck.isBefore(latestSlot(now))

    /** 다음 확인 시각(설정 화면 안내용). */
    fun nextSlot(now: LocalDateTime): LocalDateTime {
        var day = now.toLocalDate()
        if (isBusinessDay(day)) {
            SLOTS.firstOrNull { it.isAfter(now.toLocalTime()) }?.let { return day.atTime(it) }
        }
        do { day = day.plusDays(1) } while (!isBusinessDay(day))
        return day.atTime(SLOTS.first())
    }

    /**
     * 새로 받은 공시 중 **알릴 것**만 고른다.
     *
     * @param seen 이미 본 접수번호(알렸든 걸렀든). 여기 있으면 다시 알리지 않는다.
     * @param enabled 사용자가 켠 갈래.
     * @param includeCorrections `[기재정정]` 등도 받을지.
     */
    fun select(
        fresh: List<DisclosureItem>,
        seen: Set<String>,
        enabled: Set<DisclosureCategory>,
        includeCorrections: Boolean
    ): List<AlertEntry> =
        fresh.asSequence()
            .filter { it.rceptNo.isNotBlank() && it.rceptNo !in seen }
            .distinctBy { it.rceptNo }
            .filter { includeCorrections || !DisclosureCategory.isCorrection(it.reportNm) }
            .map { AlertEntry(it, DisclosureCategory.classify(it.reportNm)) }
            .filter { it.category != DisclosureCategory.OTHER && it.category in enabled }
            .toList()

    /**
     * 알림 장 수를 정한다 — **종목당 1장**, 종목이 [GROUP_OVER_CORPS]보다 많으면 **한 장으로 묶는다**.
     *
     * 🔴 문구에 «매수·매도·신호·호재·악재»를 쓰지 않는다(§8). 갈래 이름과 공시 제목만 말한다.
     */
    fun buildNotices(entries: List<AlertEntry>): List<AlertNotice> {
        if (entries.isEmpty()) return emptyList()
        val byCorp = entries.groupBy { it.item.corpCode }
        if (byCorp.size > GROUP_OVER_CORPS) {
            val first = entries.first().item.corpName
            return listOf(
                AlertNotice(
                    corpCode = null,
                    title = "🔔 $first 외 ${byCorp.size - 1}종목 · 공시 ${entries.size}건",
                    text = byCorp.values.joinToString(" · ") { list ->
                        "${list.first().item.corpName} ${list.size}건"
                    } + " — 탭해서 보기"
                )
            )
        }
        return byCorp.map { (code, list) ->
            val top = list.first()
            val more = if (list.size > 1) " 외 ${list.size - 1}건" else ""
            AlertNotice(
                corpCode = code,
                title = "🔔 ${top.item.corpName} · ${top.category.badge} ${top.category.label}",
                text = DisclosureCategory.displayTitle(top.item.reportNm) + more
            )
        }
    }
}

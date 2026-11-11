package com.kitwlshcom.kdailyutil

import com.kitwlshcom.kdailyutil.data.disclosure.AlertEntry
import com.kitwlshcom.kdailyutil.data.disclosure.DisclosureAlertPolicy
import com.kitwlshcom.kdailyutil.data.disclosure.DisclosureCategory
import com.kitwlshcom.kdailyutil.data.disclosure.DisclosureCategory.Companion.classify
import com.kitwlshcom.kdailyutil.data.disclosure.DisclosureItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * 🔔 관심종목 공시 알림(1.7.5) — 분류 · 확인 시각 · 선별 · 묶음 규칙.
 *
 * 🔴 제목은 **2026-11-10 실제 DART 제목**을 그대로 쓴다(끝의 긴 공백 패딩까지). 표기가 조금만 달라도
 * 갈래가 갈리기 때문이다.
 */
class DisclosureAlertTest {

    // ── 분류 ────────────────────────────────────────────────────────

    @Test
    fun `실제 제목이 기대한 갈래로 간다`() {
        val cases = mapOf(
            "[기재정정]주요사항보고서(유상증자결정)" to DisclosureCategory.RISK,
            "불성실공시법인지정              (공시불이행)" to DisclosureCategory.RISK,
            "자본잠식50%이상또는매출액50억원미만사실발생" to DisclosureCategory.RISK,
            "기타시장안내              (관리종목 지정 사유 일부 해제 관련 시장조치 미진행)" to DisclosureCategory.RISK,
            "최대주주변경" to DisclosureCategory.STRUCTURE,
            "회사합병결정" to DisclosureCategory.STRUCTURE,
            "타법인주식및출자증권취득결정" to DisclosureCategory.STRUCTURE,
            "주식소각결정" to DisclosureCategory.RETURN,
            "현금ㆍ현물배당결정              (분기배당)" to DisclosureCategory.RETURN,
            "연결재무제표기준영업(잠정)실적(공정공시)" to DisclosureCategory.EARNINGS,
            "분기보고서 (2026.09)" to DisclosureCategory.EARNINGS,
            "[기재정정]단일판매ㆍ공급계약체결" to DisclosureCategory.BUSINESS,
        )
        cases.forEach { (title, want) -> assertEquals(title, want, classify(title)) }
    }

    /** 🔴 그날 가장 많던 공시들 — 이것까지 알리면 알림이 소음이 된다. */
    @Test
    fun `매일 쏟아지는 지분 보고서는 알리지 않는다`() {
        listOf(
            "임원ㆍ주요주주특정증권등소유상황보고서",
            "주식등의대량보유상황보고서(약식)",
            "최대주주등소유주식변동신고서               ",
            "증권발행실적보고서",
            "주주총회소집공고",
            "기업설명회(IR)개최(안내공시)",
        ).forEach { assertEquals(it, DisclosureCategory.OTHER, classify(it)) }
    }

    /** «주식병합»이 «합병»에 걸리면 안 된다(글자 순서가 다르다). */
    @Test
    fun `주식병합은 합병이 아니다`() {
        assertEquals(DisclosureCategory.OTHER, classify("주식병합결정"))
    }

    @Test
    fun `정정공시를 알아본다`() {
        assertTrue(DisclosureCategory.isCorrection("[기재정정]최대주주변경"))
        assertTrue(DisclosureCategory.isCorrection("[첨부정정]분기보고서"))
        assertTrue(DisclosureCategory.isCorrection("[발행조건확정]증권신고서(채무증권)"))
        assertFalse(DisclosureCategory.isCorrection("최대주주변경"))
    }

    @Test
    fun `제목의 긴 공백 패딩을 한 칸으로 줄인다`() {
        assertEquals("현금ㆍ현물배당결정 (분기배당)", DisclosureCategory.displayTitle("현금ㆍ현물배당결정              (분기배당)"))
    }

    /** 기본으로 켜지는 갈래 = 위험·구조·환원·실적. 사업(공급계약)은 너무 잦아 끔. */
    @Test
    fun `기본 갈래`() {
        assertEquals(
            setOf(DisclosureCategory.RISK, DisclosureCategory.STRUCTURE, DisclosureCategory.RETURN, DisclosureCategory.EARNINGS),
            DisclosureCategory.defaults
        )
        assertFalse(DisclosureCategory.OTHER in DisclosureCategory.selectable)
    }

    // ── 확인 시각 ────────────────────────────────────────────────────

    private fun t(s: String) = LocalDateTime.parse(s)   // 2026-11-11 = 수요일

    @Test
    fun `가장 최근 확인 시각`() {
        assertEquals(t("2026-11-11T12:30"), DisclosureAlertPolicy.latestSlot(t("2026-11-11T13:00")))
        assertEquals(t("2026-11-11T16:30"), DisclosureAlertPolicy.latestSlot(t("2026-11-11T16:30")))
        // 오늘 첫 시각 전이면 어제 마지막
        assertEquals(t("2026-11-10T19:30"), DisclosureAlertPolicy.latestSlot(t("2026-11-11T07:00")))
        // 월요일 아침 전이면 금요일 마지막
        assertEquals(t("2026-11-13T19:30"), DisclosureAlertPolicy.latestSlot(t("2026-11-16T08:00")))
        // 토요일 낮 → 금요일 마지막
        assertEquals(t("2026-11-13T19:30"), DisclosureAlertPolicy.latestSlot(t("2026-11-14T15:00")))
    }

    /** 🔴 30분마다 깨도 한 시각에 한 번만 조회한다. */
    @Test
    fun `같은 시각을 두 번 보지 않는다`() {
        assertTrue(DisclosureAlertPolicy.isDue(t("2026-11-11T12:31"), t("2026-11-11T08:35")))
        assertFalse(DisclosureAlertPolicy.isDue(t("2026-11-11T13:01"), t("2026-11-11T12:31")))
        assertFalse(DisclosureAlertPolicy.isDue(t("2026-11-11T16:29"), t("2026-11-11T12:31")))
        assertTrue(DisclosureAlertPolicy.isDue(t("2026-11-11T16:30"), t("2026-11-11T12:31")))
    }

    /** 폰이 몇 시간 꺼져 있었으면 다음에 깰 때 한 번만 따라잡는다. */
    @Test
    fun `건너뛴 시각은 한 번에 따라잡는다`() {
        assertTrue(DisclosureAlertPolicy.isDue(t("2026-11-11T20:00"), t("2026-11-11T08:31")))
        assertFalse(DisclosureAlertPolicy.isDue(t("2026-11-11T20:30"), t("2026-11-11T20:00")))
    }

    @Test
    fun `주말에는 조회하지 않는다`() {
        // 금요일 19:30 뒤에 한 번 봤으면 토·일 내내 조용하다
        assertFalse(DisclosureAlertPolicy.isDue(t("2026-11-14T12:30"), t("2026-11-13T19:35")))
        assertFalse(DisclosureAlertPolicy.isDue(t("2026-11-15T19:30"), t("2026-11-13T19:35")))
        // 월요일 08:30이 되면 다시
        assertTrue(DisclosureAlertPolicy.isDue(t("2026-11-16T08:30"), t("2026-11-13T19:35")))
    }

    @Test
    fun `처음에는 바로 확인한다`() {
        assertTrue(DisclosureAlertPolicy.isDue(t("2026-11-11T09:00"), null))
    }

    @Test
    fun `다음 확인 시각`() {
        assertEquals(t("2026-11-11T16:30"), DisclosureAlertPolicy.nextSlot(t("2026-11-11T13:00")))
        assertEquals(t("2026-11-12T08:30"), DisclosureAlertPolicy.nextSlot(t("2026-11-11T19:30")))
        assertEquals(t("2026-11-16T08:30"), DisclosureAlertPolicy.nextSlot(t("2026-11-13T20:00")))
    }

    // ── 선별 ────────────────────────────────────────────────────────

    private fun item(no: String, title: String, corp: String = "00126380", name: String = "삼성전자") =
        DisclosureItem(no, corp, name, title, "20261111")

    private val all = DisclosureCategory.selectable.toSet()

    @Test
    fun `이미 본 공시는 다시 알리지 않는다`() {
        val got = DisclosureAlertPolicy.select(
            listOf(item("1", "최대주주변경"), item("2", "회사합병결정")),
            seen = setOf("1"), enabled = all, includeCorrections = false
        )
        assertEquals(listOf("2"), got.map { it.item.rceptNo })
    }

    @Test
    fun `끈 갈래와 기타는 알리지 않는다`() {
        val got = DisclosureAlertPolicy.select(
            listOf(
                item("1", "최대주주변경"),
                item("2", "단일판매ㆍ공급계약체결"),
                item("3", "임원ㆍ주요주주특정증권등소유상황보고서")
            ),
            seen = emptySet(), enabled = DisclosureCategory.defaults, includeCorrections = false
        )
        assertEquals(listOf("1"), got.map { it.item.rceptNo })
    }

    @Test
    fun `정정공시는 설정을 켰을 때만`() {
        val list = listOf(item("1", "[기재정정]최대주주변경"))
        assertTrue(DisclosureAlertPolicy.select(list, emptySet(), all, includeCorrections = false).isEmpty())
        assertEquals(1, DisclosureAlertPolicy.select(list, emptySet(), all, includeCorrections = true).size)
    }

    @Test
    fun `같은 접수번호가 두 번 와도 한 번만`() {
        val got = DisclosureAlertPolicy.select(
            listOf(item("1", "최대주주변경"), item("1", "최대주주변경")), emptySet(), all, false
        )
        assertEquals(1, got.size)
    }

    // ── 알림 묶음 ────────────────────────────────────────────────────

    private fun entry(no: String, corp: String, name: String, title: String = "최대주주변경") =
        AlertEntry(item(no, title, corp, name), classify(title))

    @Test
    fun `종목당 한 장 · 여러 건이면 외 N건`() {
        val notices = DisclosureAlertPolicy.buildNotices(
            listOf(entry("1", "A", "가나"), entry("2", "A", "가나", "회사합병결정"), entry("3", "B", "다라"))
        )
        assertEquals(2, notices.size)
        assertEquals("A", notices[0].corpCode)
        assertTrue(notices[0].title, notices[0].title.contains("가나") && notices[0].title.contains("구조 변화"))
        assertTrue(notices[0].text, notices[0].text.endsWith("외 1건"))
    }

    @Test
    fun `종목이 많으면 한 장으로 묶는다`() {
        val notices = DisclosureAlertPolicy.buildNotices(
            (1..4).map { entry("$it", "C$it", "회사$it") } + entry("9", "C1", "회사1")
        )
        assertEquals(1, notices.size)
        assertNull(notices[0].corpCode)
        assertTrue(notices[0].title, notices[0].title.contains("외 3종목") && notices[0].title.contains("공시 5건"))
    }

    // ── 🤖 3줄 해설 (P2) ──────────────────────────────────────────────

    private val explain = com.kitwlshcom.kdailyutil.data.disclosure.DisclosureExplain

    @Test
    fun `해설 프롬프트는 금지 조항과 입력을 담는다`() {
        val p = explain.prompt("삼성전자", DisclosureCategory.RETURN, "주식소각결정   ", "20261111")
        listOf("삼성전자", "주식소각결정", "주주환원", "권하지 말 것", "만들지 말 것", "무슨 일:", "확인할 점:")
            .forEach { assertTrue("«$it» 빠짐", p.contains(it)) }
    }

    @Test
    fun `세 줄 형식을 정리한다 · 마크다운과 군더더기는 떨어진다`() {
        val raw = "다음은 해설입니다.\n- **무슨 일:** 자기주식 소각 결정\n* 무슨 뜻: 주식 수가 줄어 주당 가치가 오를 수 있음\n확인할 점: 소각 규모(공시 원문 확인 필요)\n"
        assertEquals(
            "무슨 일: 자기주식 소각 결정\n무슨 뜻: 주식 수가 줄어 주당 가치가 오를 수 있음\n확인할 점: 소각 규모(공시 원문 확인 필요)",
            explain.clean(raw)
        )
    }

    @Test
    fun `형식을 못 맞추면 보여 주지 않는다`() {
        assertNull(explain.clean("무슨 일: 소각\n무슨 뜻: 좋음"))
        assertNull(explain.clean(""))
    }

    /** 🔴 프롬프트가 막아도 모델이 어길 수 있다 — 어긴 해설은 버린다. */
    @Test
    fun `권유와 예측이 섞이면 버린다`() {
        val bad = listOf(
            "지금 매수하세요", "보유를 권합니다", "목표가 9만 원", "주가가 오를 것으로 보입니다",
            "급등 예상", "명백한 호재입니다"
        )
        bad.forEach { w ->
            assertNull(w, explain.clean("무슨 일: 소각\n무슨 뜻: $w\n확인할 점: 원문"))
        }
    }

    /** 사실로서의 «매수»(자기주식 매수 등)까지 막으면 해설을 못 쓴다. */
    @Test
    fun `사실을 말하는 매수는 막지 않는다`() {
        assertFalse(explain.violates("회사가 자기주식을 장내 매수해 소각한다"))
        assertFalse(explain.violates("공개매수를 통해 지분을 늘린다"))
    }

    /** 🔴 §8 — 알림 문구에 투자 판단 표현이 끼어들면 안 된다. */
    @Test
    fun `알림 문구에 투자 판단 표현이 없다`() {
        val banned = listOf("매수", "매도", "신호", "호재", "악재", "추천", "급등", "바닥")
        val notices = DisclosureAlertPolicy.buildNotices(
            DisclosureCategory.selectable.mapIndexed { i, c ->
                AlertEntry(item("$i", "제목", "C$i", "회사$i"), c)
            }
        ) + DisclosureAlertPolicy.buildNotices(listOf(entry("1", "A", "가나")))
        notices.forEach { n ->
            banned.forEach { w -> assertFalse("«$w»: ${n.title} / ${n.text}", (n.title + n.text).contains(w)) }
        }
        DisclosureCategory.entries.forEach { c -> banned.forEach { w -> assertFalse(c.label.contains(w)) } }
    }
}

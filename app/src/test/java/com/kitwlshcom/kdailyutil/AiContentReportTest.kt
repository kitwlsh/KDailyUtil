package com.kitwlshcom.kdailyutil

import com.kitwlshcom.kdailyutil.data.AiContentReport
import com.kitwlshcom.kdailyutil.data.PassageSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 「🚩 지문 신고」 (2026-09-23 · §할 일 0-J).
 *
 * 🔴 **무엇을 지키는 테스트인가** — 신고가 **쓸모 있게 도착하는지**다.
 * 「이상해요」 한 줄만 오고 **어느 글인지 모르면** 우리는 아무것도 고칠 수 없다.
 * 그래서 식별 정보(제목·ID·본문 일부)가 **자동으로 채워지는 것**이 이 기능의 전부다.
 */
class AiContentReportTest {

    private val title = "빗속의 정거장"
    private val text = "비가 내리는 정거장에서 그는 오래 기다렸다. ".repeat(20)

    @Test
    fun `제목에 지문 ID가 들어간다`() {
        // 🔴 받은 편지함에서 **열어 보지 않고도** 어느 글인지 알아야 한다(퀴즈 신고와 같은 문법).
        assertTrue(AiContentReport.subject(20260919001L, title).contains("20260919001"))
    }

    @Test
    fun `ID가 없는 글은 제목으로 대신한다`() {
        val s = AiContentReport.subject(null, title)
        assertTrue(s.contains(title))
        assertFalse("null이 그대로 새어 나가면 안 된다", s.contains("null"))
    }

    @Test
    fun `제목도 ID도 없으면 빈 제목이 되지 않는다`() {
        val s = AiContentReport.subject(null, "   ")
        assertTrue(s.contains("제목 없음"))
    }

    @Test
    fun `본문에 ID와 제목과 본문 일부가 들어간다`() {
        val body = AiContentReport.body(20260919001L, title, text, PassageSource.ROBOT)
        assertTrue(body.contains("20260919001"))
        assertTrue(body.contains(title))
        assertTrue(body.contains(text.take(30)))
        assertTrue("출처도 같이 온다", body.contains(PassageSource.ROBOT.badge))
        assertTrue("글자 수도 단서다", body.contains("${text.length}자"))
    }

    @Test
    fun `장문이어도 메일이 본문으로 뒤덮이지 않는다`() {
        // 토요일 장문은 1,200자다. 그대로 넣으면 메일 앱이 잘라 버리거나
        // 사용자가 «내가 뭘 보내는지» 모르는 채로 보내게 된다.
        val long = "가".repeat(1200)
        val body = AiContentReport.body(1L, title, long, PassageSource.ROBOT)
        assertTrue(body.contains("…"))
        assertFalse(body.contains("가".repeat(AiContentReport.EXCERPT_MAX + 5)))
    }

    @Test
    fun `사용자가 쓸 자리가 맨 위에 있다`() {
        // 🔴 자동 정보가 먼저 오면 사용자는 스크롤을 내려야 «쓸 곳»을 찾는다 → 대부분 그냥 닫는다.
        val body = AiContentReport.body(1L, title, text, PassageSource.ROBOT)
        assertTrue(body.trimStart().startsWith("[어떤 점이 이상한지"))
    }

    @Test
    fun `내 지문은 신고 대상이 아니다`() {
        // 사용자가 찍거나 붙여 넣은 글은 우리가 고칠 수 없다.
        // 퀴즈도 «개인 제작 문제는 신고 제외»로 같은 판단을 이미 했다.
        assertFalse(AiContentReport.canReport(PassageSource.MINE))
        assertTrue(AiContentReport.canReport(PassageSource.ROBOT))
        assertTrue(AiContentReport.canReport(PassageSource.BUILT_IN))
    }

    @Test
    fun `퀴즈 신고에는 어느 지문으로 만든 문제인지가 들어간다`() {
        val body = AiContentReport.quizBody(text, listOf("글쓴이의 심정은?", "정거장에서 한 일은?"))
        assertTrue(body.contains(text.take(30)))
        assertTrue(body.contains("글쓴이의 심정은?"))
        assertTrue(body.contains("문제 2:"))
    }

    @Test
    fun `신고 주소는 퀴즈 뉴스와 같은 곳이다`() {
        // 🔴 주소가 갈리면 신고가 두 군데로 흩어져 한 곳에서 볼 수 없다.
        assertEquals("kitwlsh@gmail.com", AiContentReport.EMAIL)
    }

    @Test
    fun `증시 AI 분석 신고에는 어느 분석인지와 내용 일부가 들어간다`() {
        // 🔴 이쪽은 «사실을 말하는 글»이다 — 틀린 숫자가 나가면 지문보다 해가 크다.
        val subject = AiContentReport.aiSubject("삼성전자 AI 실적 요약")
        assertTrue(subject.contains("삼성전자"))
        val body = AiContentReport.aiBody("삼성전자 AI 실적 요약", "영업이익이 전년 대비 12% 늘었다. ".repeat(30))
        assertTrue(body.contains("삼성전자"))
        assertTrue(body.contains("영업이익"))
        assertTrue("장문은 잘라서 보낸다", body.contains("…"))
        assertTrue(body.trimStart().startsWith("[어떤 점이 이상한지"))
    }

    @Test
    fun `분석 제목이 비어 있어도 빈 제목이 되지 않는다`() {
        assertTrue(AiContentReport.aiSubject("  ").contains("제목 없음"))
    }
}

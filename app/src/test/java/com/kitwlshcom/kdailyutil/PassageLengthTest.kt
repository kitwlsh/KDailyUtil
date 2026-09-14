package com.kitwlshcom.kdailyutil

import com.kitwlshcom.kdailyutil.data.PassageLength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 지문 길이 표시 (2026-09-14).
 *
 * 🔴 **왜 필요한가** — 실측해 보니 시스템 지문이 평균 **224자 = 300 WPM에서 약 11초**였다.
 * 목록이 길이를 한마디도 하지 않아서 «어느 걸 고를까»에 답할 수 없었고, 앞으로 장문 지문이
 * 들어오면 모르고 눌렀다가 당황하게 된다.
 *
 * 🔴 **이 테스트의 핵심은 «표시가 실제와 맞는가»다.** 화면이 「약 12초」라고 했는데 훈련이
 * 20초 걸리면 그 표시는 없느니만 못하다. 그래서 어절을 세는 규칙이 RSVP와 같은지 고정한다.
 */
class PassageLengthTest {

    /** 실제 시스템 지문 하나(2026-09-13 `passages_2026.json`에서 가져온 길이대로 재현). */
    private val realPassage = List(57) { "단어$it" }.joinToString(" ")

    @Test
    fun `어절 세는 규칙이 RSVP 화면과 같다`() {
        // RsvpModule이 쓰는 식 그대로: trim().split(Regex("\s+")).filter { isNotBlank() }
        val rsvpWay = realPassage.trim().split(Regex("""\s+""")).filter { it.isNotBlank() }.size
        assertEquals(
            "표시와 실제가 어긋나면 「약 12초」가 거짓말이 된다",
            rsvpWay,
            PassageLength.wordCount(realPassage)
        )
    }

    @Test
    fun `줄바꿈과 연속 공백을 한 칸으로 센다`() {
        assertEquals(3, PassageLength.wordCount("  가  나\n\n다  "))
        assertEquals(0, PassageLength.wordCount("   "))
    }

    /** 🔴 실측값 고정 — 57어절을 300 WPM으로 읽으면 11.4초다. */
    @Test
    fun `시스템 지문 한 편은 300WPM에서 12초 안쪽이다`() {
        val sec = PassageLength.seconds(realPassage, 300)
        assertEquals(12, sec) // ceil(57/300*60) = ceil(11.4)
        assertTrue("훈련 한 판이 15초를 넘지 않는다 = 지금 지문이 짧다는 증거다", sec < 15)
    }

    /** 빠르게 읽을수록 짧아진다 — 당연해 보이지만 반대로 짜면 아무도 못 알아챈다. */
    @Test
    fun `속도가 빠르면 예상 시간이 줄어든다`() {
        assertTrue(PassageLength.seconds(realPassage, 500) < PassageLength.seconds(realPassage, 300))
    }

    /** 🔴 «0초»는 정보가 아니다. 아주 짧은 글도 최소 1초로 말한다. */
    @Test
    fun `아주 짧은 글도 0초라고 말하지 않는다`() {
        assertEquals(1, PassageLength.seconds("한마디", 600))
        assertEquals(0, PassageLength.seconds("   ", 300))
    }

    /** 말도 안 되는 속도가 들어와도 나눗셈이 터지지 않는다. */
    @Test
    fun `속도가 0이나 음수여도 계산이 무너지지 않는다`() {
        assertTrue(PassageLength.seconds(realPassage, 0) > 0)
        assertTrue(PassageLength.seconds(realPassage, -100) > 0)
    }

    /** 1분을 넘으면 초 단위는 뭉갠다 — 고르는 데 「1분 37초」의 7초는 필요 없다. */
    @Test
    fun `1분이 넘으면 분으로 말한다`() {
        val long = List(300) { "단어$it" }.joinToString(" ") // 300어절 = 300WPM에서 정확히 60초
        assertEquals("약 1분", PassageLength.durationLabel(long, 300))
        assertTrue(PassageLength.durationLabel(realPassage, 300).endsWith("초"))
    }

    /** 목록 한 줄 문구 — 글자 수와 시간이 함께 나온다. */
    @Test
    fun `목록 문구에 글자 수와 시간이 함께 나온다`() {
        val label = PassageLength.label("가나다 라마바", 300)
        assertTrue("글자 수를 말해야 한다: $label", label.contains("자"))
        assertTrue("시간을 말해야 한다: $label", label.contains("약"))
        assertEquals("빈 글에는 아무것도 붙이지 않는다", "", PassageLength.label("   ", 300))
    }
}

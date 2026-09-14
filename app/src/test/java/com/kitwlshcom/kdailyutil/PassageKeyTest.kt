package com.kitwlshcom.kdailyutil

import com.kitwlshcom.kdailyutil.data.PassageKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 지문 열쇠 — 「같은 지문을 다시 읽으면 속도가 올라간다」를 막는 장치 (2026-09-14).
 *
 * 🔴 **왜 이 테스트가 중요한가** — 열쇠가 너무 깐깐하면(공백 하나에 달라지면)
 * 재독이 매번 «초독»으로 잡혀 장치가 **있으나 마나**가 된다.
 * 반대로 너무 헐거우면 다른 글이 같은 글로 묶여 **초독인데 기록이 안 올라간다.**
 * 둘 다 조용히 나빠지는 고장이라 여기서 고정한다.
 */
class PassageKeyTest {

    private val passage = "밤하늘을 올려다보며 우리가 마주하는 별빛은 아주 먼 과거에서 출발한 빛이다."

    @Test
    fun `같은 글은 같은 열쇠다`() {
        assertEquals(PassageKey.of(passage), PassageKey.of(passage))
    }

    /**
     * 🔴 **공백 차이는 같은 글로 본다.** OCR과 붙여넣기는 줄바꿈·공백이 매번 달라서,
     * 그대로 비교하면 **같은 글을 읽을 때마다 «새 글»**이 되어 재독 판정이 무력화된다.
     */
    @Test
    fun `줄바꿈과 공백만 다른 글은 같은 열쇠다`() {
        val messy = "  밤하늘을   올려다보며 우리가\n마주하는 별빛은 아주 먼 과거에서\n\n출발한 빛이다.  "
        assertEquals(PassageKey.of(passage), PassageKey.of(messy))
    }

    /** 내용이 다르면 달라야 한다 — 아니면 초독인데 기록이 안 올라간다. */
    @Test
    fun `다른 글은 다른 열쇠다`() {
        assertNotEquals(PassageKey.of(passage), PassageKey.of(passage + " 그리고 한 문장 더."))
        assertNotEquals(PassageKey.of("가나다"), PassageKey.of("가나라"))
    }

    /** 빈 글은 기록 대상이 아니다(워밍업·안구 추적은 지문을 쓰지 않는다). */
    @Test
    fun `빈 글은 열쇠가 없다`() {
        assertEquals("", PassageKey.of(""))
        assertEquals("", PassageKey.of("   \n  "))
    }

    /** 🔴 본문을 통째로 저장하지 않는다 — 읽은 지문이 쌓여도 길이가 고정이어야 한다. */
    @Test
    fun `열쇠는 짧고 길이가 고정이다`() {
        val long = "가".repeat(5000)
        assertEquals(16, PassageKey.of(long).length)
        assertEquals(16, PassageKey.of(passage).length)
        assertTrue("본문이 그대로 들어가면 안 된다", !PassageKey.of(passage).contains("밤하늘"))
    }
}

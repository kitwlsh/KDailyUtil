package com.kitwlshcom.kdailyutil

import com.kitwlshcom.kdailyutil.data.PassageReadLog
import com.kitwlshcom.kdailyutil.data.PassageReadLog.FRESH_AFTER_DAYS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 읽은 지문 기록 — «한 번 읽으면 영영 재독»을 «90일이 지나면 다시 처음»으로 (2026-10-28).
 *
 * 🔴 조용히 나빠지는 쪽이 둘이다: 재독이 «처음»으로 새면 속도 추이가 부풀고,
 * 옛 글이 영영 재독이면 기록이 안 쌓인다. 경계(89일/90일)와 옛 기록 이전을 여기서 고정한다.
 */
class PassageReadLogTest {

    private val today = 20_000L

    @Test
    fun `처음 보는 지문은 처음이다`() {
        val (first, log) = PassageReadLog.record(emptyMap(), "a", today)
        assertTrue(first)
        assertEquals(mapOf("a" to today), log)
    }

    @Test
    fun `89일 전에 읽은 지문은 재독이고 90일 전이면 다시 처음이다`() {
        assertFalse(PassageReadLog.record(mapOf("a" to today - (FRESH_AFTER_DAYS - 1)), "a", today).first)
        assertTrue(PassageReadLog.record(mapOf("a" to today - FRESH_AFTER_DAYS), "a", today).first)
    }

    /** 기준은 «마지막으로 읽은 날» — 재독해도 날짜가 새로 찍힌다. */
    @Test
    fun `재독하면 마지막으로 읽은 날이 오늘로 바뀐다`() {
        val (first, log) = PassageReadLog.record(mapOf("a" to today - 30), "a", today)
        assertFalse(first)
        assertEquals(today, log["a"])
    }

    /** 오래된 항목은 «없음»과 같으니 버린다 — 집합이 끝없이 자라지 않게. */
    @Test
    fun `기록할 때 90일 지난 다른 항목은 버리고 최근 항목은 남긴다`() {
        val old = mapOf("old" to today - FRESH_AFTER_DAYS, "recent" to today - 10)
        val (_, log) = PassageReadLog.record(old, "new", today)
        assertEquals(setOf("recent", "new"), log.keys)
    }

    /** 🔴 옛 기록은 «오늘 읽은 것»으로 — 업데이트 순간 옛 글이 한꺼번에 «처음»이 되지 않게. */
    @Test
    fun `옛 형식 기록은 이전한 날 읽은 것으로 옮긴다`() {
        val log = PassageReadLog.migrate(setOf("a", "b", ""), today)
        assertEquals(mapOf("a" to today, "b" to today), log)
        assertFalse(PassageReadLog.record(log, "a", today + 1).first)
        assertTrue(PassageReadLog.record(log, "a", today + FRESH_AFTER_DAYS).first)
    }

    @Test
    fun `저장 형식을 오가도 같고 깨진 항목은 버린다`() {
        val log = mapOf("a" to today, "b" to today - 5)
        assertEquals(log, PassageReadLog.decode(PassageReadLog.encode(log)))
        assertEquals(mapOf("ok" to 7L), PassageReadLog.decode(setOf("ok|7", "noday", "|5", "bad|x")))
    }
}

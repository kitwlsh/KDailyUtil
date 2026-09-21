package com.kitwlshcom.kdailyutil

import com.kitwlshcom.kdailyutil.data.PassageLength
import com.kitwlshcom.kdailyutil.data.PassageLength.LengthFilter
import com.kitwlshcom.kdailyutil.data.PassageSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 「📜 긴 지문」 갈래 나누기 + 출처 표기 (2026-09-21 · 사용자 요청).
 *
 * 🔴 **무엇을 지키는 테스트인가** — 화면의 칩이 말하는 편수와 목록에 실제로 남는 편수가
 * **어긋나면 안 된다.** 「긴 지문 2」를 눌렀는데 3편이 나오거나 빈 목록이 나오면
 * 그 칩은 길잡이가 아니라 함정이다.
 *
 * ⚠️ 실제 데이터로 고정한다 — 2026-09-19 첫 장문이 **1,100자**였고 평소 지문은
 * **201~235자**였다(`passages_2026.json` 실측). 이 두 덩이 사이에서 갈리는지 본다.
 */
class PassageFilterTest {

    /** 평소 지문(실측 201~235자대). */
    private val short1 = "가".repeat(201)
    private val short2 = "나".repeat(235)

    /** 2026-09-19 첫 장문(실측 1,100자). */
    private val long1 = "다".repeat(1100)

    /** 로봇 규격의 아래끝(800자). 규격이 흔들려도 장문으로 잡혀야 한다. */
    private val longMin = "라".repeat(800)

    private val all = listOf(short1, long1, short2, longMin)

    @Test
    fun `전체는 하나도 빼지 않는다`() {
        assertEquals(all.size, all.count { LengthFilter.ALL.matches(it) })
    }

    @Test
    fun `긴 지문과 짧은 지문이 서로를 남기지 않는다`() {
        val longs = all.filter { LengthFilter.LONG.matches(it) }
        val shorts = all.filter { LengthFilter.SHORT.matches(it) }
        assertEquals(listOf(long1, longMin), longs)
        assertEquals(listOf(short1, short2), shorts)
        // 🔴 둘을 합치면 정확히 전체여야 한다 — 어느 갈래에도 없는 지문이 생기면
        //    사용자는 «분명히 있었는데 사라진 지문»을 만나게 된다.
        assertEquals(all.size, longs.size + shorts.size)
        assertTrue(longs.intersect(shorts.toSet()).isEmpty())
    }

    @Test
    fun `칩에 적는 편수가 목록과 같다`() {
        LengthFilter.entries.forEach { f ->
            assertEquals(
                "칩이 말하는 편수와 실제로 남는 편수가 다르면 그 칩은 함정이다",
                all.count { f.matches(it) },
                PassageLength.countIn(all, f)
            )
        }
    }

    @Test
    fun `경계값 500자에서 갈린다`() {
        val justUnder = "마".repeat(PassageLength.LONG_CHARS - 1)
        val exactly = "바".repeat(PassageLength.LONG_CHARS)
        assertTrue(LengthFilter.SHORT.matches(justUnder))
        assertFalse(LengthFilter.LONG.matches(justUnder))
        assertTrue(LengthFilter.LONG.matches(exactly))
        assertFalse(LengthFilter.SHORT.matches(exactly))
    }

    @Test
    fun `빈 목록에서도 편수는 0이고 터지지 않는다`() {
        LengthFilter.entries.forEach { f ->
            assertEquals(0, PassageLength.countIn(emptyList(), f))
        }
    }

    @Test
    fun `길이가 출처를 바꾸지 않는다`() {
        // 🔴 토요일 장문도 같은 로봇·같은 프롬프트가 만든다(`is_long_day`가 길이만 바꾼다).
        //    «긴 글은 어디서 퍼 온 것» 같은 오해가 코드에 스미지 않게 못 박아 둔다.
        assertEquals(PassageSource.ROBOT, PassageSource.ofRemote())
        assertTrue(LengthFilter.LONG.matches(long1))
        assertEquals(PassageSource.ROBOT, PassageSource.ofRemote())
    }

    @Test
    fun `출처 문구는 갈래마다 다르고 비어 있지 않다`() {
        PassageSource.entries.forEach {
            assertTrue("꼬리표가 비면 화면에 빈 줄만 생긴다", it.badge.isNotBlank())
            assertTrue("설명이 없으면 꼬리표만 남아 «그래서 뭐?»가 된다", it.notice.isNotBlank())
        }
        // 사용자가 넣은 글은 앱이 만든 글과 **같은 말을 하면 안 된다** — 책임 주체가 다르다.
        assertNotEquals(PassageSource.ROBOT.notice, PassageSource.MINE.notice)
        assertNotEquals(PassageSource.BUILT_IN.notice, PassageSource.MINE.notice)
    }
}

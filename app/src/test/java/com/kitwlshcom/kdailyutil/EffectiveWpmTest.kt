package com.kitwlshcom.kdailyutil

import com.kitwlshcom.kdailyutil.data.EffectiveWpm
import com.kitwlshcom.kdailyutil.data.EffectiveWpm.Pace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 유효 속도(EWPM) — **«빠르게만»이 이득이 아니게** (2026-09-23).
 *
 * 🔴 **무엇을 지키는 테스트인가** — 세 가지다.
 * 1. **키 없는 사용자가 다쳐서는 안 된다.** 검증된 기록이 0건이면 목표 계산이
 *    2026-09-14까지와 **한 값도 달라지면 안 된다**(최근 5회 평균 × 1.08 · 10단위 · 150~700).
 * 2. **이해도가 낮은데 목표가 올라가면 안 된다.** 그 순간 앱이 «읽지 말고 넘겨라»라고 시키는 셈이다.
 * 3. **한 판으로 목표가 움직이면 안 된다.** 문항 몇 개짜리 점수는 출렁인다 — 평균으로만 본다.
 */
class EffectiveWpmTest {

    private fun rec(wpm: Int, comp: Int, q: Int = 4) =
        EffectiveWpm.Record(wpm = wpm, comprehension = comp, questions = q, date = "20260923")

    // ── 곱셈 자체 ──────────────────────────────────────────

    @Test
    fun `유효 속도는 속도에 이해도를 곱한 값이다`() {
        assertEquals(300, EffectiveWpm.of(500, 60))
        assertEquals(500, EffectiveWpm.of(500, 100))
        assertEquals(0, EffectiveWpm.of(500, 0))
    }

    @Test
    fun `깨진 값이 들어와도 음수나 과장된 속도가 나오지 않는다`() {
        assertEquals(0, EffectiveWpm.of(-100, 80))
        assertEquals(500, EffectiveWpm.of(500, 150)) // 100%로 잘린다
        assertEquals(0, EffectiveWpm.of(500, -20))
    }

    // ── 표본이 작은 문제 ────────────────────────────────────

    @Test
    fun `문항이 너무 적은 판은 기록하지 않는다`() {
        assertFalse(EffectiveWpm.isRecordable(wpm = 400, questions = 2))
        assertTrue(EffectiveWpm.isRecordable(wpm = 400, questions = EffectiveWpm.MIN_QUESTIONS))
    }

    @Test
    fun `속도가 없는 판은 기록하지 않는다`() {
        // 워밍업·안구 추적은 wpm=0으로 들어온다 — 곱할 속도가 애초에 없다.
        assertFalse(EffectiveWpm.isRecordable(wpm = 0, questions = 5))
    }

    // ── 🔴 키 없는 사용자: 예전과 한 값도 달라지지 않는다 ────────

    @Test
    fun `검증된 기록이 없으면 목표는 2026-09-14까지의 계산 그대로다`() {
        // 예전 식: 최근 5회 평균 × 1.08 → 10단위 내림 → 150~700
        val history = listOf(300, 320, 340, 360, 380) // 평균 340 × 1.08 = 367.2 → 360
        assertEquals(360, EffectiveWpm.nextTarget(history, emptyList()))
        assertEquals(Pace.UNVERIFIED, EffectiveWpm.paceOf(emptyList()))
    }

    @Test
    fun `기록이 없으면 기본값으로 시작한다`() {
        assertEquals(EffectiveWpm.DEFAULT_WPM, EffectiveWpm.nextTarget(emptyList(), emptyList()))
        assertEquals(EffectiveWpm.DEFAULT_WPM, EffectiveWpm.nextTarget(emptyList(), listOf(rec(400, 90))))
    }

    @Test
    fun `목표는 슬라이더 범위를 벗어나지 않는다`() {
        assertEquals(700, EffectiveWpm.nextTarget(listOf(700, 700, 700), emptyList()))
        assertEquals(150, EffectiveWpm.nextTarget(listOf(100, 100, 100), emptyList()))
    }

    // ── 🔴 이해도가 방향을 정한다 ──────────────────────────

    @Test
    fun `이해도가 높으면 목표를 올린다`() {
        val history = listOf(300, 320, 340, 360, 380) // 평균 340
        val verified = listOf(rec(340, 85), rec(360, 90))
        assertEquals(Pace.RAISE, EffectiveWpm.paceOf(verified))
        assertEquals(360, EffectiveWpm.nextTarget(history, verified)) // 340 × 1.08
    }

    @Test
    fun `이해도가 어중간하면 목표를 그대로 둔다`() {
        val history = listOf(300, 320, 340, 360, 380) // 평균 340
        val verified = listOf(rec(340, 60), rec(360, 55))
        assertEquals(Pace.HOLD, EffectiveWpm.paceOf(verified))
        assertEquals(340, EffectiveWpm.nextTarget(history, verified)) // 올리지 않는다
    }

    @Test
    fun `이해도가 낮으면 목표를 낮춘다`() {
        val history = listOf(300, 320, 340, 360, 380) // 평균 340
        val verified = listOf(rec(340, 40), rec(360, 30))
        assertEquals(Pace.SLOW, EffectiveWpm.paceOf(verified))
        val target = EffectiveWpm.nextTarget(history, verified)
        assertTrue("낮아져야 한다: $target", target < 340)
        assertEquals(310, target) // 340 × 0.92 = 312.8 → 310
    }

    @Test
    fun `빠르게 넘기기만 하면 목표가 따라 오르지 않는다`() {
        // 🔴 이 테스트가 이 기능의 존재 이유다.
        // 같은 사람이 속도만 700으로 밀었을 때, 이해도가 따라오면 목표가 오르고
        // 따라오지 않으면 오르지 않아야 한다 — 기준선(history)은 동일하다.
        val history = listOf(600, 650, 700, 700, 700) // 평균 670
        val understood = EffectiveWpm.nextTarget(history, listOf(rec(700, 90)))
        val skimmed = EffectiveWpm.nextTarget(history, listOf(rec(700, 20)))
        assertTrue("이해하고 읽으면 목표가 오른다", understood > 670)
        assertTrue("넘기기만 하면 목표가 오르지 않는다", skimmed < 670)
    }

    // ── 평균으로만 본다(한 판에 끌려가지 않는다) ──────────────

    @Test
    fun `직전 한 판이 나빠도 평균이 좋으면 방향이 바뀌지 않는다`() {
        val good = List(4) { rec(400, 90) }
        assertEquals(Pace.RAISE, EffectiveWpm.paceOf(good + rec(400, 50)))
    }

    @Test
    fun `평균은 최근 다섯 판만 본다`() {
        // 옛날에 100%를 몇 번 맞았다고 지금의 낮은 이해도가 가려지면 안 된다.
        val old = List(10) { rec(400, 100) }
        val recent = List(EffectiveWpm.RECENT) { rec(400, 30) }
        assertEquals(30, EffectiveWpm.averageComprehension(old + recent))
        assertEquals(Pace.SLOW, EffectiveWpm.paceOf(old + recent))
    }

    @Test
    fun `평균 유효 속도도 최근 다섯 판만 본다`() {
        val records = List(10) { rec(100, 100) } + List(EffectiveWpm.RECENT) { rec(400, 50) }
        assertEquals(200, EffectiveWpm.averageEwpm(records)) // 400 × 50%
    }

    @Test
    fun `기록이 없으면 평균은 0이고 터지지 않는다`() {
        assertEquals(0, EffectiveWpm.averageEwpm(emptyList()))
        assertEquals(0, EffectiveWpm.averageComprehension(emptyList()))
    }

    @Test
    fun `경계값 — 올림과 유지가 갈리는 지점`() {
        assertEquals(Pace.RAISE, EffectiveWpm.paceOf(listOf(rec(400, EffectiveWpm.RAISE_ABOVE))))
        assertEquals(Pace.HOLD, EffectiveWpm.paceOf(listOf(rec(400, EffectiveWpm.RAISE_ABOVE - 1))))
        assertEquals(Pace.HOLD, EffectiveWpm.paceOf(listOf(rec(400, EffectiveWpm.SLOW_BELOW))))
        assertEquals(Pace.SLOW, EffectiveWpm.paceOf(listOf(rec(400, EffectiveWpm.SLOW_BELOW - 1))))
    }
}

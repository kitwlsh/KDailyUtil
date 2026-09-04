package com.kitwlshcom.kdailyutil

import com.kitwlshcom.kdailyutil.data.DailyRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 출석·연속(스트릭)·오늘의 퀴즈 **규칙**을 고정하는 테스트.
 *
 * 문구가 아니라 «규칙»을 지킨다. 이 규칙들은 한 번 출시하면 사용자 기기에 그대로 기록이 쌓여
 * 되돌리기 어렵기 때문에, 나중에 누가 무심코 바꾸면 여기서 걸려야 한다.
 */
class DailyRecordTest {

    private val today = LocalDate.of(2026, 9, 4)

    // ── 연속(스트릭) ──────────────────────────────────────────

    @Test
    fun `처음 완료하면 1일차다`() {
        val r = DailyRecord.advance(today, lastDone = null, streak = 0)
        assertEquals(1, r.streak)
        assertTrue(r.changed)
    }

    @Test
    fun `어제 했으면 이어진다`() {
        val r = DailyRecord.advance(today, lastDone = today.minusDays(1), streak = 4)
        assertEquals(5, r.streak)
    }

    @Test
    fun `같은 날 두 번 해도 두 번 세지 않는다`() {
        val r = DailyRecord.advance(today, lastDone = today, streak = 5)
        assertEquals(5, r.streak)
        assertFalse(r.changed)
    }

    @Test
    fun `하루 빠져도 유예로 살아난다`() {
        // 관대 규칙: 스트릭이 한 번 끊기면 「어차피 망했다」며 이탈한다 → 살려 준다
        val r = DailyRecord.advance(today, lastDone = today.minusDays(2), streak = 9, lastFreeze = null)
        assertEquals(10, r.streak)
        assertTrue(r.freezeUsed)
        assertEquals(today, r.lastFreeze)
    }

    @Test
    fun `유예는 7일에 한 번만 쓸 수 있다`() {
        // 어제 유예를 썼다면 오늘 또 쓸 수는 없다 → 1일차로 초기화
        val r = DailyRecord.advance(today, lastDone = today.minusDays(2), streak = 9, lastFreeze = today.minusDays(1))
        assertEquals(1, r.streak)
        assertFalse(r.freezeUsed)
    }

    @Test
    fun `유예를 쓴 지 7일이 지나면 다시 쓸 수 있다`() {
        val r = DailyRecord.advance(today, lastDone = today.minusDays(2), streak = 9, lastFreeze = today.minusDays(7))
        assertEquals(10, r.streak)
        assertTrue(r.freezeUsed)
    }

    @Test
    fun `이틀 넘게 빠지면 유예가 있어도 처음부터다`() {
        val r = DailyRecord.advance(today, lastDone = today.minusDays(3), streak = 20, lastFreeze = null)
        assertEquals(1, r.streak)
        assertFalse(r.freezeUsed)
    }

    @Test
    fun `기기 시간이 과거로 돌아가도 죽지 않는다`() {
        // 미래 날짜가 저장돼 있는 이상 상태 — 예외 없이 1일차로 처리한다
        val r = DailyRecord.advance(today, lastDone = today.plusDays(3), streak = 7)
        assertEquals(1, r.streak)
    }

    // ── 화면에 보여 줄 연속일수 ────────────────────────────────

    @Test
    fun `사흘 쉰 사람에게 연속일수를 자랑하지 않는다`() {
        // 저장값은 마지막으로 푼 날 기준이라 그대로 쓰면 거짓말이 된다
        assertEquals(0, DailyRecord.displayStreak(today, today.minusDays(3), streak = 7))
    }

    @Test
    fun `오늘 했거나 어제 했으면 연속은 살아 있다`() {
        assertEquals(7, DailyRecord.displayStreak(today, today, streak = 7))
        assertEquals(7, DailyRecord.displayStreak(today, today.minusDays(1), streak = 7))
    }

    @Test
    fun `하루 빠진 사람에게는 아직 살릴 수 있다고 보여 준다`() {
        // 오늘 풀면 유예로 이어지므로, 여기서 0을 보여 주면 포기하게 만든다
        assertEquals(7, DailyRecord.displayStreak(today, today.minusDays(2), streak = 7))
    }

    @Test
    fun `한 번도 안 했으면 0이다`() {
        assertEquals(0, DailyRecord.displayStreak(today, null, streak = 5))
    }

    // ── 오늘의 퀴즈 세트 ──────────────────────────────────────

    @Test
    fun `같은 날에는 몇 번을 불러도 같은 문제가 나온다`() {
        val a = DailyRecord.pickDailyIndices(today, total = 500)
        val b = DailyRecord.pickDailyIndices(today, total = 500)
        assertEquals(a, b)
    }

    @Test
    fun `날이 바뀌면 문제도 바뀐다`() {
        val a = DailyRecord.pickDailyIndices(today, total = 500)
        val b = DailyRecord.pickDailyIndices(today.plusDays(1), total = 500)
        assertTrue("연달아 같은 세트가 나오면 오늘의 퀴즈가 아니다", a != b)
    }

    @Test
    fun `같은 문제가 두 번 들어가지 않는다`() {
        val picked = DailyRecord.pickDailyIndices(today, total = 500)
        assertEquals(DailyRecord.DAILY_QUIZ_COUNT, picked.size)
        assertEquals(picked.size, picked.toSet().size)
    }

    @Test
    fun `문항이 모자라도 빈 화면으로 나가지 않는다`() {
        assertEquals(listOf(0, 1, 2), DailyRecord.pickDailyIndices(today, total = 3))
        assertTrue(DailyRecord.pickDailyIndices(today, total = 0).isEmpty())
    }

    @Test
    fun `새로 들어온 문항이 최소 한 칸은 배정된다`() {
        // 하루 5문제씩 새로 들어오는데 사용자가 그걸 영영 못 보는 일이 없어야 한다
        val total = 500
        val freshFrom = 495 // 뒤쪽 5개가 새 문항이라고 치자
        val picked = DailyRecord.pickDailyIndices(today, total = total, freshFrom = freshFrom)
        assertTrue(
            "새 문항 구간(>= $freshFrom)에서 최소 1개는 뽑혀야 한다: $picked",
            picked.any { it >= freshFrom }
        )
        assertEquals(DailyRecord.DAILY_QUIZ_COUNT, picked.size)
    }

    @Test
    fun `새 문항 배정은 여러 날에 걸쳐도 유지된다`() {
        val total = 500
        val freshFrom = 495
        for (d in 0L until 30L) {
            val picked = DailyRecord.pickDailyIndices(today.plusDays(d), total = total, freshFrom = freshFrom)
            assertEquals(DailyRecord.DAILY_QUIZ_COUNT, picked.size)
            assertEquals(picked.size, picked.toSet().size)
            assertTrue("$d 일차에 새 문항이 안 들어갔다", picked.any { it >= freshFrom })
        }
    }

    // ── 기록 보관 ────────────────────────────────────────────

    @Test
    fun `기록은 저장했다가 그대로 읽힌다`() {
        val list = listOf(
            DailyRecord.DayScore(today.minusDays(1), 3, 5),
            DailyRecord.DayScore(today, 5, 5)
        )
        assertEquals(list, DailyRecord.decodeHistory(DailyRecord.encodeHistory(list)))
    }

    @Test
    fun `깨진 기록 한 줄 때문에 화면이 죽지 않는다`() {
        val decoded = DailyRecord.decodeHistory("2026-09-04:5/5\n쓰레기값\n\n2026-09-03:2/5")
        assertEquals(2, decoded.size)
    }

    @Test
    fun `기록은 30일치만 남는다`() {
        val many = (0 until 60).map { DailyRecord.DayScore(today.minusDays(it.toLong()), 1, 5) }
        val kept = DailyRecord.decodeHistory(DailyRecord.encodeHistory(many))
        assertEquals(DailyRecord.HISTORY_KEEP_DAYS, kept.size)
        assertEquals(today, kept.last().date) // 최근 것이 남아야 한다
    }

    @Test
    fun `같은 날을 다시 기록하면 더 좋은 성적이 남는다`() {
        val base = listOf(DailyRecord.DayScore(today, 2, 5))
        val better = DailyRecord.upsertHistory(base, DailyRecord.DayScore(today, 4, 5))
        assertEquals(1, better.size)
        assertEquals(4, better.first().correct)

        val worse = DailyRecord.upsertHistory(better, DailyRecord.DayScore(today, 1, 5))
        assertEquals(4, worse.first().correct)
    }

    // ── 배지 ────────────────────────────────────────────────

    @Test
    fun `배지는 최고 기록으로 판정한다`() {
        // 지금 연속이 끊겼다고 예전에 받은 배지를 빼앗으면 안 된다
        val badges = DailyRecord.badges(streak = 0, bestStreak = 7, totalSolved = 60, totalCorrect = 50)
        assertTrue(badges.first { it.id == "streak_7" }.achieved)
        assertTrue(badges.first { it.id == "solved_50" }.achieved)
        assertFalse(badges.first { it.id == "streak_30" }.achieved)
    }

    @Test
    fun `표본이 적을 때 정답률 배지를 주지 않는다`() {
        // 1문항 풀고 맞혔다고 명중 배지를 주면 배지 전체가 우스워진다
        val few = DailyRecord.badges(streak = 1, bestStreak = 1, totalSolved = 1, totalCorrect = 1)
        assertFalse(few.first { it.id == "sharp" }.achieved)

        val enough = DailyRecord.badges(streak = 1, bestStreak = 1, totalSolved = 40, totalCorrect = 36)
        assertTrue(enough.first { it.id == "sharp" }.achieved)
    }

    @Test
    fun `못 받은 배지도 얼마나 왔는지 보여 준다`() {
        val badges = DailyRecord.badges(streak = 3, bestStreak = 3, totalSolved = 25, totalCorrect = 20)
        val streak7 = badges.first { it.id == "streak_7" }
        assertFalse(streak7.achieved)
        assertEquals(3f / 7f, streak7.progress, 0.001f)
    }
}

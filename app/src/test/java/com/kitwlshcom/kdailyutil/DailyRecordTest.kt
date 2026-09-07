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


    // ── 「새 문제 N개」 상한·복귀 사면 ───────────────────────────
    //
    // 여기서 지키는 것은 문구가 아니라 **«오래 비운 사람에게 큰 숫자를 보내지 않는다»** 는 규칙이다.
    // 상한이 없던 시절에는 두 달 비운 사용자에게 「새 문제 300개」가 나갔다(doc/FEATURE_DAILY_PASSAGES.md §6).

    @Test
    fun `새 문제 수는 마지막으로 본 뒤 늘어난 만큼이다`() {
        val n = DailyRecord.newQuizNotice(today, lastDone = today.minusDays(1), total = 515, seenCount = 508)
        assertEquals(7, n.count)
        assertFalse(n.capped)
        assertFalse(n.amnesty)
        assertTrue(n.hasNumber)
        assertEquals("7개", n.text)
    }

    @Test
    fun `상한을 넘으면 잘라서 20개+로 말한다`() {
        // 하루 5문항이 들어오므로 상한이 없으면 세 자리 숫자가 알림으로 나간다
        val n = DailyRecord.newQuizNotice(today, lastDone = today.minusDays(1), total = 815, seenCount = 515)
        assertEquals(DailyRecord.QUIZ_NEW_CAP, n.count)
        assertTrue(n.capped)
        assertEquals("20개+", n.text)
    }

    @Test
    fun `상한과 같은 값은 자르지 않는다`() {
        val n = DailyRecord.newQuizNotice(
            today, lastDone = today.minusDays(1),
            total = 500 + DailyRecord.QUIZ_NEW_CAP, seenCount = 500
        )
        assertEquals(DailyRecord.QUIZ_NEW_CAP, n.count)
        assertFalse(n.capped)
        assertEquals("20개", n.text)
    }

    @Test
    fun `7일 이상 비웠다 돌아오면 숫자를 아예 말하지 않는다`() {
        val n = DailyRecord.newQuizNotice(today, lastDone = today.minusDays(60), total = 815, seenCount = 515)
        assertTrue(n.amnesty)
        assertEquals(0, n.count)
        assertFalse(n.hasNumber)   // 「놓친 300개」는 초대가 아니라 청구서다
    }

    @Test
    fun `사면 경계는 7일이다`() {
        // 6일 = 아직 사면이 아니다(숫자를 보여 준다) / 7일 = 사면
        val six = DailyRecord.newQuizNotice(today, lastDone = today.minusDays(6), total = 520, seenCount = 515)
        assertFalse(six.amnesty)
        assertEquals(5, six.count)

        val seven = DailyRecord.newQuizNotice(today, lastDone = today.minusDays(7), total = 520, seenCount = 515)
        assertTrue(seven.amnesty)
    }

    @Test
    fun `한 번도 푼 적 없는 사람은 사면 대상이 아니다`() {
        // 첫 방문은 «복귀»가 아니다. 사면을 걸면 첫 사용자에게 「다시 시작해요」가 나간다
        assertFalse(DailyRecord.isReturningAfterBreak(today, lastDone = null))

        val n = DailyRecord.newQuizNotice(today, lastDone = null, total = 515, seenCount = 0)
        assertFalse(n.amnesty)
        assertFalse(n.hasNumber)   // 기준점이 없으니 숫자도 말하지 않는다(전 문항이 새것이 되어 버린다)
    }

    @Test
    fun `기준점이 없으면 전 문항을 새 문제라고 하지 않는다`() {
        val n = DailyRecord.newQuizNotice(today, lastDone = today.minusDays(1), total = 515, seenCount = 0)
        assertEquals(0, n.count)
        assertFalse(n.hasNumber)
    }

    @Test
    fun `기준점이 전체보다 크면 음수가 되지 않는다`() {
        // 문항이 줄어드는 일(원격 파일 교체)이 있어도 「새 문제 -3개」가 나가면 안 된다
        val n = DailyRecord.newQuizNotice(today, lastDone = today.minusDays(1), total = 500, seenCount = 515)
        assertEquals(0, n.count)
        assertFalse(n.hasNumber)
    }

    @Test
    fun `미래 날짜가 저장돼 있으면 사면을 걸지 않는다`() {
        // 기기 시간을 앞당겼다 되돌린 경우. 이상 상태에서 사면까지 주면 이유 없이 문구가 바뀐다
        assertFalse(DailyRecord.isReturningAfterBreak(today, lastDone = today.plusDays(30)))
    }


    // ── 「새 지문 N편」 (2026-09-07) ─────────────────────────────
    //
    // 지문에는 도착일이 있어서 «신규 창»(7일)을 쓸 수 있다. 그 덕에 카운터가 구조적으로
    // 7을 넘지 못한다 — 「새 지문 60편」이 나올 길이 아예 없다.

    @Test
    fun `새 지문은 최근 7일에 도착한 것만 센다`() {
        val dates = listOf(
            today.minusDays(1), today.minusDays(2), today.minusDays(3),  // 창 안 = 3편
            today.minusDays(20), today.minusDays(40)                     // 창 밖 = 세지 않는다
        )
        val n = DailyRecord.newPassageNotice(
            today, lastTrained = today.minusDays(1),
            createdDates = dates, total = 5, seenCount = 1  // 기준점 = 1편까지 봤다
        )
        assertEquals(3, n.count)
        assertEquals("3편", n.text)
    }

    @Test
    fun `새 지문은 기준점 이후 늘어난 수를 넘지 않는다`() {
        // 최근 7일에 5편이 왔지만 사용자가 이미 3편까지 봤다면 새것은 2편이다.
        // (창만 보면 어제 다 본 지문을 오늘도 «새것»이라고 말하게 된다)
        val dates = (1..5).map { today.minusDays(it.toLong()) }
        val n = DailyRecord.newPassageNotice(
            today, lastTrained = today.minusDays(1),
            createdDates = dates, total = 5, seenCount = 3
        )
        assertEquals(2, n.count)
    }

    @Test
    fun `지문도 7일 이상 비우면 숫자를 말하지 않는다`() {
        val dates = (1..7).map { today.minusDays(it.toLong()) }
        val n = DailyRecord.newPassageNotice(
            today, lastTrained = today.minusDays(30),
            createdDates = dates, total = 40, seenCount = 5
        )
        assertTrue(n.amnesty)
        assertFalse(n.hasNumber)
    }

    @Test
    fun `지문 기준점이 없으면 숫자를 말하지 않는다`() {
        // 첫 설치 직후. 놔두면 「새 지문 40편」이 되어 버린다
        val n = DailyRecord.newPassageNotice(
            today, lastTrained = today.minusDays(1),
            createdDates = (1..7).map { today.minusDays(it.toLong()) }, total = 40, seenCount = 0
        )
        assertFalse(n.hasNumber)
        assertFalse(n.amnesty)
    }

    @Test
    fun `미래 날짜로 도착한 지문은 새것으로 세지 않는다`() {
        // 기기 시간을 앞당겼다 되돌린 경우. 「새 지문」이 유령처럼 남으면 안 된다
        val n = DailyRecord.newPassageNotice(
            today, lastTrained = today.minusDays(1),
            createdDates = listOf(today.plusDays(3)), total = 10, seenCount = 9
        )
        assertEquals(0, n.count)
    }

    // ── 알림 한 줄 조립 (2026-09-07) ────────────────────────────
    //
    // 이 문구는 사용자를 다시 데려오는 마지막 수단이다. 한 번 「알림 끄기」를 누르면
    // 그 사용자에게 닿을 길이 영구히 사라지므로, 규칙을 기기 없이 고정해 둔다.

    private fun passages(count: Int, amnesty: Boolean = false) =
        DailyRecord.NewItemNotice(count = count, amnesty = amnesty, unit = "편")

    private fun quizzes(count: Int, capped: Boolean = false, amnesty: Boolean = false) =
        DailyRecord.NewItemNotice(count = count, capped = capped, amnesty = amnesty, unit = "개")

    @Test
    fun `알림 우선순위는 새 지문 새 문제 연속일 순이다`() {
        val msg = DailyRecord.briefingMessage(passages(1), quizzes(5), streak = 12)
        assertTrue(msg.startsWith("새 지문 1편 · 새 문제 5개"))
        // 🔴 파트가 셋 다 들어가면 알림이 잘려 아무것도 전달되지 않는다 → 연속일이 밀린다
        assertFalse(msg.contains("12일 연속"))
    }

    @Test
    fun `알림 파트는 두 개를 넘지 않는다`() {
        val msg = DailyRecord.briefingMessage(passages(2), quizzes(5), streak = 3)
        assertEquals(2, msg.substringBefore(" — ").split(" · ").size)
    }

    @Test
    fun `말할 것이 적으면 연속일이 들어온다`() {
        val msg = DailyRecord.briefingMessage(passages(0), quizzes(0), streak = 4)
        assertTrue(msg.contains("4일 연속 도전 중"))
    }

    @Test
    fun `퀴즈 상한은 알림 문구에도 20개+로 나온다`() {
        val msg = DailyRecord.briefingMessage(passages(0), quizzes(20, capped = true), streak = 0)
        assertTrue(msg.contains("새 문제 20개+"))
    }

    @Test
    fun `복귀 사면 중에는 숫자도 연속일도 말하지 않는다`() {
        val both = DailyRecord.briefingMessage(
            passages(0, amnesty = true), quizzes(0, amnesty = true), streak = 9
        )
        assertEquals("그동안 새 지문과 새 문제가 쌓였어요 — 오늘 한 편부터 다시 시작해요.", both)

        val onlyPassage = DailyRecord.briefingMessage(passages(0, amnesty = true), quizzes(0), streak = 0)
        assertTrue(onlyPassage.contains("새 지문이 쌓였어요"))

        val onlyQuiz = DailyRecord.briefingMessage(passages(0), quizzes(0, amnesty = true), streak = 0)
        assertTrue(onlyQuiz.contains("새 문제가 쌓였어요"))

        // 「놓쳤다」·「끊겼다」는 어떤 경우에도 쓰지 않는다 — 죄책감 알림은 알림 해제로 직행한다
        listOf(both, onlyPassage, onlyQuiz).forEach {
            assertFalse(it.contains("놓친"))
            assertFalse(it.contains("끊겼"))
            assertFalse(it.contains("밀린"))
        }
    }

    @Test
    fun `한쪽만 사면이면 다른 쪽 숫자는 그대로 말한다`() {
        // 퀴즈는 두 달 안 풀었지만 독서 훈련은 어제도 했다 → 지문 숫자는 살아 있다
        val msg = DailyRecord.briefingMessage(passages(2), quizzes(0, amnesty = true), streak = 0)
        assertTrue(msg.contains("새 지문 2편"))
        assertFalse(msg.contains("쌓였어요"))
    }

    @Test
    fun `말할 것이 하나도 없으면 기본 문구로 간다`() {
        val msg = DailyRecord.briefingMessage(passages(0), quizzes(0), streak = 0)
        assertEquals(DailyRecord.BRIEFING_FALLBACK, msg)
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

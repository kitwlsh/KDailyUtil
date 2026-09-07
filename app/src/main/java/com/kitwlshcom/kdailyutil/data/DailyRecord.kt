package com.kitwlshcom.kdailyutil.data

import java.time.LocalDate
import kotlin.random.Random

/**
 * 「매일 한 번 들어오게 하는」 장치의 **순수 로직**.
 *
 * 안드로이드 API를 일절 쓰지 않는다 — 기기·DataStore 없이 단위 테스트로 규칙을 고정하기 위해서다.
 * 저장(=DataStore)은 [com.kitwlshcom.kdailyutil.data.repository.SettingsRepository]가, 화면은 ViewModel이 맡는다.
 *
 * ⚠️ 여기 규칙 2개는 **한 번 출시하면 되돌리기 어렵다**(사용자 기기에 그 규칙대로 기록이 쌓인다).
 *   1. 출석 기준 = 「오늘의 퀴즈를 끝까지 푼 날」. 앱을 열기만 한 것은 출석이 아니다.
 *      → 출석이 너무 쉬우면 연속일수가 아무 의미가 없어지고, '오늘 몫을 다 했다'는 완결감도 안 생긴다.
 *   2. 연속(스트릭)은 **관대하게** 센다 — 하루 빠져도 유예로 살려 준다(아래 [FREEZE_INTERVAL_DAYS]).
 *      → 스트릭은 양날이다. 한 번 끊기면 「어차피 망했다」며 아예 이탈한다. 그래서 살려 주는 쪽을 택했다.
 *      → 같은 이유로 **「연속이 끊겼어요」 같은 알림은 만들지 않는다**(죄책감 알림 = 앱 삭제).
 */
object DailyRecord {

    /** 하루치 분량. 아침에 10문제는 부담이라 5문제로 잡았다. */
    const val DAILY_QUIZ_COUNT = 5

    /** 유예(하루 빠진 것을 눈감아 주는 것)를 다시 쓸 수 있게 되기까지의 간격(일). */
    const val FREEZE_INTERVAL_DAYS = 7L

    /** 기록 보관 일수. 그래프·배지 계산용이라 30일이면 충분하고, 그 이상은 저장만 커진다. */
    const val HISTORY_KEEP_DAYS = 30

    // ──────────────────────────────────────────────────────────────
    // 1. 연속(스트릭) 계산
    // ──────────────────────────────────────────────────────────────

    /**
     * 오늘 분량을 끝냈을 때의 새 연속 기록을 계산한다.
     *
     * @param today        오늘 날짜(기기 로컬)
     * @param lastDone     마지막으로 오늘의 퀴즈를 끝낸 날. 처음이면 null
     * @param streak       현재 연속일수
     * @param lastFreeze   마지막으로 유예를 쓴 날. 쓴 적 없으면 null
     */
    fun advance(
        today: LocalDate,
        lastDone: LocalDate?,
        streak: Int,
        lastFreeze: LocalDate? = null
    ): StreakResult {
        // 오늘 이미 했다면 아무것도 바뀌지 않는다(하루에 두 번 세지 않는다).
        if (lastDone == today) {
            return StreakResult(streak = streak, lastFreeze = lastFreeze, changed = false)
        }

        // 처음이거나, 미래 날짜가 저장돼 있는 이상 상태(기기 시간 변경 등)면 1일부터 다시 시작한다.
        if (lastDone == null || lastDone.isAfter(today)) {
            return StreakResult(streak = 1, lastFreeze = lastFreeze, changed = true)
        }

        val gap = today.toEpochDay() - lastDone.toEpochDay()

        return when {
            // 어제 했다 → 그냥 이어진다
            gap == 1L -> StreakResult(streak = streak + 1, lastFreeze = lastFreeze, changed = true)

            // 하루 빠졌다 → 유예를 쓸 수 있으면 살려 준다
            gap == 2L && canUseFreeze(today, lastFreeze) ->
                StreakResult(streak = streak + 1, lastFreeze = today, changed = true, freezeUsed = true)

            // 그 외(이틀 이상 빠졌거나 유예를 못 쓰는 상태) → 오늘부터 1일차
            else -> StreakResult(streak = 1, lastFreeze = lastFreeze, changed = true)
        }
    }

    /** 유예를 지금 쓸 수 있는가([FREEZE_INTERVAL_DAYS]일에 한 번). */
    fun canUseFreeze(today: LocalDate, lastFreeze: LocalDate?): Boolean {
        if (lastFreeze == null) return true
        if (lastFreeze.isAfter(today)) return true // 이상 상태면 막지 않는다
        return today.toEpochDay() - lastFreeze.toEpochDay() >= FREEZE_INTERVAL_DAYS
    }

    /**
     * 화면에 보여 줄 「현재 연속일수」.
     *
     * 저장된 값은 **마지막으로 푼 날 기준**이라 그대로 쓰면 안 된다.
     * 사흘 쉰 사람에게 「7일 연속!」이라고 말하면 그 순간 신뢰를 잃는다.
     */
    fun displayStreak(today: LocalDate, lastDone: LocalDate?, streak: Int): Int {
        if (lastDone == null) return 0
        val gap = today.toEpochDay() - lastDone.toEpochDay()
        return when {
            gap < 0L -> streak          // 미래 날짜(기기 시간 변경) — 건드리지 않는다
            gap <= 1L -> streak         // 오늘 했거나 어제 했다 = 아직 살아 있다
            gap == 2L -> streak         // 하루 빠짐 = 오늘 하면 유예로 살릴 수 있다(희망을 남긴다)
            else -> 0                   // 이틀 넘게 빠졌다 = 끊겼다
        }
    }

    data class StreakResult(
        val streak: Int,
        val lastFreeze: LocalDate?,
        val changed: Boolean,
        val freezeUsed: Boolean = false
    )

    // ──────────────────────────────────────────────────────────────
    // 2. 오늘의 퀴즈 — 날짜로 정해지는 고정 세트
    // ──────────────────────────────────────────────────────────────

    /**
     * 날짜를 시드로 [count]개를 뽑는다. **같은 날은 몇 번을 불러도 같은 문제**, 날이 바뀌면 다른 문제다.
     *
     * 서버가 없으므로 기준은 기기 로컬 날짜다(자정에 세트가 바뀐다).
     *
     * @param total     고를 수 있는 전체 문항 수
     * @param freshFrom 이 인덱스부터는 '새로 들어온 문항'으로 본다(호출자가 최신순 꼬리를 지정).
     *                  하루에 5문제쯤 새로 들어오는데 사용자가 그걸 영영 못 보는 일이 없도록,
     *                  최소 [FRESH_SLOTS]칸은 새 문항 몫으로 남긴다. 0이면 이 배려를 끄는 것.
     * @return 뽑힌 인덱스 목록(중복 없음, 원본 순서와 무관)
     */
    fun pickDailyIndices(
        date: LocalDate,
        total: Int,
        count: Int = DAILY_QUIZ_COUNT,
        freshFrom: Int = 0
    ): List<Int> {
        if (total <= 0) return emptyList()
        if (total <= count) return (0 until total).toList()

        val random = Random(date.toEpochDay())
        val picked = LinkedHashSet<Int>()

        // 새 문항 몫을 먼저 채운다(있을 때만).
        if (freshFrom in 1 until total) {
            val freshSize = total - freshFrom
            val freshQuota = minOf(FRESH_SLOTS, freshSize, count)
            var guard = 0
            while (picked.size < freshQuota && guard < freshSize * 4) {
                picked.add(freshFrom + random.nextInt(freshSize))
                guard++
            }
        }

        // 나머지는 전체에서 채운다.
        var guard = 0
        while (picked.size < count && guard < total * 8) {
            picked.add(random.nextInt(total))
            guard++
        }

        // 방어: 난수가 계속 겹쳐 못 채웠으면 앞에서부터 메운다(빈 세트로 나가는 것보다 낫다).
        var i = 0
        while (picked.size < count && i < total) {
            picked.add(i)
            i++
        }

        return picked.toList()
    }

    /** 오늘의 퀴즈에서 '새로 들어온 문항'에 배정하는 칸 수. */
    const val FRESH_SLOTS = 1

    // ──────────────────────────────────────────────────────────────
    // 2-1. 「새 문제 N개」 — 표시 상한과 복귀 사면 (2026-09-07)
    //
    // 하루 5문항이 계속 들어오는데 «마지막으로 본 뒤 늘어난 수»를 그대로 말하면,
    // 두 달 비운 사용자에게 「새 문제 300개」가 나간다. 그건 초대가 아니라 **청구서**다.
    // 오래 비운 사람일수록 큰 숫자를 받는 구조 = 돌아오기 가장 어려운 사람에게 가장 큰 벽.
    //
    // → 실제로 해야 하는 일은 어느 쪽이든 **오늘 5문제 한 판**이다. 바뀌는 건 숫자를 보여 주는 방식뿐이다.
    //   설계 근거는 doc/FEATURE_DAILY_PASSAGES.md §6.
    // ──────────────────────────────────────────────────────────────

    /** 「새 문제 N개」의 표시 상한. 넘으면 「20개+」로 자른다 — 세 자리 숫자를 내보내지 않는다. */
    const val QUIZ_NEW_CAP = 20

    /** 이 일수 이상 비웠다 돌아오면 밀린 숫자를 **아예 말하지 않는다**(복귀 사면). */
    const val RETURN_AMNESTY_DAYS = 7L

    /**
     * 오래 비운 뒤 돌아온 상태인가.
     *
     * ⚠️ 한 번도 푼 적 없는 사람은 «복귀»가 아니다(첫 방문). 사면은 **돌아온 사람에게만** 준다.
     */
    fun isReturningAfterBreak(
        today: LocalDate,
        lastDone: LocalDate?,
        amnestyDays: Long = RETURN_AMNESTY_DAYS
    ): Boolean {
        if (lastDone == null) return false
        if (lastDone.isAfter(today)) return false // 미래 날짜(기기 시간 변경) — 사면을 걸지 않는다
        return today.toEpochDay() - lastDone.toEpochDay() >= amnestyDays
    }

    /**
     * 「새 문제」를 사용자에게 어떻게 말할지.
     *
     * @param amnesty true면 **숫자를 말하지 않고** 「다시 시작」으로 맞이한다([count]는 0).
     * @param capped  실제 값이 상한을 넘어 잘렸다 → 「20개+」로 표시한다.
     */
    data class NewQuizNotice(
        val count: Int = 0,
        val capped: Boolean = false,
        val amnesty: Boolean = false
    ) {
        /** 숫자를 말해도 되는가. 사면 중이거나 셀 것이 없으면 말하지 않는다. */
        val hasNumber: Boolean get() = !amnesty && count > 0

        /** 사용자에게 보이는 수량 표기. 상한에서 잘렸으면 「20개+」. */
        val text: String get() = if (capped) "${count}개+" else "${count}개"
    }

    /**
     * @param total     지금 기기에 있는 전체 문항 수
     * @param seenCount 사용자가 마지막으로 인지한 전체 문항 수(기준점). 0 = 아직 기준이 없다
     */
    fun newQuizNotice(
        today: LocalDate,
        lastDone: LocalDate?,
        total: Int,
        seenCount: Int,
        cap: Int = QUIZ_NEW_CAP
    ): NewQuizNotice {
        // 복귀 사면이 가장 강하다 — 「밀린 것」 개념 자체를 지운다.
        if (isReturningAfterBreak(today, lastDone)) return NewQuizNotice(amnesty = true)

        // 기준점이 없으면 «전 문항이 새로 왔다»가 되어 버린다 → 아무 숫자도 말하지 않는다.
        if (seenCount <= 0) return NewQuizNotice()

        val raw = (total - seenCount).coerceAtLeast(0)
        return NewQuizNotice(count = minOf(raw, cap), capped = raw > cap)
    }

    // ──────────────────────────────────────────────────────────────
    // 3. 기록(최근 N일) 직렬화 — DataStore에 문자열 한 줄로 넣기 위한 것
    // ──────────────────────────────────────────────────────────────

    /** 하루치 성적. */
    data class DayScore(val date: LocalDate, val correct: Int, val total: Int)

    /** "2026-09-04:4/5" 를 줄바꿈으로 이은 형식. 사람이 읽을 수 있어 디버깅이 쉽다. */
    fun encodeHistory(history: List<DayScore>): String =
        history.sortedBy { it.date }
            .takeLast(HISTORY_KEEP_DAYS)
            .joinToString("\n") { "${it.date}:${it.correct}/${it.total}" }

    /** 깨진 줄은 조용히 버린다 — 기록 하나 때문에 화면이 죽는 쪽이 훨씬 나쁘다. */
    fun decodeHistory(raw: String?): List<DayScore> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split("\n").mapNotNull { line ->
            try {
                val (dateText, score) = line.trim().split(":", limit = 2)
                val (correct, total) = score.split("/", limit = 2)
                DayScore(LocalDate.parse(dateText), correct.toInt(), total.toInt())
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.date }
    }

    /** 오늘 기록을 넣는다(같은 날이 이미 있으면 **성적이 더 좋은 쪽**을 남긴다). */
    fun upsertHistory(history: List<DayScore>, today: DayScore): List<DayScore> {
        val existing = history.firstOrNull { it.date == today.date }
        val merged = if (existing == null || today.correct >= existing.correct) today else existing
        return (history.filterNot { it.date == today.date } + merged)
            .sortedBy { it.date }
            .takeLast(HISTORY_KEEP_DAYS)
    }

    // ──────────────────────────────────────────────────────────────
    // 4. 배지 — 돈이 드는 보상 대신 '자기 기록'을 준다
    // ──────────────────────────────────────────────────────────────

    /**
     * 보상(포인트·현금)은 주지 않기로 했다. 비용·정책 문제도 있지만,
     * **보상으로 온 사용자는 보상이 끊기면 전부 나가기** 때문이다.
     * 대신 돈이 안 드는 보상인 «자기 기록»을 준다 — 쌓인 것이 계속할 이유가 된다.
     */
    data class Badge(
        val id: String,
        val title: String,
        val description: String,
        val achieved: Boolean,
        /** 진행률 0f~1f. 아직 못 받은 배지도 «얼마나 왔는지»를 보여 준다. */
        val progress: Float
    )

    fun badges(streak: Int, bestStreak: Int, totalSolved: Int, totalCorrect: Int): List<Badge> {
        fun of(id: String, title: String, desc: String, value: Int, goal: Int) = Badge(
            id = id,
            title = title,
            description = desc,
            achieved = value >= goal,
            progress = if (goal <= 0) 1f else (value.toFloat() / goal).coerceIn(0f, 1f)
        )

        val accuracy = if (totalSolved > 0) totalCorrect * 100 / totalSolved else 0

        return listOf(
            of("first_step", "첫걸음", "오늘의 퀴즈를 하루 완료", bestStreak, 1),
            of("streak_3", "사흘의 힘", "3일 연속 완료", bestStreak, 3),
            of("streak_7", "일주일 개근", "7일 연속 완료", bestStreak, 7),
            of("streak_30", "한 달 개근", "30일 연속 완료", bestStreak, 30),
            of("solved_50", "오십 문항", "누적 50문항 풀이", totalSolved, 50),
            of("solved_200", "이백 문항", "누적 200문항 풀이", totalSolved, 200),
            of("solved_500", "오백 문항", "누적 500문항 풀이", totalSolved, 500),
            // 정확도 배지는 표본이 너무 적을 때 «100%» 가 떠 버리면 의미가 없다 → 30문항 이상일 때만 센다
            of("sharp", "명중", "30문항 이상 + 정답률 80%", if (totalSolved >= 30) accuracy else 0, 80)
        )
    }
}

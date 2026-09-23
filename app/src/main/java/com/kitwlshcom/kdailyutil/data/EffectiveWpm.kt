package com.kitwlshcom.kdailyutil.data

/**
 * 유효 속도(EWPM = 속도 × 이해도) — **「빠르게만」이 이득이 아니게 만드는 장치** (2026-09-23).
 *
 * 사용자가 09-14에 물었다: 「연습만 하면 속도가 올라가는 거죠? 퀴즈 안 풀어도?」 — **맞는 말이었다.**
 * 그때 재독은 빼게 고쳤지만(§할 일 0-F ①②③), **«읽지 않고 넘기기»는 그대로 남아 있었다.**
 * 700으로 밀어 놓고 ▶만 눌러도 기록이 되고, 그 기록이 다음 목표까지 올린다.
 *
 * EWPM = 500 WPM × 이해도 60% = **300**. 속독 훈련의 표준 지표이고, 이 곱셈이 붙는 순간
 * 「이해 안 되는 속도」는 숫자상으로도 이득이 아니게 된다.
 *
 * ## 🔴 미뤄 뒀던 걸림돌 셋과 그 답
 *
 * | 걸림돌 | 답 |
 * |---|---|
 * | **키가 없으면 이해도를 낼 수 없다** | **별도 트랙**으로 둔다. 기존 WPM 기록은 손대지 않고 그대로 «미검증»으로 남는다. 키 없는 사용자에게는 **아무것도 달라지지 않는다** |
 * | **퀴즈 점수가 세션과 이어져 있지 않다**(`recordComprehension`은 최고치만 갱신) | `wpmHistory` 형식을 **바꾸지 않는다.** 검증된 기록은 **다른 파일**에 따로 쌓는다 → 기존 데이터 이전이 **아예 필요 없다** |
 * | **표본이 작다** — 문항이 몇 개뿐이라 한 문제만 틀려도 점수가 크게 흔들리고, 그걸 속도에 곱하면 유효 속도가 들쭉날쭉해진다 | ① 문항이 [MIN_QUESTIONS]개 미만이면 **기록하지 않는다** ② 화면이 말하는 값은 **최근 [RECENT]회 평균** ③ 목표 조정도 평균으로만 한다 — **한 판으로는 목표가 움직이지 않는다** |
 *
 * ⚠️ **한 판의 EWPM은 «기록»이 아니라 «이번 판의 성적»이다.** 그 구분이 흐려지면
 * 문항 하나 차이로 최고 기록이 오르내리는 숫자가 되어 신뢰를 잃는다.
 *
 * 안드로이드 API를 쓰지 않는 순수 계산이다(단위 테스트 대상).
 */
object EffectiveWpm {

    /** 이 문항 수 미만이면 유효 속도로 세지 않는다. 2문항짜리는 50%p 단위로만 움직여 곱할 값이 못 된다. */
    const val MIN_QUESTIONS = 3

    /** 평균을 내는 창. `computeRecommendedWpm`이 쓰는 5회와 **같은 값**으로 맞췄다 — 두 숫자가 다른 창을 보면 설명할 수 없다. */
    const val RECENT = 5

    /** 기록이 없을 때의 시작 목표(일반 성인 평균에 가깝다). */
    const val DEFAULT_WPM = 300

    /** 이 이해도 이상이면 목표를 올린다. */
    const val RAISE_ABOVE = 70

    /** 이 이해도 미만이면 목표를 **낮춘다**. 그 사이는 «유지»다. */
    const val SLOW_BELOW = 50

    private const val RAISE_FACTOR = 1.08
    private const val SLOW_FACTOR = 0.92

    /** 속도 세션과 이해도 퀴즈를 **한 판으로 묶는 시간 창**. 어제 읽은 것에 오늘 점수를 붙이면 안 된다. */
    const val PAIR_WINDOW_MS = 2 * 60 * 60 * 1000L

    /**
     * 검증된 한 판. 🔴 **`wpmHistory`(숫자 목록)와 별개의 파일에 쌓는다** — 형식을 바꾸지 않으려는
     * 결정이 이 타입을 따로 만든 이유다.
     *
     * @param questions 문항 수. **표본 크기를 기록해 둔다** — 나중에 문항 수를 늘렸을 때
     *   옛 기록이 몇 문항짜리였는지 모르면 두 시기를 섞어 놓고 비교하게 된다.
     * @param date yyyyMMdd. 추이를 그릴 때 쓴다.
     */
    data class Record(
        val wpm: Int,
        val comprehension: Int,
        val questions: Int,
        val date: String
    ) {
        val ewpm: Int get() = of(wpm, comprehension)
    }

    /** EWPM = 속도 × 이해도. 이해도는 0~100으로 잘라 쓴다(깨진 값이 음수 속도가 되지 않게). */
    fun of(wpm: Int, comprehensionPercent: Int): Int =
        (wpm.coerceAtLeast(0) * comprehensionPercent.coerceIn(0, 100)) / 100

    /** 기록으로 남길 만한 판인가 — 문항이 너무 적으면 남기지 않는다. */
    fun isRecordable(wpm: Int, questions: Int): Boolean = wpm > 0 && questions >= MIN_QUESTIONS

    /** 최근 [RECENT]회 평균 유효 속도. 기록이 없으면 0. */
    fun averageEwpm(records: List<Record>): Int =
        records.takeLast(RECENT).let { if (it.isEmpty()) 0 else it.map { r -> r.ewpm }.average().toInt() }

    /** 최근 [RECENT]회 평균 이해도. 기록이 없으면 0. */
    fun averageComprehension(records: List<Record>): Int =
        records.takeLast(RECENT).let { if (it.isEmpty()) 0 else it.map { r -> r.comprehension }.average().toInt() }

    /** 다음 목표를 **어느 쪽으로** 움직일지. 화면이 그 이유를 말할 수 있어야 해서 값으로 돌려준다. */
    enum class Pace {
        /** 검증된 기록이 없다 — 예전(2026-09-14까지)과 똑같이 올린다. 🔴 키 없는 사용자가 여기다. */
        UNVERIFIED,
        /** 이해도가 따라오고 있다 → 올린다. */
        RAISE,
        /** 이해도가 어중간하다 → 속도를 **그대로 둔다**. */
        HOLD,
        /** 이해도가 낮다 → **낮춘다**. 이 경우까지 올리면 앱이 «읽지 말고 넘겨라»라고 시키는 셈이다. */
        SLOW
    }

    fun paceOf(verified: List<Record>): Pace {
        if (verified.isEmpty()) return Pace.UNVERIFIED
        val comp = averageComprehension(verified)
        return when {
            comp >= RAISE_ABOVE -> Pace.RAISE
            comp >= SLOW_BELOW -> Pace.HOLD
            else -> Pace.SLOW
        }
    }

    /**
     * 다음 목표 속도(WPM).
     *
     * 🔴 **검증된 기록이 없으면 2026-09-14까지의 계산과 한 글자도 다르지 않다**
     * (최근 5회 평균 × 1.08 · 10단위 · 150~700). 키가 없어 퀴즈를 못 푸는 사용자에게
     * 이 판이 **아무 영향도 주지 않는다**는 뜻이고, 그것이 이 기능을 미뤄 뒀던 첫째 이유의 답이다.
     *
     * @param history 기존 WPM 기록(미검증 포함). 목표의 **기준선**은 계속 이쪽이다 —
     *   퀴즈를 푼 판만 세면 표본이 너무 적어진다.
     * @param verified 이해도까지 확인된 기록. **방향만** 정한다.
     */
    fun nextTarget(history: List<Int>, verified: List<Record>): Int {
        if (history.isEmpty()) return DEFAULT_WPM
        val base = history.takeLast(RECENT).average()
        val factor = when (paceOf(verified)) {
            Pace.UNVERIFIED, Pace.RAISE -> RAISE_FACTOR
            Pace.HOLD -> 1.0
            Pace.SLOW -> SLOW_FACTOR
        }
        return ((base * factor).toInt() / 10 * 10).coerceIn(150, 700)
    }
}

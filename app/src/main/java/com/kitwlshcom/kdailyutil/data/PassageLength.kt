package com.kitwlshcom.kdailyutil.data

import kotlin.math.ceil

/**
 * 지문의 «길이»를 사람이 고를 수 있는 말로 바꾼다 (2026-09-14).
 *
 * 🔴 **왜 필요한가** — 지문 목록이 길이를 한마디도 하지 않았다. 그런데 실측해 보니
 * 시스템 지문이 **206~239자(평균 224자)**이고, 이는 300 WPM에서 **약 11초**다.
 * 「어느 걸 고를까」에 답하려면 «얼마나 걸리나»를 알아야 하고, 앞으로 장문 지문이 들어오면
 * (주 1회 800~1,200자 안) **모르고 눌렀다가 당황하는** 일이 생긴다.
 *
 * ⚠️ **«안 읽음 배지»와는 성격이 다르다.** 그것은 «네가 밀린 과제»를 세지만 이것은
 * «이걸 고르면 얼마나 걸리나»를 말한다. 목록을 과제로 만들지 않는다(§6-6).
 *
 * 🔴 **어절을 세는 방식은 RSVP·페이서와 반드시 같아야 한다** — 화면이 「약 12초」라고 했는데
 * 실제로 20초가 걸리면 그 표시는 없느니만 못하다. 그래서 같은 규칙(공백으로 쪼갠다)을 쓰고
 * 테스트로 묶어 뒀다.
 *
 * 안드로이드 API를 쓰지 않는 순수 함수들이다(단위 테스트 대상).
 */
object PassageLength {

    /** 기록이 없는 사람에게 쓸 기준 속도. 앱의 추천 목표 초기값과 같다. */
    const val DEFAULT_WPM = 300

    /**
     * 이 글자 수 이상이면 **«장문»**으로 본다 (2026-09-14).
     *
     * 🔴 **별도 필드를 만들지 않고 «길이»로 판정한다.** 로봇이 주 1회(**토요일 KST** · 2026-09-15에 일요일에서 바꿨다) 800~1,200자를
     * 만드는데, JSON에 `kind` 같은 필드를 새로 넣으면 **구버전 앱이 모르는 값**이 생긴다.
     * 길이는 이미 본문에 들어 있으므로 형식을 바꾸지 않고도 판정할 수 있다.
     *
     * 경계값 500자는 **평소 지문(최대 250자)과 장문(최소 800자) 사이의 빈 구간**이다.
     * 어느 쪽 규격이 조금 흔들려도 오판하지 않는다.
     * 🔴 로봇 쪽 값(`update_passages.py`의 `LONG_MIN_CHARS`)을 바꾸면 여기도 같이 본다.
     */
    const val LONG_CHARS = 500

    /** 「오늘은 긴 지문」인가. 사용자가 **시작 전에** 알아야 하는 정보다. */
    fun isLong(text: String): Boolean = text.trim().length >= LONG_CHARS

    /**
     * 어절 수. 🔴 `RsvpModule`이 화면에 뿌리는 단위와 **같은 규칙**이다
     * (`trim().split(Regex("""\s+""")).filter { it.isNotBlank() }`).
     */
    fun wordCount(text: String): Int =
        text.trim().split(Regex("""\s+""")).count { it.isNotBlank() }

    /** 이 속도로 읽으면 몇 초 걸리는가. 0초로 내려가지 않는다(«0초»는 정보가 아니다). */
    fun seconds(text: String, wpm: Int = DEFAULT_WPM): Int {
        val w = wordCount(text)
        if (w <= 0) return 0
        val speed = wpm.coerceAtLeast(60)
        return ceil(w.toDouble() / speed * 60.0).toInt().coerceAtLeast(1)
    }

    /** 「약 12초」 · 「약 1분 5초」 · 「약 2분」. 1분을 넘으면 초 단위는 뭉갠다(고르는 데 필요 없다). */
    fun durationLabel(text: String, wpm: Int = DEFAULT_WPM): String {
        val s = seconds(text, wpm)
        if (s <= 0) return ""
        if (s < 60) return "약 ${s}초"
        val m = s / 60
        val rest = s % 60
        return if (rest == 0) "약 ${m}분" else "약 ${m}분 ${rest}초"
    }

    /** 목록 한 줄에 붙이는 문구 — 「224자 · 약 12초」. 글자 수가 없으면 빈 문자열. */
    fun label(text: String, wpm: Int = DEFAULT_WPM): String {
        val chars = text.trim().length
        if (chars <= 0) return ""
        return "${chars}자 · ${durationLabel(text, wpm)}"
    }
}

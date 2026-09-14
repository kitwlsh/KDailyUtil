package com.kitwlshcom.kdailyutil.data

import java.security.MessageDigest

/**
 * 지문 하나를 가리키는 **짧은 열쇠** (2026-09-14).
 *
 * 🔴 **왜 필요한가 — 「같은 지문을 다시 읽으면 속도가 올라간다」**(사용자 지적).
 * 내용을 아는 글은 더 빠른 속도로도 «따라갈» 수 있다. 그 기록이 실력 향상으로 잡히면
 * WPM 추이가 거짓말이 되고, 「추천 목표」(최근 5회 평균 × 1.08)까지 함께 부풀어
 * **다음 목표가 더 헛돌게** 된다. 그래서 초독과 재독을 갈라야 한다.
 *
 * 🔴 **id가 아니라 «본문»으로 가른다.** 지문이 오는 길이 넷이다 —
 * 로봇 지문 · 보관함 지문 · 붙여넣은 텍스트 · 촬영(OCR). 뒤 둘은 id가 없고,
 * 같은 글을 보관함에 두 번 담으면 id는 달라도 **사람에게는 같은 글**이다.
 * 본문을 기준으로 삼으면 네 경로가 한 잣대로 묶인다.
 *
 * 🔴 **본문 전체를 저장하지 않는다.** 읽은 지문이 쌓이면 그만큼 파일이 커진다.
 * 해시 앞 16자리면 충돌이 사실상 없고 길이는 고정이다.
 *
 * 안드로이드 API를 쓰지 않는 순수 함수다(단위 테스트 대상).
 */
object PassageKey {

    /**
     * 비교용으로 다듬은 본문 — **공백 차이는 같은 글로 본다.**
     * OCR·붙여넣기는 줄바꿈과 공백이 매번 달라서, 그대로 비교하면 같은 글이 매번 «새 글»이 된다.
     */
    internal fun normalize(text: String): String =
        text.trim().replace(Regex("""\s+"""), " ")

    /** 지문의 열쇠. 빈 글이면 빈 문자열(= 기록하지 않는다는 뜻). */
    fun of(text: String): String {
        val norm = normalize(text)
        if (norm.isEmpty()) return ""
        val digest = MessageDigest.getInstance("SHA-256").digest(norm.toByteArray(Charsets.UTF_8))
        return digest.take(8).joinToString("") { "%02x".format(it) }
    }
}

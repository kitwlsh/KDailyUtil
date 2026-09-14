package com.kitwlshcom.kdailyutil.data

/**
 * 지문으로 하는 독서 훈련 3종 (2026-09-08).
 *
 * 왜 별도 타입인가 — 화면 안의 `ReadingModule`은 **허브·결과·통계까지 포함한 «지금 무슨 화면인가»**이고,
 * 이쪽은 **«지문을 태워 WPM을 내는 훈련»** 만 모은 목록이다. 둘을 한 enum으로 합치면
 * 「다음 훈련」이 결과 화면이나 통계로 넘어가는 사고가 난다.
 *
 * 🔴 **워밍업·안구 추적·쉐도잉은 여기 넣지 않는다.** 셋 다 지문을 쓰지 않아
 * 결과 화면(WPM)에 도달하지 않는다 — 「다음 훈련」 순환에 끼면 WPM이 없는 화면으로 떨어진다.
 * 그 셋은 허브의 훈련 목록에 그대로 남아 있다.
 *
 * `key`는 DataStore에 저장되는 값이다 → 🔴 **한 번 정한 문자열을 바꾸지 말 것.**
 * 바꾸면 기존 사용자의 «마지막 훈련»이 통째로 인식 불가가 되어 매번 고르기 창이 뜬다.
 * (인식 못 하는 값은 [fromKey]가 null로 돌려주고, 호출부가 «처음 쓰는 사람»과 같게 다룬다.)
 */
enum class ReadingTrainingModule(
    val key: String,
    val label: String,
    val emoji: String
) {
    PACER("pacer", "리듬 페이서", "🎯"),
    RSVP("rsvp", "단어 점멸 (RSVP)", "⚡"),
    CHUNK("chunk", "묶어 읽기 (청크)", "🔭");

    /** 버튼·시트에 쓰는 표시용 이름. 🔴 버튼 이름이 «무엇이 열리는지»를 말해야 한다. */
    val display: String get() = "$emoji $label"

    companion object {
        /**
         * **기본 훈련의 초기값**(2026-09-14부터 진짜 기본값이다).
         *
         * 사용자가 ☆로 정하기 전까지 지문 카드의 시작 버튼은 이것으로 시작한다.
         * 🔴 **리듬 페이서여야 한다** — 목록의 첫 훈련이자 가장 기본이 되는 훈련이고,
         * 사용자가 「새 지문은 늘 첫 번째 훈련이면 좋겠다」고 요청한 값이다(2026-09-14).
         *
         * ⚠️ 예전에는 «표시용 폴백»이었고 처음 쓰는 사람에게는 고르기 창을 먼저 띄웠다.
         * 시작 버튼에 **훈련 이름이 적히게 된 뒤로는** 그 우회가 필요 없어졌다 —
         * 「▶ 리듬 페이서 시작」은 무엇이 열리는지 이미 말하고 있다.
         */
        val DEFAULT = PACER

        /** 저장된 키 → 모듈. 모르는 값·null이면 null (= 아직 고른 적 없음과 같게 다룬다). */
        fun fromKey(key: String?): ReadingTrainingModule? =
            entries.firstOrNull { it.key == key }

        /**
         * 「다음 훈련」 — 목록 순서대로 돌고 마지막에서 처음으로 돌아온다.
         * 순환이라 «끝»이 없다: 한 판 더 하려는 사람이 막다른 곳에 닿지 않는다.
         */
        fun next(current: ReadingTrainingModule): ReadingTrainingModule =
            entries[(current.ordinal + 1) % entries.size]
    }
}

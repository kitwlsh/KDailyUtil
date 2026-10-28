package com.kitwlshcom.kdailyutil.data

/**
 * 읽은 지문 기록 — **«언제» 읽었는지까지** 남긴다 (2026-10-28).
 *
 * 🔴 **왜 바꿨나 — «한 번 읽은 지문은 영영 재독»**(09-16 리뷰 1번 · 그때부터 열려 있던 일).
 * 예전에는 열쇠([PassageKey])만 모아 «읽었나/안 읽었나»만 봤다. 그래서 반년 전에 읽은 글을
 * 다시 읽어도 재독으로 잡혀 속도 기록에서 빠졌다. 그 사이 내용은 거의 잊혔는데도 그렇다.
 * 이제는 **마지막으로 읽은 날에서 [FRESH_AFTER_DAYS]일이 지나면 다시 «처음»으로 친다.**
 *
 * 🔴 **기준은 «마지막으로 읽은 날»이다**(처음 읽은 날이 아니다). 재독도 기억을 되살리므로
 * 60일마다 같은 글을 읽는 사람에게 그 글은 계속 «아는 글»이다.
 *
 * 🔴 **기존 기록(날짜 없는 열쇠)은 «이전한 날 읽은 것»으로 옮긴다** — 날짜를 모르니
 * 보수적으로 잡는다. 앞당겨 잡으면 업데이트하는 순간 옛 글들이 한꺼번에 «처음»이 되어
 * 속도 기록이 갑자기 부풀 수 있다.
 *
 * 저장 형식 = `"열쇠|epochDay"` 문자열 집합(DataStore stringSet). [FRESH_AFTER_DAYS]가 지난
 * 항목은 «없음»과 같으므로 기록할 때 버린다 — 그래서 집합이 끝없이 자라지 않는다.
 *
 * 안드로이드 API를 쓰지 않는 순수 함수다(단위 테스트 대상).
 */
object PassageReadLog {

    /** 이만큼 지나면 다시 «처음 읽는 글»로 친다. 석 달이면 내용 기억이 거의 남지 않는다. */
    const val FRESH_AFTER_DAYS = 90L

    /** 저장된 문자열 집합 → (열쇠 → 마지막으로 읽은 epochDay). 깨진 항목은 버린다. */
    fun decode(raw: Set<String>): Map<String, Long> =
        raw.mapNotNull { entry ->
            val key = entry.substringBefore('|', "")
            val day = entry.substringAfter('|', "").toLongOrNull()
            if (key.isBlank() || day == null) null else key to day
        }.toMap()

    fun encode(log: Map<String, Long>): Set<String> =
        log.entries.mapTo(mutableSetOf()) { (key, day) -> "$key|$day" }

    /** 날짜 없는 옛 기록을 [today]에 읽은 것으로 옮긴다. */
    fun migrate(legacyKeys: Set<String>, today: Long): Map<String, Long> =
        legacyKeys.filter { it.isNotBlank() }.associateWith { today }

    /**
     * [key]를 [today]에 읽었다고 기록한다.
     * @return (처음 읽는가, 갱신된 기록). 오래된 항목은 갱신된 기록에서 빠진다.
     */
    fun record(log: Map<String, Long>, key: String, today: Long): Pair<Boolean, Map<String, Long>> {
        val last = log[key]
        val first = last == null || today - last >= FRESH_AFTER_DAYS
        val updated = (log + (key to today)).filterValues { today - it < FRESH_AFTER_DAYS }
        return first to updated
    }
}

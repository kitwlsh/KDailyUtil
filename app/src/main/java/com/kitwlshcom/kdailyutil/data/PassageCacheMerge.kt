package com.kitwlshcom.kdailyutil.data

import com.kitwlshcom.kdailyutil.data.repository.RemotePassage

/**
 * 로봇 지문 캐시 합치기 — **«2년 창»을 없앤다** (2026-10-28).
 *
 * 🔴 **왜 바꿨나** — 동기화는 작년·올해 파일 2개만 받고, 예전에는 받은 것만으로 캐시를
 * **통째로 덮어썼다.** 그래서 2028-01-01 첫 동기화에 2026년 지문이 기기에서 전부 사라질
 * 참이었다(2026-09-21 발견). 이제는 받은 것을 **기존 캐시에 더한다.**
 *
 * 🔴 **그렇다고 무조건 쌓지 않는다 — 받은 연도는 원격이 정본이다.** 로봇 쪽에서 지문을
 * 지우거나 고치면(신고 처리 등) 그 해 파일을 받는 순간 기기에도 반영돼야 한다.
 * 그래서 이번에 **받는 데 성공한 연도**에 속한 옛 캐시는 버리고 받은 것으로 갈음한다.
 * 받지 못한 연도(통신 실패·404)와 창 밖의 옛 연도는 손대지 않는다.
 * 도착일([RemotePassage.createdAt])이 깨진 지문은 연도를 모르니 «받은 연도»로 보고 원격을 따른다.
 *
 * 🔴 **총 편수 상한 [MAX_PASSAGES]** — 하루 1편이면 3년 가까이 담는다(한 편 ≈ 1KB · 전체 ≈ 1MB).
 * 넘치면 **오래된 것부터** 버린다.
 *
 * 안드로이드 API를 쓰지 않는 순수 함수다(단위 테스트 대상).
 */
object PassageCacheMerge {

    const val MAX_PASSAGES = 1000

    /**
     * @param cached 지금 기기에 있는 캐시
     * @param fetched 이번에 받은 지문(받는 데 성공한 연도의 파일들)
     * @param fetchedYears 받는 데 **성공한**(200) 연도
     * @return id 오름차순으로 정렬된 새 캐시
     */
    fun merge(
        cached: List<RemotePassage>,
        fetched: List<RemotePassage>,
        fetchedYears: Set<Int>,
        max: Int = MAX_PASSAGES
    ): List<RemotePassage> {
        val kept = cached.filter { p -> p.createdAt != null && p.createdAt.year !in fetchedYears }
        val byId = LinkedHashMap<Long, RemotePassage>()
        kept.forEach { byId[it.id] = it }
        fetched.forEach { byId[it.id] = it }  // 같은 id면 받은 것이 이긴다
        return byId.values.sortedBy { it.id }.takeLast(max)
    }
}

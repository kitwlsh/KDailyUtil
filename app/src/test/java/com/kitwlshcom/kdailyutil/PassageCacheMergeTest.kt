package com.kitwlshcom.kdailyutil

import com.kitwlshcom.kdailyutil.data.PassageCacheMerge
import com.kitwlshcom.kdailyutil.data.repository.RemotePassage
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * 로봇 지문 캐시 합치기 — «2년 창» 해소 (2026-10-28).
 *
 * 🔴 두 고장을 함께 막는다: 옛 연도 지문이 해가 바뀌며 **사라지는 것**, 그리고
 * 로봇이 지운 지문이 기기에 **영영 남는 것**(받은 연도는 원격이 정본이어야 한다).
 */
class PassageCacheMergeTest {

    private fun p(id: Long, date: String?, text: String = "본문$id") =
        RemotePassage(id, "제목$id", "", text, date?.let { LocalDate.parse(it) })

    private fun ids(list: List<RemotePassage>) = list.map { it.id }

    /** 2028-01-01 첫 동기화 = 2027·2028 파일만 받는다. 2026년 지문이 남아야 한다. */
    @Test
    fun `받지 않은 옛 연도 지문은 남는다`() {
        val cached = listOf(p(20261231001, "2026-12-31"), p(20270105001, "2027-01-05"))
        val fetched = listOf(p(20270105001, "2027-01-05"), p(20280101001, "2028-01-01"))

        val merged = PassageCacheMerge.merge(cached, fetched, fetchedYears = setOf(2027, 2028))

        assertEquals(listOf(20261231001, 20270105001, 20280101001), ids(merged))
    }

    /** 🔴 받은 연도에서 원격에 없는 지문은 지워진 것이다(신고 처리 등). */
    @Test
    fun `받은 연도에서 원격이 지운 지문은 기기에서도 빠진다`() {
        val cached = listOf(p(20261020001, "2026-10-20"), p(20261021001, "2026-10-21"))
        val fetched = listOf(p(20261021001, "2026-10-21"))

        val merged = PassageCacheMerge.merge(cached, fetched, fetchedYears = setOf(2025, 2026))

        assertEquals(listOf(20261021001), ids(merged))
    }

    /** 통신 실패·404로 못 받은 연도는 손대지 않는다 — 못 받은 것을 «지워졌다»로 읽으면 안 된다. */
    @Test
    fun `받지 못한 연도의 캐시는 그대로 둔다`() {
        val cached = listOf(p(20251230001, "2025-12-30"), p(20261020001, "2026-10-20"))
        val fetched = listOf(p(20261020001, "2026-10-20"), p(20261021001, "2026-10-21"))

        val merged = PassageCacheMerge.merge(cached, fetched, fetchedYears = setOf(2026))

        assertEquals(listOf(20251230001, 20261020001, 20261021001), ids(merged))
    }

    @Test
    fun `같은 id면 받은 쪽 내용이 이긴다`() {
        val merged = PassageCacheMerge.merge(
            cached = listOf(p(20261020001, "2026-10-20", "옛 본문")),
            fetched = listOf(p(20261020001, "2026-10-20", "고친 본문")),
            fetchedYears = setOf(2026)
        )
        assertEquals("고친 본문", merged.single().text)
    }

    /** 도착일이 깨진 캐시는 연도를 몰라 남길 근거가 없다 — 원격을 따른다. */
    @Test
    fun `도착일이 깨진 캐시 지문은 받은 것에 없으면 빠진다`() {
        val merged = PassageCacheMerge.merge(
            cached = listOf(p(1, null)),
            fetched = listOf(p(20261020001, "2026-10-20")),
            fetchedYears = setOf(2026)
        )
        assertEquals(listOf(20261020001), ids(merged))
    }

    @Test
    fun `상한을 넘으면 오래된 것부터 버린다`() {
        val cached = listOf(p(20240101001, "2024-01-01"), p(20240102001, "2024-01-02"))
        val fetched = listOf(p(20261020001, "2026-10-20"))

        val merged = PassageCacheMerge.merge(cached, fetched, setOf(2026), max = 2)

        assertEquals(listOf(20240102001, 20261020001), ids(merged))
    }
}

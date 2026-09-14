package com.kitwlshcom.kdailyutil

import com.kitwlshcom.kdailyutil.data.ReadingTrainingModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 지문 훈련 모듈 — 「다음 훈련」 순환과 저장 키 (2026-09-08).
 *
 * 무엇을 지키는 테스트인가:
 * 1. 결과 화면의 「다음: X」가 **어디서 시작해도 막다른 곳에 닿지 않는다**(순환).
 * 2. DataStore에 저장되는 `key`가 **바뀌지 않는다** — 바꾸면 기존 사용자가 정해 둔 「기본 훈련」이
 *    통째로 인식 불가가 되어 전원이 리듬 페이서로 되돌아간다. 조용히 나빠지는 회귀라 여기서 못 박는다.
 * 3. 지문을 쓰지 않는 훈련(워밍업·안구 추적)이 **순환에 섞여 들어오지 않는다** — 섞이면
 *    WPM이 없는 화면으로 「다음 훈련」이 떨어진다.
 */
class ReadingTrainingModuleTest {

    @Test
    fun `다음 훈련은 목록 순서대로 돈다`() {
        assertEquals(ReadingTrainingModule.RSVP, ReadingTrainingModule.next(ReadingTrainingModule.PACER))
        assertEquals(ReadingTrainingModule.CHUNK, ReadingTrainingModule.next(ReadingTrainingModule.RSVP))
    }

    @Test
    fun `마지막 훈련의 다음은 처음으로 돌아온다 - 막다른 곳이 없다`() {
        assertEquals(ReadingTrainingModule.PACER, ReadingTrainingModule.next(ReadingTrainingModule.CHUNK))
    }

    @Test
    fun `어느 훈련에서 시작해도 순환하면 전부 한 번씩 나온다`() {
        ReadingTrainingModule.entries.forEach { start ->
            val seen = mutableListOf(start)
            var cur = start
            repeat(ReadingTrainingModule.entries.size - 1) {
                cur = ReadingTrainingModule.next(cur)
                seen.add(cur)
            }
            assertEquals(
                "$start 에서 출발한 순환이 전부를 돌지 못했다: $seen",
                ReadingTrainingModule.entries.toSet(),
                seen.toSet()
            )
            // 한 바퀴를 더 돌면 출발점으로 정확히 복귀한다.
            assertEquals(start, ReadingTrainingModule.next(cur))
        }
    }

    @Test
    fun `저장 키는 고정이다 - 바꾸면 기존 사용자의 마지막 훈련이 날아간다`() {
        assertEquals("pacer", ReadingTrainingModule.PACER.key)
        assertEquals("rsvp", ReadingTrainingModule.RSVP.key)
        assertEquals("chunk", ReadingTrainingModule.CHUNK.key)
    }

    @Test
    fun `저장한 키를 그대로 되읽는다`() {
        ReadingTrainingModule.entries.forEach { m ->
            assertEquals(m, ReadingTrainingModule.fromKey(m.key))
        }
    }

    @Test
    fun `모르는 키와 null은 아직 고른 적 없음으로 다룬다`() {
        assertNull(ReadingTrainingModule.fromKey(null))
        assertNull(ReadingTrainingModule.fromKey(""))
        assertNull(ReadingTrainingModule.fromKey("eye"))        // 순환에 없는 훈련
        assertNull(ReadingTrainingModule.fromKey("warmup"))     // 순환에 없는 훈련
        assertNull(ReadingTrainingModule.fromKey("PACER"))      // 대소문자가 다르면 다른 키다
    }

    @Test
    fun `지문을 쓰지 않는 훈련은 순환에 들어 있지 않다`() {
        val keys = ReadingTrainingModule.entries.map { it.key }
        assertTrue("워밍업이 순환에 섞였다", "warmup" !in keys)
        assertTrue("안구 추적이 순환에 섞였다", "eye" !in keys)
        assertTrue("쉐도잉이 순환에 섞였다", "shadow" !in keys)
        assertEquals(3, ReadingTrainingModule.entries.size)
    }

    @Test
    fun `버튼에 쓸 이름이 비어 있지 않다 - 무엇이 열리는지 말해야 한다`() {
        ReadingTrainingModule.entries.forEach { m ->
            assertTrue("${m.key}의 label이 비었다", m.label.isNotBlank())
            assertTrue("${m.key}의 emoji가 비었다", m.emoji.isNotBlank())
            assertTrue("${m.key}의 display에 label이 없다", m.display.contains(m.label))
        }
    }

    @Test
    fun `기본값은 순환 안에 있는 훈련이다`() {
        assertTrue(ReadingTrainingModule.DEFAULT in ReadingTrainingModule.entries)
    }

    // ── 기본 훈련 (2026-09-14) ──────────────────────────────────────

    /**
     * 🔴 **사용자 요청을 값으로 고정한다**(2026-09-14):
     * 「새로운 지문에는 항상 첫 번째 연습(리듬 페이서)으로 되어 있는 게 낫지 않을지」.
     * 기본 훈련의 초기값이 곧 «새 지문을 열었을 때 나오는 훈련»이다.
     */
    @Test
    fun `기본 훈련의 초기값은 목록의 첫 훈련인 리듬 페이서다`() {
        assertEquals(ReadingTrainingModule.PACER, ReadingTrainingModule.DEFAULT)
        assertEquals(
            "초기값은 «목록의 첫 훈련»이어야 한다 — 순서를 바꾸면 이 테스트가 먼저 알려 준다",
            ReadingTrainingModule.entries.first(),
            ReadingTrainingModule.DEFAULT
        )
    }

    /**
     * 저장된 값이 깨져 있어도 화면이 «고를 것 없음»으로 비지 않는다.
     * 저장소가 쓰는 폴백 경로(`fromKey(...) ?: DEFAULT`)를 그대로 재현한 것이다.
     */
    @Test
    fun `저장값이 깨져 있으면 기본 훈련으로 떨어진다`() {
        for (broken in listOf(null, "", "eye", "warmup", "PACER")) {
            assertEquals(
                ReadingTrainingModule.DEFAULT,
                ReadingTrainingModule.fromKey(broken) ?: ReadingTrainingModule.DEFAULT
            )
        }
    }
}

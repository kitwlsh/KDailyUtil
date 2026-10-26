package com.kitwlshcom.kdailyutil

import com.kitwlshcom.kdailyutil.data.ai.AiAccess
import com.kitwlshcom.kdailyutil.data.ai.AiArea
import com.kitwlshcom.kdailyutil.data.ai.AiKeyProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * AI 키 해석의 단일 진입점 (2026-10-26 · AI 체험판 1단계).
 *
 * 🔴 **무엇을 지키는 테스트인가** — 14곳에 흩어져 있던 «키 꺼내기»를 한 곳으로 모으면서
 * **동작이 하나도 달라지지 않았다**는 것이다. 본인 키가 있으면 그 키, 없으면 «못 씀».
 * 체험 카운터가 붙은 뒤에도 **본인 키 사용자는 이 결과가 그대로**여야 한다(개발자 비용 0원 → 무제한).
 */
class AiKeyProviderTest {

    private fun provider(key: String?) = AiKeyProvider { key }

    @Test
    fun `본인 키가 있으면 그 키를 쓴다 — 분야와 무관하다`() = runBlocking {
        for (area in AiArea.values()) {
            assertEquals(AiAccess.UserKey("abc"), provider("abc").resolve(area))
        }
    }

    @Test
    fun `키가 없으면 못 쓴다`() = runBlocking {
        val access = provider(null).resolve(AiArea.NEWS)
        assertEquals(AiAccess.Unavailable(AiAccess.Reason.NO_KEY), access)
        assertNull(access.keyOrNull)
    }

    @Test
    fun `빈 키는 없는 키다`() = runBlocking {
        assertNull(provider("").resolve(AiArea.STOCK).keyOrNull)
        assertNull(provider("   ").resolve(AiArea.LEARNING).keyOrNull)
    }
}

package com.kitwlshcom.kdailyutil

import com.kitwlshcom.kdailyutil.data.ai.AiAccess
import com.kitwlshcom.kdailyutil.data.ai.AiArea
import com.kitwlshcom.kdailyutil.data.ai.AiKeyProvider
import com.kitwlshcom.kdailyutil.data.ai.AiTrialConfig
import com.kitwlshcom.kdailyutil.data.ai.errorMessage
import com.kitwlshcom.kdailyutil.data.ai.unavailableMessage
import com.kitwlshcom.kdailyutil.data.remote.GeminiManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AI 키 해석의 단일 진입점 (2026-10-26 · AI 체험판 1단계).
 *
 * 🔴 **무엇을 지키는 테스트인가** — 14곳에 흩어져 있던 «키 꺼내기»를 한 곳으로 모으면서
 * **동작이 하나도 달라지지 않았다**는 것이다. 본인 키가 있으면 그 키, 없으면 «못 씀».
 * 체험 카운터가 붙은 뒤에도 **본인 키 사용자는 이 결과가 그대로**여야 한다(개발자 비용 0원 → 무제한).
 */
class AiKeyProviderTest {

    private fun provider(key: String?) = AiKeyProvider(userKey = { key })

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

    // ── 🎁 체험 카운터 ──

    /** 분야별 사용 횟수를 메모리에 두는 가짜 저장소. */
    private class Counter {
        val used = mutableMapOf<AiArea, Int>()
        fun provider(
            userKey: String? = null,
            trialKey: String? = "TRIAL",
            cfg: AiTrialConfig = AiTrialConfig.DEFAULT,
        ) = AiKeyProvider(
            userKey = { userKey },
            trialKey = trialKey,
            trialConfig = { cfg },
            trialUsed = { used[it] ?: 0 },
            addTrialUse = { used[it] = (used[it] ?: 0) + 1 },
        )
    }

    @Test
    fun `체험 키가 없는 빌드는 지금과 똑같다 — 키 없으면 못 쓴다`() = runBlocking {
        val c = Counter()
        assertEquals(
            AiAccess.Unavailable(AiAccess.Reason.NO_KEY),
            c.provider(trialKey = null).resolve(AiArea.NEWS)
        )
    }

    @Test
    fun `체험은 분야별 한도까지 쓰고 그다음은 소진이다`() = runBlocking {
        val c = Counter()
        val p = c.provider()
        repeat(7) {
            val access = p.resolve(AiArea.NEWS)
            assertTrue("${it + 1}번째는 쓸 수 있어야 한다", access is AiAccess.Trial)
            p.recordUse(access)
        }
        assertEquals(AiAccess.Unavailable(AiAccess.Reason.TRIAL_EXHAUSTED), p.resolve(AiArea.NEWS))
    }

    @Test
    fun `한 분야를 다 써도 다른 분야는 남아 있다`() = runBlocking {
        val c = Counter()
        c.used[AiArea.NEWS] = 7
        val p = c.provider()
        assertTrue(p.resolve(AiArea.LEARNING) is AiAccess.Trial)
        assertEquals(6, (p.resolve(AiArea.STOCK) as AiAccess.Trial).remaining)
    }

    @Test
    fun `resolve만 하고 recordUse를 안 부르면 깎이지 않는다 — 실패는 세지 않는다`() = runBlocking {
        val c = Counter()
        val p = c.provider()
        repeat(20) { p.resolve(AiArea.STOCK) }
        assertEquals(6, (p.resolve(AiArea.STOCK) as AiAccess.Trial).remaining)
    }

    @Test
    fun `본인 키가 있으면 체험보다 먼저고, 써도 체험이 줄지 않는다`() = runBlocking {
        val c = Counter()
        val p = c.provider(userKey = "MINE")
        val access = p.resolve(AiArea.NEWS)
        assertEquals(AiAccess.UserKey("MINE"), access)
        repeat(100) { p.recordUse(access) }
        assertEquals(0, c.used[AiArea.NEWS] ?: 0)
    }

    @Test
    fun `본인 키를 지우면 남은 체험부터 이어진다`() = runBlocking {
        val c = Counter()
        c.used[AiArea.LEARNING] = 3
        assertEquals(4, (c.provider(userKey = null).resolve(AiArea.LEARNING) as AiAccess.Trial).remaining)
    }

    @Test
    fun `원격으로 끄면 횟수가 남아도 못 쓴다`() = runBlocking {
        val c = Counter()
        val off = AiTrialConfig.DEFAULT.copy(enabled = false)
        assertEquals(
            AiAccess.Unavailable(AiAccess.Reason.TRIAL_DISABLED),
            c.provider(cfg = off).resolve(AiArea.NEWS)
        )
    }

    @Test
    fun `원격으로 꺼도 본인 키 사용자는 그대로다`() = runBlocking {
        val c = Counter()
        val off = AiTrialConfig.DEFAULT.copy(enabled = false)
        assertEquals(AiAccess.UserKey("MINE"), c.provider(userKey = "MINE", cfg = off).resolve(AiArea.NEWS))
    }

    // ── 안내 ──

    @Test
    fun `체험 없는 빌드에서는 화면마다 쓰던 «키 등록» 문구가 그대로다`() = runBlocking {
        val access = Counter().provider(trialKey = null).resolve(AiArea.NEWS)
        assertEquals("원래 문구", access.unavailableMessage("원래 문구"))
    }

    @Test
    fun `소진과 꺼짐은 «키 등록»이 아니라 그 사유를 말한다`() = runBlocking {
        val c = Counter()
        c.used[AiArea.STOCK] = 6
        val exhausted = c.provider().resolve(AiArea.STOCK).unavailableMessage("원래 문구")
        assertTrue(exhausted, exhausted.contains("체험을 모두 썼어요"))
        val off = c.provider(cfg = AiTrialConfig.DEFAULT.copy(enabled = false))
            .resolve(AiArea.NEWS).unavailableMessage("원래 문구")
        assertTrue(off, off.contains("체험을 쓸 수 없어요"))
    }

    @Test
    fun `체험 중 429는 «소진»이라 말하지 않고 횟수가 줄지 않았다고 말한다`() = runBlocking {
        val access = Counter().provider().resolve(AiArea.NEWS)
        val msg = access.errorMessage(Exception("429 RESOURCE_EXHAUSTED Please retry in 12.3s"))
        assertTrue(msg, msg.contains("체험 횟수는 줄지 않았어요"))
        assertTrue(msg, !msg.contains("모두 썼어요"))
    }

    @Test
    fun `체험 키가 막히면 «내 키를 확인하라»가 아니라 본인 키를 안내한다`() = runBlocking {
        val access = Counter().provider().resolve(AiArea.NEWS)
        val msg = access.errorMessage(Exception("API key not valid. Please pass a valid API key."))
        assertTrue(msg, msg.contains("체험 통로에 문제"))
    }

    @Test
    fun `본인 키의 오류 문구는 예전 그대로다`() = runBlocking {
        val e = Exception("429 RESOURCE_EXHAUSTED Please retry in 12.3s")
        val access = Counter().provider(userKey = "MINE").resolve(AiArea.NEWS)
        assertEquals(GeminiManager.aiErrorMessage(e), access.errorMessage(e))
    }

    @Test
    fun `남은 횟수 표시는 체험 중일 때만 나온다`() = runBlocking {
        val c = Counter()
        c.used[AiArea.NEWS] = 2
        assertEquals(mapOf(AiArea.NEWS to 5, AiArea.LEARNING to 7, AiArea.STOCK to 6), c.provider().trialRemaining())
        assertNull(c.provider(userKey = "MINE").trialRemaining())
        assertNull(c.provider(trialKey = null).trialRemaining())
        assertNull(c.provider(cfg = AiTrialConfig.DEFAULT.copy(enabled = false)).trialRemaining())
    }

    @Test
    fun `한도를 0으로 내린 분야는 꺼진 것으로 본다`() = runBlocking {
        val c = Counter()
        val cfg = AiTrialConfig(enabled = true, limits = mapOf(AiArea.NEWS to 0, AiArea.LEARNING to 7))
        assertEquals(AiAccess.Unavailable(AiAccess.Reason.TRIAL_DISABLED), c.provider(cfg = cfg).resolve(AiArea.NEWS))
        assertEquals(AiAccess.Unavailable(AiAccess.Reason.TRIAL_DISABLED), c.provider(cfg = cfg).resolve(AiArea.STOCK))
        assertTrue(c.provider(cfg = cfg).resolve(AiArea.LEARNING) is AiAccess.Trial)
    }
}

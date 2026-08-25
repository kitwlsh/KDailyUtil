package com.kitwlshcom.kdailyutil

import com.kitwlshcom.kdailyutil.data.remote.GeminiManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AI 오류 안내 회귀 테스트.
 *
 * 🔴 **왜 필요한가**: 2026-08-18 K장부 실사용에서 «사진 인식이 하루종일 실패»했는데, 원인은
 * 503(모델 과부하)이 404 판정에도 429 판정에도 걸리지 않아 **폴백도 재시도도 없이** 그대로
 * 실패한 것이었다. 안내는 «원인 미상»으로 떨어져 사용자도 개발자도 원인을 알 수 없었다.
 * KDailyUtil에도 같은 구멍이 v1.6.1까지 남아 있었다.
 *
 * 이런 분기는 **사고가 난 날 처음 확인하게 되는 물건**이라, 그날 안 먹히면 최악이다.
 * 그래서 문구 자체가 아니라 **«어느 문제인지 구분하는가»** 를 고정한다.
 */
class AiErrorMessageTest {

    /** 실제 Gemini SDK가 던지는 503 본문(2026-08-18 실측). */
    private val real503 = RuntimeException(
        "com.google.ai.client.generativeai.type.ServerException: This model is currently " +
            "experiencing high demand. Spikes in demand are usually temporary."
    )

    @Test
    fun `503 과부하를 원인 미상으로 흘리지 않는다`() {
        val msg = GeminiManager.aiErrorMessage(real503)
        assertTrue("붐빈다는 사실을 말해야 한다: $msg", msg.contains("붐빕니다"))
        assertFalse("원인 미상으로 떨어지면 회귀다: $msg", msg.contains("상세:"))
    }

    /**
     * 🔴 503을 «한도 소진»으로 안내하면 사용자가 «내가 많이 썼나» 하고 그냥 기다린다.
     * 남의 문제(구글 혼잡)를 내 문제로 읽히게 하면 안 된다.
     */
    @Test
    fun `503을 내 한도 소진으로 오인시키지 않는다`() {
        val msg = GeminiManager.aiErrorMessage(real503)
        assertFalse("한도 초과로 읽히면 안 된다: $msg", msg.contains("한도를 초과"))
    }

    @Test
    fun `503과 429는 다른 안내여야 한다`() {
        val busy = GeminiManager.aiErrorMessage(RuntimeException("503 UNAVAILABLE"))
        val quota = GeminiManager.aiErrorMessage(
            RuntimeException("429 RESOURCE_EXHAUSTED: exceeded your current quota")
        )
        assertNotEquals("혼잡과 한도 초과를 같은 문구로 안내하면 사용자가 엉뚱한 조치를 한다", busy, quota)
    }

    /**
     * 🔴 혼잡을 «키 문제»로 안내하면 사용자가 **멀쩡한 키를 지우고** 새로 발급받는다.
     */
    @Test
    fun `혼잡이나 한도를 키 문제로 안내하지 않는다`() {
        for (e in listOf(real503, RuntimeException("429 RESOURCE_EXHAUSTED"))) {
            val msg = GeminiManager.aiErrorMessage(e)
            assertFalse("키를 의심하게 하면 안 된다: $msg", msg.contains("키가 올바르지 않습니다"))
        }
    }

    @Test
    fun `키가 틀린 것은 키 문제라고 말한다`() {
        val msg = GeminiManager.aiErrorMessage(RuntimeException("API_KEY_INVALID: API key not valid"))
        assertTrue("키 문제임을 알려야 한다: $msg", msg.contains("API 키가 올바르지 않습니다"))
    }

    /**
     * 2026-08(v1.6.1 이전) 신규 계정 전멸 장애의 오류 본문.
     * 이건 사용자가 할 수 있는 게 «업데이트»뿐이라 다른 안내와 섞이면 안 된다.
     */
    @Test
    fun `404 모델 없음은 업데이트를 안내한다`() {
        val msg = GeminiManager.aiErrorMessage(
            RuntimeException("404 NOT_FOUND: This model is no longer available to new users")
        )
        assertTrue("업데이트를 안내해야 한다: $msg", msg.contains("업데이트"))
        assertFalse("혼잡으로 오인하면 안 된다: $msg", msg.contains("붐빕니다"))
    }

    @Test
    fun `모르는 오류도 다음 행동을 알려준다`() {
        val msg = GeminiManager.aiErrorMessage(RuntimeException("무슨 일인지 알 수 없는 오류"))
        assertTrue("다시 시도를 안내해야 한다: $msg", msg.contains("다시 시도"))
        assertTrue("조사용으로 원문은 남긴다: $msg", msg.contains("무슨 일인지 알 수 없는 오류"))
    }

    // ── 판정 함수 자체 (ask()의 폴백 분기가 이것으로 갈린다) ──────────────────

    /**
     * 🔴 **503은 폴백, 429는 폴백 안 함.** 한도는 키 단위라 모델을 바꿔도 그대로이기 때문이다.
     * 이 구분이 무너지면 한도 초과 때 후보 수만큼 같은 실패를 반복한다.
     */
    @Test
    fun `503만 폴백 대상이고 429는 아니다`() {
        assertTrue(GeminiManager.looksOverloaded("503 UNAVAILABLE: high demand"))
        assertFalse("429를 과부하로 보면 쓸데없이 모델을 갈아탄다", GeminiManager.looksOverloaded("429 RESOURCE_EXHAUSTED"))
        assertTrue(GeminiManager.looksRateLimited("429 RESOURCE_EXHAUSTED"))
        assertFalse("503을 한도로 보면 안 된다", GeminiManager.looksRateLimited("503 Service Unavailable"))
    }

    /** 404("no longer available")가 503 판정("UNAVAILABLE")에 걸려 넘어가면 안 된다. */
    @Test
    fun `404 문구가 503 판정에 걸리지 않는다`() {
        val notFound = "This model is no longer available to new users"
        assertTrue(GeminiManager.looksModelUnavailable(notFound))
        assertFalse("두 판정이 겹치면 로그가 엉뚱한 사유를 남긴다", GeminiManager.looksOverloaded(notFound))
    }

    /**
     * 🔴 **폴백 후보에 실재하지 않는 모델을 두면 한 칸이 헛돈다.**
     * `gemini-2.0-flash`는 이 계정 models 목록에 없어서 폴백이 한 칸 낭비되고 있었다(2026-08-18 실측).
     */
    @Test
    fun `폴백 후보는 별칭으로 시작하고 없는 모델을 담지 않는다`() {
        assertTrue("첫 후보는 항상 별칭이어야 한다", GeminiManager.FALLBACK_MODELS.first().endsWith("-latest"))
        assertFalse(
            "이 계정에 존재하지 않는 모델이다",
            GeminiManager.FALLBACK_MODELS.contains("gemini-2.0-flash")
        )
        assertFalse(
            "신규 계정에 닫힌 모델이다(2026-08 장애)",
            GeminiManager.FALLBACK_MODELS.contains("gemini-2.5-flash")
        )
    }
}

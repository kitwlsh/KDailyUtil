package com.kitwlshcom.kdailyutil

import com.kitwlshcom.kdailyutil.data.remote.GeminiManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * **폴백 루프 실동작 회귀 테스트.**
 *
 * 🔴 **왜 필요한가**: 2026-08-18 K장부 장애의 본질은 «503이 나면 다음 모델로 넘어가는가»였다.
 * 그런데 그 판단은 실제 API가 503을 뱉어줘야만 확인되고, 503은 **마음대로 만들 수 없다**
 * (2026-08-25 실측에서도 21회 중 4회만 나왔다). 그래서 이 루프는 **사고가 난 날에야
 * 처음 확인하게 되는 코드**였다 — 그날 안 먹히면 최악이다.
 *
 * [GeminiManager.askWithFallback]에 가짜 호출을 넣어 **기기·네트워크·키 없이** 그 판단을 고정한다.
 *
 * ⚠️ [GeminiManager.lastGoodModel]은 앱 전역(companion)이라 테스트끼리 샌다 → [reset]에서 비운다.
 */
class GeminiFallbackTest {

    /** 실제 SDK가 뱉는 문구 그대로 쓴다. 문자열이 조금만 달라도 판정이 갈리기 때문이다. */
    private val busy503 = RuntimeException(
        "ServerException: This model is currently experiencing high demand. (503 UNAVAILABLE)"
    )
    private val gone404 = RuntimeException(
        "NOT_FOUND: This model models/gemini-2.5-flash is no longer available to new users"
    )
    private val quota429 = RuntimeException("429 RESOURCE_EXHAUSTED: exceeded your current quota")
    private val badKey = RuntimeException("API_KEY_INVALID: API key not valid")

    /** 시도된 모델 이름을 순서대로 기록한다. «몇 번째에서 멈췄나»가 이 테스트의 핵심이다. */
    private lateinit var tried: MutableList<String>

    @Before
    fun reset() {
        tried = mutableListOf()
        GeminiManager.lastGoodModel = null
        GeminiManager.preferredModel = null
    }

    private fun manager() = GeminiManager("test-key-not-used")

    /** [failures]에 이름이 있으면 그 예외를 던지고, 없으면 성공 응답을 돌려준다. */
    private fun caller(failures: Map<String, Exception>): suspend (String) -> String = { name ->
        tried += name
        failures[name]?.let { throw it }
        "OK from $name"
    }

    // ── 넘어가야 하는 경우 ────────────────────────────────────────────────

    /** 🔴 이 테스트가 곧 2026-08-18 장애 그 자체다. 깨지면 그 장애가 돌아온 것이다. */
    @Test
    fun `503이면 다음 후보로 넘어간다`() = runBlocking {
        val first = GeminiManager.FALLBACK_MODELS[0]
        val second = GeminiManager.FALLBACK_MODELS[1]

        val out = manager().askWithFallback(caller(mapOf(first to busy503)))

        assertEquals("두 번째 후보의 응답이 나와야 한다", "OK from $second", out)
        assertEquals("첫 후보를 시도한 뒤 두 번째로 넘어가야 한다", listOf(first, second), tried)
    }

    @Test
    fun `404면 다음 후보로 넘어간다`() = runBlocking {
        val first = GeminiManager.FALLBACK_MODELS[0]
        val second = GeminiManager.FALLBACK_MODELS[1]

        val out = manager().askWithFallback(caller(mapOf(first to gone404)))

        assertEquals("OK from $second", out)
        assertEquals(listOf(first, second), tried)
    }

    /** 앞의 둘이 동시에 막혀도 마지막 칸까지 간다 — 후보가 헛돌지 않는지 확인한다. */
    @Test
    fun `앞 후보가 연달아 막히면 끝까지 내려간다`() = runBlocking {
        val models = GeminiManager.FALLBACK_MODELS
        val out = manager().askWithFallback(
            caller(mapOf(models[0] to busy503, models[1] to gone404))
        )

        assertEquals("OK from ${models[2]}", out)
        assertEquals(models, tried)
    }

    // ── 넘어가면 안 되는 경우 ─────────────────────────────────────────────

    /**
     * 🔴 **429는 폴백하지 않는다.** 한도는 **키 단위**라 모델을 바꿔도 그대로다.
     * 폴백하면 같은 실패를 후보 수만큼 반복하고(사용자는 그만큼 더 기다린다),
     * 한도를 더 태우며, 사용자에게 보여줄 사유까지 흐려진다.
     */
    @Test
    fun `429는 폴백하지 않고 즉시 던진다`() = runBlocking {
        val first = GeminiManager.FALLBACK_MODELS[0]
        var thrown: Exception? = null

        try {
            manager().askWithFallback(caller(mapOf(first to quota429)))
        } catch (e: Exception) {
            thrown = e
        }

        assertNotNull("한도 초과는 그대로 던져야 한다", thrown)
        assertEquals("후보를 더 시도하면 한도만 더 태운다", listOf(first), tried)
    }

    @Test
    fun `키 오류는 폴백하지 않고 즉시 던진다`() = runBlocking {
        val first = GeminiManager.FALLBACK_MODELS[0]
        var thrown: Exception? = null

        try {
            manager().askWithFallback(caller(mapOf(first to badKey)))
        } catch (e: Exception) {
            thrown = e
        }

        assertNotNull(thrown)
        assertTrue("사유가 흐려지면 안 된다", GeminiManager.aiErrorMessage(thrown!!).contains("API 키"))
        assertEquals(listOf(first), tried)
    }

    @Test
    fun `모든 후보가 막히면 마지막 사유를 던진다`() = runBlocking {
        val allBusy = GeminiManager.FALLBACK_MODELS.associateWith { busy503 }
        var thrown: Exception? = null

        try {
            manager().askWithFallback(caller(allBusy))
        } catch (e: Exception) {
            thrown = e
        }

        assertNotNull(thrown)
        assertTrue(
            "«원인 미상»이 아니라 붐빈다고 말해야 한다",
            GeminiManager.aiErrorMessage(thrown!!).contains("붐빕니다")
        )
        assertEquals("후보를 전부 시도해야 한다", GeminiManager.FALLBACK_MODELS, tried)
    }

    // ── 막힌 모델을 다시 찌르지 않는가 ────────────────────────────────────

    /**
     * 🔴 **`lastGoodModel`이 없으면 «너무 오래 걸린다»가 된다.**
     * 2026-08-18 실측에서 503 응답이 오기까지 3~70초가 걸렸다. 기본 모델이 막힌 동안
     * 호출마다 그것을 먼저 찌르면 **매 요청이 그만큼씩 늦어진다.**
     * KDailyUtil은 화면마다 `GeminiManager`를 새로 만들기 때문에 인스턴스 기억으론 부족하다.
     */
    @Test
    fun `한 번 통한 모델은 다음 인스턴스가 먼저 쓴다`() = runBlocking {
        val first = GeminiManager.FALLBACK_MODELS[0]
        val second = GeminiManager.FALLBACK_MODELS[1]

        // ① 첫 화면: 기본 모델이 503 → 두 번째로 넘어가 성공
        manager().askWithFallback(caller(mapOf(first to busy503)))
        assertEquals("성공한 모델을 앱 전역에 기억해야 한다", second, GeminiManager.lastGoodModel)

        // ② 다른 화면(새 인스턴스): 막힌 기본 모델을 다시 찌르지 않아야 한다
        tried.clear()
        val out = manager().askWithFallback(caller(mapOf(first to busy503)))

        assertEquals("OK from $second", out)
        assertEquals("막힌 모델을 또 찌르면 매 요청이 503 대기만큼 느려진다", listOf(second), tried)
    }

    /**
     * 원격 레버(`family.json`의 `aiModel`)가 **가장 세다** — 사고 대응용이기 때문이다.
     *
     * ⚠️ 한 번도 실전에서 당겨본 적 없는 레버다(2026-08-25 기준 `family.json` 4곳 모두 키 없음).
     * 그리고 이 키는 **K장부도 같이 읽는다** — `doc/family_config/README.md` §3-1.
     */
    @Test
    fun `원격 지정 모델을 가장 먼저 시도한다`() = runBlocking {
        GeminiManager.preferredModel = "gemini-remote-override"

        val out = manager().askWithFallback(caller(emptyMap()))

        assertEquals("OK from gemini-remote-override", out)
        assertEquals("레버가 기본 모델보다 앞서야 한다", listOf("gemini-remote-override"), tried)
    }

    /** 레버로 지정한 모델이 없는 이름이어도 앱이 죽으면 안 된다 — 폴백으로 살아남아야 한다. */
    @Test
    fun `원격 지정 모델이 없는 이름이면 기본 후보로 되돌아온다`() = runBlocking {
        GeminiManager.preferredModel = "gemini-bogus"
        val first = GeminiManager.FALLBACK_MODELS[0]

        val out = manager().askWithFallback(caller(mapOf("gemini-bogus" to gone404)))

        assertEquals("OK from $first", out)
        assertEquals(listOf("gemini-bogus", first), tried)
    }
}

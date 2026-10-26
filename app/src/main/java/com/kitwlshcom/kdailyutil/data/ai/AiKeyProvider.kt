package com.kitwlshcom.kdailyutil.data.ai

import com.kitwlshcom.kdailyutil.BuildConfig
import com.kitwlshcom.kdailyutil.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first

/**
 * AI 기능이 어느 분야에서 불렸는가. 체험판은 횟수를 **분야별로 나눠** 센다
 * (doc/NEXT_SESSION.md §할 일 4 — K장부처럼 총량 하나로 세면 13가지 기능을 한 번씩만 눌러도 절반이 사라진다).
 */
enum class AiArea { NEWS, LEARNING, STOCK }

/**
 * 체험판 설정. 원격(`family.json`의 `aiTrial`)으로 끄거나 횟수를 바꿀 수 있다.
 *
 * @param limits 분야별 횟수. 뉴스 7 · 배움터 7 · 증시 6 = 20(K장부와 같은 총량을 셋으로 나눴다).
 */
data class AiTrialConfig(
    val enabled: Boolean,
    val limits: Map<AiArea, Int>,
) {
    fun limitOf(area: AiArea): Int = limits[area] ?: 0

    companion object {
        /** 원격이 비었거나 못 읽었을 때. **켜짐**이다 — 원격이 죽어도 체험이 갑자기 끊기지 않게. */
        val DEFAULT = AiTrialConfig(
            enabled = true,
            limits = mapOf(AiArea.NEWS to 7, AiArea.LEARNING to 7, AiArea.STOCK to 6),
        )
    }
}

/**
 * AI 키 사용 권한. 호출부는 **종류만 보고 분기**하면 된다.
 */
sealed interface AiAccess {
    /** 사용자 본인 키 — 개발자 비용이 0원이라 **무제한**. */
    data class UserKey(val key: String) : AiAccess

    /** 개발자 체험 키 — 작업이 성공하면 [AiKeyProvider.recordUse]로 1회 차감한다. */
    data class Trial(val key: String, val area: AiArea, val remaining: Int, val limit: Int) : AiAccess

    /** 쓸 수 없음. [reason]으로 안내 문구를 고른다. */
    data class Unavailable(val reason: Reason) : AiAccess

    enum class Reason {
        /** 본인 키도 없고 체험도 없는 빌드다 → 발급 안내 */
        NO_KEY,
        /** 이 분야의 체험 횟수를 다 썼다 → 본인 키 유도 */
        TRIAL_EXHAUSTED,
        /** 원격 스위치로 체험이 꺼졌다 → 본인 키 유도 */
        TRIAL_DISABLED,
    }

    /** 실제 호출에 쓸 키. 못 쓰면 null. */
    val keyOrNull: String?
        get() = when (this) {
            is UserKey -> key
            is Trial -> key
            is Unavailable -> null
        }
}

/**
 * 🔑 Gemini 키 해석의 **단일 진입점**(2026-10-26 · AI 체험판).
 *
 * ### 왜 한 곳으로 모으나
 * 예전엔 `settingsRepository.geminiApiKeyFlow.first()`가 **14곳**(뉴스 2 · 배움터 8 · 증시 4)에 흩어져 있었다.
 * 여기에 체험 카운터를 각각 붙이면 **한 곳만 빠뜨려도 세지 않는 무료 호출**이 생긴다
 * (K장부 `doc/AI_KEY_POLICY.md` §8-3이 같은 이유로 먼저 모았다).
 *
 * 🔴 **순서를 지킬 것** — 모으기 → 카운터 → 안내 → 원격 끄기 스위치 → **개발자 키 넣기는 맨 마지막**.
 * 키부터 넣으면 카운터 없이 전 기능이 무제한으로 열린다.
 *
 * ### 우선순위
 * 1. **본인 키가 있으면 무조건 그것** — 개발자 비용이 0원이라 제한할 이유가 없다
 * 2. 없으면 체험 키(빌드에 들어 있을 때만) — 원격 스위치가 켜져 있고 그 분야에 횟수가 남았을 때만
 * 3. 둘 다 아니면 [AiAccess.Unavailable]
 *
 * ### 세는 규칙
 * **«호출»이 아니라 «사용자가 한 작업»** 단위로, **성공했을 때만** 센다 — 실패(네트워크·429·빈 응답)까지
 * 깎으면 결과도 못 받고 횟수만 잃는다. 호출부는 결과를 화면에 낸 뒤 [recordUse]를 한 번 부른다.
 *
 * ⚠️ «연결 테스트»(`BriefingViewModel.validateApiKey`)는 여기를 거치지 않는다 — 시험하는 대상이 **본인 키**다.
 *
 * 모든 의존을 함수로 받는 이유 = 테스트가 DataStore·원격 없이 바꿔 끼울 수 있게.
 */
class AiKeyProvider(
    private val userKey: suspend () -> String?,
    private val trialKey: String? = null,
    private val trialConfig: suspend () -> AiTrialConfig = { AiTrialConfig.DEFAULT },
    private val trialUsed: suspend (AiArea) -> Int = { 0 },
    private val addTrialUse: suspend (AiArea) -> Unit = {},
) {

    constructor(settings: SettingsRepository) : this(
        userKey = { settings.geminiApiKeyFlow.first() },
        trialKey = BuildConfig.GEMINI_TRIAL_KEY.takeIf { it.isNotBlank() },
        trialUsed = { settings.aiTrialUsedFlow(it).first() },
        addTrialUse = { settings.incrementAiTrialUsed(it) },
    )

    /** 지금 [area]에서 AI를 쓸 수 있는가. **차감하지 않는다.** */
    suspend fun resolve(area: AiArea): AiAccess {
        val key = userKey()
        if (!key.isNullOrBlank()) return AiAccess.UserKey(key)

        val trial = trialKey ?: return AiAccess.Unavailable(AiAccess.Reason.NO_KEY)

        val cfg = trialConfig()
        val limit = cfg.limitOf(area)
        if (!cfg.enabled || limit <= 0) return AiAccess.Unavailable(AiAccess.Reason.TRIAL_DISABLED)

        val remaining = (limit - trialUsed(area)).coerceAtLeast(0)
        return if (remaining > 0) AiAccess.Trial(trial, area, remaining, limit)
        else AiAccess.Unavailable(AiAccess.Reason.TRIAL_EXHAUSTED)
    }

    /**
     * 체험 1회 차감. **작업이 성공한 뒤에만** 부른다.
     * [access]가 체험이 아니면(본인 키 등) 아무 일도 하지 않으므로 호출부가 분기할 필요 없다.
     */
    suspend fun recordUse(access: AiAccess) {
        if (access is AiAccess.Trial) addTrialUse(access.area)
    }

    /**
     * 화면 표시용 — 분야별 남은 체험 횟수. **체험을 보여 줄 상황이 아니면 null**이다
     * (본인 키가 있다 · 체험 키가 없는 빌드다 · 원격으로 꺼졌다). null이면 화면은 예전 그대로 그린다.
     */
    suspend fun trialRemaining(): Map<AiArea, Int>? {
        if (!userKey().isNullOrBlank() || trialKey == null) return null
        val cfg = trialConfig()
        if (!cfg.enabled) return null
        return AiArea.values().associateWith { (cfg.limitOf(it) - trialUsed(it)).coerceAtLeast(0) }
    }
}

/**
 * 키가 없을 때의 안내 — **«왜 못 쓰는지»를 구분한다**(2026-10-26 · 체험판 3단계).
 *
 * 체험을 다 쓴 사람에게 «키를 등록해 주세요»만 말하면 «방금까지 됐는데 왜?»가 된다.
 * 체험이 없는 빌드(지금)에서는 [noKey]를 그대로 돌려준다 — **화면마다 쓰던 문구가 하나도 안 바뀐다.**
 *
 * @param noKey 그 화면이 원래 쓰던 «키를 등록해 주세요» 문구
 */
fun AiAccess.unavailableMessage(noKey: String): String =
    when ((this as? AiAccess.Unavailable)?.reason) {
        AiAccess.Reason.TRIAL_EXHAUSTED ->
            "이 분야의 AI 체험을 모두 썼어요. Google AI Studio에서 무료 키를 발급받아 설정 > AI·키 에 넣으면 " +
                "계속 쓸 수 있어요(발급 약 3분 · 요금 없음)."
        AiAccess.Reason.TRIAL_DISABLED ->
            "지금은 AI 체험을 쓸 수 없어요. 무료 키를 발급받아 설정 > AI·키 에 넣으면 바로 쓸 수 있어요."
        else -> noKey
    }

/**
 * AI 호출 실패 안내 — 체험 키로 불렀을 때만 두 경우를 다르게 말한다(2026-10-26).
 *
 * - **429(한도):** 기본 문구는 «이 키의 사용 한도»라고 말하는데 체험 사용자에게는 «이 키»가 없다.
 *   체험은 **여러 사람이 한 통로를 나눠 쓰므로** 적게 써도 걸릴 수 있다 — 그 사실과 할 일(본인 키)을 덧붙인다.
 *   🔴 **«체험 소진»으로 말하면 안 된다** — 횟수가 남은 사람에게 거짓말이 된다(K장부 §8-4).
 * - **키 오류·권한:** 기본 문구는 «붙여넣은 키를 확인하라»인데 체험 사용자는 고칠 키가 없다.
 *   체험 키가 막힌 것이니 본인 키를 안내한다.
 *
 * 본인 키(또는 키 없음)이면 예전 [GeminiManager.aiErrorMessage] 그대로다.
 */
fun AiAccess.errorMessage(e: Throwable): String {
    val base = com.kitwlshcom.kdailyutil.data.remote.GeminiManager.aiErrorMessage(e)
    if (this !is AiAccess.Trial) return base
    val m = (e.message ?: "") + (e.cause?.message ?: "")
    return when {
        m.contains("API_KEY_INVALID", true) || m.contains("API key not valid", true) ||
            m.contains("PERMISSION", true) ->
            "지금은 AI 체험 통로에 문제가 있어요. 무료 키를 발급받아 설정 > AI·키 에 넣으면 바로 쓸 수 있어요."
        com.kitwlshcom.kdailyutil.data.remote.GeminiManager.looksRateLimited(m) ->
            "$base\n체험은 여러 사람이 한 통로를 함께 써서 많이 쓰지 않았어도 걸릴 수 있어요. " +
                "본인 키를 넣으면 이런 대기가 거의 없어집니다. (체험 횟수는 줄지 않았어요)"
        else -> base
    }
}

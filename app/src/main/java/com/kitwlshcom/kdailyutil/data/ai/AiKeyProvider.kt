package com.kitwlshcom.kdailyutil.data.ai

import com.kitwlshcom.kdailyutil.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first

/**
 * AI 기능이 어느 분야에서 불렸는가. 체험판은 횟수를 **분야별로 나눠** 센다
 * (doc/NEXT_SESSION.md §할 일 4 — K장부처럼 총량 하나로 세면 13가지 기능을 한 번씩만 눌러도 절반이 사라진다).
 */
enum class AiArea { NEWS, LEARNING, STOCK }

/**
 * AI 키 사용 권한. 호출부는 **종류만 보고 분기**하면 된다.
 */
sealed interface AiAccess {
    /** 사용자 본인 키 — 개발자 비용이 0원이라 **무제한**. */
    data class UserKey(val key: String) : AiAccess

    /** 쓸 수 없음. */
    data class Unavailable(val reason: Reason) : AiAccess

    enum class Reason {
        /** 본인 키가 없다 → 발급 안내 */
        NO_KEY,
    }

    /** 실제 호출에 쓸 키. 못 쓰면 null. */
    val keyOrNull: String?
        get() = when (this) {
            is UserKey -> key
            is Unavailable -> null
        }
}

/**
 * 🔑 Gemini 키 해석의 **단일 진입점**(2026-10-26 · AI 체험판 1단계).
 *
 * ### 왜 한 곳으로 모으나
 * 예전엔 `settingsRepository.geminiApiKeyFlow.first()`가 **14곳**(뉴스 2 · 배움터 8 · 증시 4)에 흩어져 있었다.
 * 여기에 체험 카운터를 각각 붙이면 **한 곳만 빠뜨려도 세지 않는 무료 호출**이 생긴다
 * (K장부 `doc/AI_KEY_POLICY.md` §8-3이 같은 이유로 먼저 모았다).
 *
 * 🔴 **순서를 지킬 것** — 모으기(이 클래스) → 카운터 → 안내 → 원격 끄기 스위치 → **개발자 키 넣기는 맨 마지막**.
 * 키부터 넣으면 카운터 없이 전 기능이 무제한으로 열린다.
 *
 * ⚠️ «연결 테스트»(`BriefingViewModel.validateApiKey`)는 여기를 거치지 않는다 — 시험하는 대상이 **본인 키**다.
 *
 * @param userKey 본인 키를 읽는 곳. 테스트가 DataStore 없이 바꿔 끼울 수 있게 함수로 받는다.
 */
class AiKeyProvider(private val userKey: suspend () -> String?) {

    constructor(settings: SettingsRepository) : this({ settings.geminiApiKeyFlow.first() })

    /** 지금 [area]에서 AI를 쓸 수 있는가. **차감하지 않는다.** */
    @Suppress("UNUSED_PARAMETER")
    suspend fun resolve(area: AiArea): AiAccess {
        val key = userKey()
        if (!key.isNullOrBlank()) return AiAccess.UserKey(key)
        return AiAccess.Unavailable(AiAccess.Reason.NO_KEY)
    }
}

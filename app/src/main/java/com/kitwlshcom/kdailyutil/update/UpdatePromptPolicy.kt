package com.kitwlshcom.kdailyutil.update

/**
 * 「새 버전이 있어요」를 **언제 물어도 되는가** (2026-10-08).
 *
 * 🔴 **왜 정책이 따로 필요한가** — 인앱 업데이트에서 제일 쉽게 망치는 것이 **조르기**다.
 * 앱을 열 때마다 「업데이트하세요」가 뜨면 사용자는 그 창을 **읽지 않고 닫는 버릇**이 들고,
 * 그러면 정작 중요한 판을 올렸을 때도 똑같이 닫는다. **안내가 소음이 되는 순간 안내가 아니다.**
 *
 * 그래서 규칙은 둘뿐이다:
 * 1. **같은 버전은 [QUIET_DAYS]일에 한 번만 묻는다** — 거절한 사람에게 내일 또 묻지 않는다.
 * 2. **새 버전이 나오면 그 날짜를 잊는다** — 사용자가 1.7.3을 거절한 것이지 1.8.0을 거절한 것이 아니다.
 *
 * ⚠️ **«업데이트를 받았으니 다시 시작하자»는 이 정책을 타지 않는다.** 그것은 사용자가 이미
 * «받겠다»고 누른 뒤의 일이라 조르기가 아니다 — 받아 놓고 안 알리면 그게 더 이상하다.
 *
 * 안드로이드 API를 쓰지 않는 순수 계산이다(단위 테스트 대상). 자매앱에 그대로 옮길 수 있다.
 */
object UpdatePromptPolicy {

    /** 같은 버전을 다시 묻기까지 쉬는 기간. */
    const val QUIET_DAYS = 3L

    private const val DAY_MS = 24L * 60 * 60 * 1000

    /** 아직 아무것도 물어본 적이 없음을 뜻하는 값. */
    const val NEVER_ASKED = -1

    /**
     * 지금 물어도 되는가.
     *
     * @param availableVersionCode 스토어에 올라와 있는 새 버전
     * @param lastAskedVersionCode 마지막으로 물어본 버전([NEVER_ASKED]이면 처음)
     * @param lastAskedAtMs 그때의 시각
     * @param nowMs 지금
     */
    fun shouldPrompt(
        availableVersionCode: Int,
        lastAskedVersionCode: Int,
        lastAskedAtMs: Long,
        nowMs: Long
    ): Boolean {
        // 처음이거나, **다른(더 새로운) 버전**이 올라왔다 → 묻는다.
        if (lastAskedVersionCode != availableVersionCode) return true
        // 같은 버전이다 → 쉬는 기간이 지났을 때만.
        val elapsed = nowMs - lastAskedAtMs
        // 🔴 시각이 거꾸로 간 경우(사용자가 폰 날짜를 바꿨다)도 «지났다»로 본다.
        //    음수를 그냥 두면 «영원히 안 물어보는» 상태에 갇힌다.
        if (elapsed < 0) return true
        return elapsed >= QUIET_DAYS * DAY_MS
    }
}

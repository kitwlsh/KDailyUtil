package com.kitwlshcom.kdailyutil

import com.kitwlshcom.kdailyutil.update.UpdatePromptPolicy
import com.kitwlshcom.kdailyutil.update.UpdatePromptPolicy.NEVER_ASKED
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 「새 버전이 있어요」를 **언제 물어도 되는가** (2026-10-08).
 *
 * 🔴 **무엇을 지키는 테스트인가** — «조르지 않는 것»이다.
 * 앱을 열 때마다 업데이트 창이 뜨면 사용자는 그 창을 **읽지 않고 닫는 버릇**이 들고,
 * 그러면 정작 중요한 판을 올렸을 때도 똑같이 닫는다. 안내가 소음이 되면 안내가 아니다.
 *
 * ⚠️ 그렇다고 **새 버전이 나왔는데 입을 다물어도 안 된다** — 두 실패가 반대 방향이라
 * 경계를 양쪽에서 다 박아 둔다.
 */
class UpdatePromptPolicyTest {

    private val day = 24L * 60 * 60 * 1000
    private val now = 1_800_000_000_000L   // 임의의 «지금»

    @Test
    fun `처음이면 묻는다`() {
        assertTrue(
            UpdatePromptPolicy.shouldPrompt(
                availableVersionCode = 12,
                lastAskedVersionCode = NEVER_ASKED,
                lastAskedAtMs = 0L,
                nowMs = now
            )
        )
    }

    @Test
    fun `같은 버전을 방금 물어봤으면 다시 묻지 않는다`() {
        assertFalse(
            UpdatePromptPolicy.shouldPrompt(
                availableVersionCode = 12,
                lastAskedVersionCode = 12,
                lastAskedAtMs = now - day,   // 어제 물어봤다
                nowMs = now
            )
        )
    }

    @Test
    fun `쉬는 기간이 지나면 같은 버전도 다시 묻는다`() {
        // 🔴 영원히 입을 다물면 «업데이트가 있는 줄도 모르는» 사용자가 생긴다.
        assertTrue(
            UpdatePromptPolicy.shouldPrompt(
                availableVersionCode = 12,
                lastAskedVersionCode = 12,
                lastAskedAtMs = now - UpdatePromptPolicy.QUIET_DAYS * day,
                nowMs = now
            )
        )
    }

    @Test
    fun `경계 — 쉬는 기간 하루 전은 아직 묻지 않는다`() {
        assertFalse(
            UpdatePromptPolicy.shouldPrompt(
                availableVersionCode = 12,
                lastAskedVersionCode = 12,
                lastAskedAtMs = now - (UpdatePromptPolicy.QUIET_DAYS - 1) * day,
                nowMs = now
            )
        )
    }

    @Test
    fun `새 버전이 나오면 쉬는 기간을 무시하고 묻는다`() {
        // 🔴 사용자가 거절한 것은 **그 버전**이지 다음 버전이 아니다.
        //    긴급 수정을 올렸는데 «3일 전에 물어봤다»고 입을 다물면 그게 사고다.
        assertTrue(
            UpdatePromptPolicy.shouldPrompt(
                availableVersionCode = 13,       // 더 새 버전이 올라왔다
                lastAskedVersionCode = 12,
                lastAskedAtMs = now,             // 방금 물어봤는데도
                nowMs = now
            )
        )
    }

    @Test
    fun `폰 날짜를 과거로 돌려도 영영 입을 다물지 않는다`() {
        // 사용자가 기기 날짜를 바꾸면 «마지막으로 물어본 시각»이 미래가 된다.
        // 그걸 음수 경과로 두면 **다시는 묻지 않는 상태**에 갇힌다.
        assertTrue(
            UpdatePromptPolicy.shouldPrompt(
                availableVersionCode = 12,
                lastAskedVersionCode = 12,
                lastAskedAtMs = now + 30 * day,  // 미래에 물어본 것으로 기록돼 있다
                nowMs = now
            )
        )
    }

    @Test
    fun `쉬는 기간은 사흘이다`() {
        // 값이 바뀌면 이 테스트가 먼저 알려 준다 — 숫자는 판단이고, 판단은 눈에 보여야 한다.
        org.junit.Assert.assertEquals(3L, UpdatePromptPolicy.QUIET_DAYS)
    }
}

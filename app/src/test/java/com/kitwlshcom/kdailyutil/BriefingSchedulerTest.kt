package com.kitwlshcom.kdailyutil

import com.kitwlshcom.kdailyutil.scheduler.BriefingScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * 「매일 아침 알림」이 **매일** 오는지를 지키는 테스트.
 *
 * 2026-09-04 이전에는 알람이 한 번 울린 뒤 아무도 다음 날을 다시 걸지 않아서
 * 사실상 1일차에만 알림이 왔다. 재예약 자체는 BroadcastReceiver 안에서 일어나 기기가 필요하지만,
 * **다음 발동 시각 계산**은 순수 함수로 빼 두었으므로 여기서 고정할 수 있다.
 */
class BriefingSchedulerTest {

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int = 0): Long =
        Calendar.getInstance().apply {
            set(year, month - 1, day, hour, minute, second)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun fieldsOf(millis: Long): Triple<Int, Int, Int> {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return Triple(c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
    }

    @Test
    fun `아직 안 지난 시각이면 오늘 울린다`() {
        val now = at(2026, 9, 4, 5, 0)
        val next = BriefingScheduler.nextTriggerAt(hour = 7, minute = 0, nowMillis = now)
        assertEquals(Triple(4, 7, 0), fieldsOf(next))
    }

    @Test
    fun `이미 지난 시각이면 내일로 넘어간다`() {
        val now = at(2026, 9, 4, 9, 30)
        val next = BriefingScheduler.nextTriggerAt(hour = 7, minute = 0, nowMillis = now)
        assertEquals(Triple(5, 7, 0), fieldsOf(next))
    }

    @Test
    fun `알람이 울린 직후 다시 걸면 반드시 내일이다`() {
        // 이게 「매일 온다」의 핵심이다.
        // 브리핑 알람은 울린 그 순간 다음 알람을 다시 거는데, 그때 오늘로 잡히면
        // 같은 아침에 알림이 반복해서 터진다.
        val firedAt = at(2026, 9, 4, 7, 0, 1) // 7시 정각 알람이 1초 뒤에 처리되는 상황
        val next = BriefingScheduler.nextTriggerAt(hour = 7, minute = 0, nowMillis = firedAt)
        assertEquals(Triple(5, 7, 0), fieldsOf(next))
        assertTrue("다음 알람은 반드시 미래여야 한다", next > firedAt)
    }

    @Test
    fun `설정 시각이 지금과 너무 붙어 있으면 내일로 미룬다`() {
        // 5초 안쪽은 오발동으로 본다(설정 화면에서 지금과 거의 같은 시각을 고르는 경우).
        // 기존 동작을 그대로 유지한다 — 2026-09-04 재예약을 붙이면서 이 가드가 더 중요해졌다.
        val imminent = at(2026, 9, 4, 6, 59, 58) // 2초 뒤 = 가드 안쪽
        assertEquals(
            Triple(5, 7, 0),
            fieldsOf(BriefingScheduler.nextTriggerAt(hour = 7, minute = 0, nowMillis = imminent))
        )

        val comfortable = at(2026, 9, 4, 6, 59, 50) // 10초 뒤 = 가드 밖이므로 오늘 울린다
        assertEquals(
            Triple(4, 7, 0),
            fieldsOf(BriefingScheduler.nextTriggerAt(hour = 7, minute = 0, nowMillis = comfortable))
        )
    }

    @Test
    fun `자정 직전 설정도 날짜가 밀리지 않는다`() {
        val now = at(2026, 9, 4, 23, 50)
        val next = BriefingScheduler.nextTriggerAt(hour = 23, minute = 55, nowMillis = now)
        assertEquals(Triple(4, 23, 55), fieldsOf(next))
    }

    @Test
    fun `월말에도 다음 날로 정상 이월된다`() {
        val now = at(2026, 9, 30, 8, 0)
        val next = BriefingScheduler.nextTriggerAt(hour = 7, minute = 0, nowMillis = now)
        val c = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(1, c.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.OCTOBER, c.get(Calendar.MONTH))
    }

    @Test
    fun `다음 알람은 언제나 지금보다 뒤다`() {
        // 하루 24시간 어느 시각에 걸어도 과거로 잡히면 안 된다(과거 알람은 즉시 터진다)
        for (h in 0..23) {
            for (m in listOf(0, 30, 59)) {
                val now = at(2026, 9, 4, h, m, 30)
                for (targetH in 0..23) {
                    val next = BriefingScheduler.nextTriggerAt(targetH, 0, now)
                    assertTrue("$h:$m 에 ${targetH}시 알람이 과거로 잡혔다", next > now)
                }
            }
        }
    }
}

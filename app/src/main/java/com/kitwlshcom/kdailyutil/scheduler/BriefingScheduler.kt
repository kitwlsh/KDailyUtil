package com.kitwlshcom.kdailyutil.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.kitwlshcom.kdailyutil.receiver.BriefingReceiver
import java.util.Calendar

/**
 * 모닝 브리핑 알람.
 *
 * ⚠️ 이 알람은 **단발**이다(안드로이드의 반복 알람은 정확한 시각을 보장하지 않아 쓰지 않는다).
 * 그래서 **울린 쪽에서 다음 날을 다시 걸어 주어야 한다** — 그 재예약은
 * [com.kitwlshcom.kdailyutil.receiver.BriefingReceiver]가 한다.
 * 2026-09-04 이전에는 그 재예약이 없어서 「매일 아침 알림」이 한 번 울리고 죽었다.
 */
class BriefingScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        /** 설정 시각이 지금과 너무 붙어 있으면(초 단위) 오늘 것으로 보지 않는다 — 즉시 오발동 방지. */
        private const val IMMINENT_GUARD_SECONDS = 5

        /**
         * 다음 발동 시각(밀리초). 오늘 그 시각이 아직 안 지났으면 오늘, 지났으면 내일.
         *
         * 안드로이드 API를 쓰지 않는 **순수 계산**이라 기기 없이 테스트할 수 있다
         * (BriefingSchedulerTest). 「매일 온다」의 절반이 이 계산이라 고정해 둔다.
         */
        @JvmStatic
        fun nextTriggerAt(hour: Int, minute: Int, nowMillis: Long = System.currentTimeMillis()): Long {
            val target = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val threshold = nowMillis + IMMINENT_GUARD_SECONDS * 1000L
            if (target.timeInMillis < threshold) {
                target.add(Calendar.DATE, 1)
            }
            return target.timeInMillis
        }
    }

    fun scheduleBriefing(hour: Int, minute: Int) {
        val intent = Intent(context, BriefingReceiver::class.java).apply {
            action = "com.kitwlshcom.kdailyutil.ACTION_MORNING_BRIEFING"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAt = nextTriggerAt(hour, minute)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            } else {
                // 권한이 없는 경우 일반 알람으로 대체하거나 설정 화면 유도 필요
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        }
    }

    fun cancelBriefing() {
        val intent = Intent(context, BriefingReceiver::class.java).apply {
            action = "com.kitwlshcom.kdailyutil.ACTION_MORNING_BRIEFING"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}

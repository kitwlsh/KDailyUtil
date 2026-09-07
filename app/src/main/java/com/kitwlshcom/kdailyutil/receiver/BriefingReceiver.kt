package com.kitwlshcom.kdailyutil.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.kitwlshcom.kdailyutil.MainActivity
import com.kitwlshcom.kdailyutil.data.DailyRecord
import com.kitwlshcom.kdailyutil.data.repository.QuizRepository
import com.kitwlshcom.kdailyutil.data.repository.ReadingTrainingRepository
import com.kitwlshcom.kdailyutil.data.repository.SettingsRepository
import com.kitwlshcom.kdailyutil.scheduler.BriefingScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BriefingReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BriefingReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {

            // ──────────────────────────────────────────────────────────
            // 알람 시간이 되어 발동: 자동 브리핑이 켜져 있는지 확인 후 알림
            // ──────────────────────────────────────────────────────────
            "com.kitwlshcom.kdailyutil.ACTION_MORNING_BRIEFING" -> {
                Log.d(TAG, "📅 모닝 브리핑 알람 수신")

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val settingsRepo = SettingsRepository(context)
                        val isEnabled = settingsRepo.isBriefingEnabledFlow.first()

                        if (isEnabled) {
                            Log.d(TAG, "✅ 자동 브리핑 활성화 상태 — 알림 표시")
                            showNotification(context, buildBriefingMessage(context))
                        } else {
                            Log.d(TAG, "⏸️ 자동 브리핑 비활성화 상태 — 알림 생략")
                        }

                        // 🔴 여기가 없으면 「매일 아침 알림」이 **딱 한 번만** 온다.
                        //
                        // setExactAndAllowWhileIdle은 단발 알람이다. 2026-09-04 이전에는 알람이
                        // 울린 뒤 아무도 다음 알람을 걸지 않아서, 사용자가 설정을 다시 만지거나
                        // 폰을 재부팅하기 전까지 알림이 영영 오지 않았다.
                        // (개발자는 설정을 자주 만져 계속 되살아나므로 자기 기기로는 안 보이는 실패다)
                        //
                        // 알림을 못 띄웠더라도 재예약은 한다 — 오늘 실패했다고 내일까지 포기할 이유는 없다.
                        // 켜져 있을 때만 다시 건다(꺼져 있으면 알람 자체가 필요 없다).
                        if (isEnabled) {
                            try {
                                val (hour, minute) = settingsRepo.briefingTimeFlow.first()
                                BriefingScheduler(context).scheduleBriefing(hour, minute)
                                Log.d(TAG, "🔁 다음 브리핑 재예약: ${hour}:${String.format("%02d", minute)}")
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ 다음 브리핑 재예약 실패: ${e.message}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "설정 읽기 실패: ${e.message}")
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            // ──────────────────────────────────────────────────────────
            // 기기 재부팅: 자동 브리핑이 켜져 있으면 알람 재등록만 수행
            // (재부팅 시 알림을 즉시 띄우지 않음 — 다음 예약 시간에 자동 발동)
            // ──────────────────────────────────────────────────────────
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "📱 기기 재부팅 감지 — 자동 브리핑 알람 재등록 여부 확인")

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val settingsRepo = SettingsRepository(context)
                        val isEnabled = settingsRepo.isBriefingEnabledFlow.first()

                        if (isEnabled) {
                            val (hour, minute) = settingsRepo.briefingTimeFlow.first()
                            Log.d(TAG, "✅ 자동 브리핑 재등록: ${hour}:${String.format("%02d", minute)}")
                            BriefingScheduler(context).scheduleBriefing(hour, minute)
                        } else {
                            Log.d(TAG, "⏸️ 자동 브리핑 비활성화 — 알람 재등록 생략")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "설정 읽기 실패 (BOOT): ${e.message}")
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            else -> {
                Log.w(TAG, "알 수 없는 액션: ${intent.action}")
            }
        }
    }

    /**
     * 오늘 무엇이 새로운지 한 줄로 만든다.
     *
     * 고정 문구를 사흘 보면 사람은 알림을 끈다. **알림을 끄면 다시 데려올 수단이 영구히 사라진다**
     * — 가장 비싼 실패라 매일 달라지게 한다.
     *
     * ⚠️ 네트워크는 쓰지 않는다. 이 코드는 BroadcastReceiver 안에서 도는 데다(시간 제한이 있다),
     * 알림 한 줄 때문에 통신이 걸려 알림 자체를 놓치는 쪽이 훨씬 나쁘다.
     * 그래서 **이미 기기에 있는 것**(내려받아 둔 문제 수, 출석 기록)만 본다.
     */
    private suspend fun buildBriefingMessage(context: Context): String {
        return try {
            val today = java.time.LocalDate.now()
            val settingsRepo = SettingsRepository(context)
            val status = settingsRepo.dailyStatusFlow.first()

            // 🔴 «늘어난 수»를 그대로 말하면 오래 비운 사용자에게 「새 문제 300개」가 나간다.
            // 상한·복귀 사면·문구 조립 규칙은 전부 DailyRecord에 있다(테스트로 고정) —
            // 여기서는 기기에 있는 값을 모아 넘기기만 한다.
            val quizNotice = DailyRecord.newQuizNotice(
                today = today,
                lastDone = status.lastDone,
                total = QuizRepository().countAll(context),
                seenCount = status.seenQuizCount
            )

            val readingRepo = ReadingTrainingRepository(context)
            val passages = readingRepo.loadRemotePassages()
            val passageNotice = DailyRecord.newPassageNotice(
                today = today,
                lastTrained = parseCompactDate(readingRepo.lastTrainedDateFlow.first()),
                createdDates = passages.mapNotNull { it.createdAt },
                total = passages.size,
                seenCount = readingRepo.seenPassageCountFlow.first()
            )

            DailyRecord.briefingMessage(passageNotice, quizNotice, status.displayStreak(today))
        } catch (e: Exception) {
            // 문구를 못 만들었다고 알림을 포기하지 않는다 — 기본 문구로라도 나간다.
            Log.w(TAG, "알림 문구 구성 실패(기본 문구로 대체): ${e.message}")
            DailyRecord.BRIEFING_FALLBACK
        }
    }

    /** 훈련 기록이 쓰는 yyyyMMdd 형식. 깨져 있으면 null(그러면 사면 판정을 하지 않는다). */
    private fun parseCompactDate(raw: String?): java.time.LocalDate? = try {
        if (raw.isNullOrBlank()) null
        else java.time.LocalDate.parse(raw, java.time.format.DateTimeFormatter.BASIC_ISO_DATE)
    } catch (e: Exception) {
        null
    }

    private fun showNotification(context: Context, contentText: String) {
        val channelId = "briefing_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "모닝 브리핑",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "설정된 시간에 오늘의 뉴스를 알려줍니다."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("START_AUTO_BRIEFING", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("모닝 브리핑 준비 완료! 📢")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            notificationManager.notify(2001, notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "알림 권한이 없습니다: ${e.message}")
        }
    }
}

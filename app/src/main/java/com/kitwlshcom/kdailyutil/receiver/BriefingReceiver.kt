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
                            showNotification(context)
                        } else {
                            Log.d(TAG, "⏸️ 자동 브리핑 비활성화 상태 — 알림 생략")
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

    private fun showNotification(context: Context) {
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
            .setContentText("터치하여 오늘의 뉴스와 AI 맞춤 분석을 들어보세요.")
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

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

class BriefingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.kitwlshcom.kdailyutil.ACTION_MORNING_BRIEFING" || 
            intent.action == Intent.ACTION_BOOT_COMPLETED) {
            
            Log.d("BriefingReceiver", "시간이 되었습니다! 뉴스 브리핑을 준비합니다.")
            showNotification(context)
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

        // 앱의 기본 아이콘(mipmap/ic_launcher)을 사용하거나 시스템 아이콘 사용
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
            Log.e("BriefingReceiver", "알림 권한이 없습니다: ${e.message}")
        }
    }
}

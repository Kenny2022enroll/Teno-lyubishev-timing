package com.example.lyubishchevtiming.service

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = getSystemService(NotificationManager::class.java) ?: return

        // 前台计时服务通知
        mgr.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Time Tracking Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
        )
        // 休息提醒通知（高优先级，弹出横幅）
        mgr.createNotificationChannel(
            NotificationChannel(
                BREAK_CHANNEL_ID,
                "Break Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }

    companion object {
        const val CHANNEL_ID = "timeTrackingServiceChannel"
        const val BREAK_CHANNEL_ID = "breakReminderChannel"
    }
}

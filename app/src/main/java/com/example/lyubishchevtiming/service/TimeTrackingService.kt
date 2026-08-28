package com.example.lyubishchevtiming.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.IntentCompat
import com.example.lyubishchevtiming.R
import com.example.lyubishchevtiming.TimeTrackingActivity
import com.example.lyubishchevtiming.model.Task
import com.example.lyubishchevtiming.service.App.Companion.BREAK_CHANNEL_ID
import com.example.lyubishchevtiming.service.App.Companion.CHANNEL_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 时间追踪前台服务。
 *
 * 使用逻辑基于现代与经典科学理论：
 *  - 番茄工作法（Cirillo, 1980s）：每 25 分钟专注后提醒 5 分钟短休息，以对抗认知疲劳、
 *    维持持续性注意（sustained attention）。
 *  - 超日节律 / 基本休息-活动周期（Kleitman, 1955）：人体以约 90 分钟为一个生理工作单元，
 *    超过该阈值后警觉性下降；因此在累计 90 分钟时提醒一次较长的恢复性休息（约 20 分钟）。
 *
 * 服务在"整个计时会话"期间持续运行（不再随 Activity 暂停/恢复而启停），
 * 使休息提醒在前后台两种状态下都能触发。
 */
class TimeTrackingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var watchJob: Job? = null

    /** 服务启动时继承的已计时偏移量（来自 Activity 的 timeWhenStopped）。 */
    private var elapsedOffset = 0L
    /** 服务自身的启动时刻（elapsedRealtime 域）。 */
    private var startRealtime = 0L
    private var running = true

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val task = intent?.let { IntentCompat.getParcelableExtra(it, "taskExtra", Task::class.java) }
        val desiredTime = intent?.getLongExtra("desiredTime", 0) ?: 0
        val timeWhenStopped = intent?.getLongExtra("time", 0) ?: 0
        val isRunning = intent?.getBooleanExtra("running", true) ?: true

        elapsedOffset = timeWhenStopped
        startRealtime = SystemClock.elapsedRealtime()
        running = isRunning

        // 清理上一会话可能残留的休息提醒
        cancelBreakReminders()
        watchJob?.cancel()
        startForeground(NOTIF_ID, buildNotification(task, elapsedOffset(), isRunning))

        if (isRunning && task != null) {
            startBreakWatcher(task)
        }
        return START_NOT_STICKY
    }

    private fun startBreakWatcher(task: Task) {
        watchJob = scope.launch {
            // 计算下一个尚未触发的休息里程碑（重启后可由当前累计时长推得，无需持久化状态）
            var nextPomodoro = nextMilestone(elapsedOffset, POMODORO_WORK_MS)
            var nextUltradian = nextMilestone(elapsedOffset, ULTRADIAN_MS)
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                val elapsed = elapsedOffset + (SystemClock.elapsedRealtime() - startRealtime)
                // 刷新前台通知中的实时计时
                updateForeground(task, elapsed)
                if (elapsed >= nextPomodoro) {
                    postBreakReminder(
                        BREAK_POMODORO_ID,
                        getString(R.string.pomodoro_break_title),
                        getString(R.string.pomodoro_break_text, POMODORO_BREAK_MIN)
                    )
                    nextPomodoro += POMODORO_WORK_MS
                }
                if (elapsed >= nextUltradian) {
                    postBreakReminder(
                        BREAK_ULTRADIAN_ID,
                        getString(R.string.ultradian_break_title),
                        getString(R.string.ultradian_break_text, ULTRADIAN_BREAK_MIN)
                    )
                    nextUltradian += ULTRADIAN_MS
                }
            }
        }
    }

    private fun buildNotification(task: Task?, elapsedMs: Long, isRunning: Boolean): Notification {
        val taskName = task?.name?.takeIf { it.isNotEmpty() } ?: getString(R.string.app_name)
        val contentIntent = Intent(this, TimeTrackingActivity::class.java).apply {
            putExtra("task", task)
            putExtra("todayDesiredTime", 0L)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, contentIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val text = if (isRunning) getString(R.string.foreground_running, formatElapsed(elapsedMs))
        else getString(R.string.foreground_paused)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(taskName)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_lyubishchev_r)
            .setOngoing(isRunning)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateForeground(task: Task, elapsedMs: Long) {
        getSystemService(NotificationManager::class.java)
            ?.notify(NOTIF_ID, buildNotification(task, elapsedMs, true))
    }

    private fun postBreakReminder(id: Int, title: String, text: String) {
        val notif = NotificationCompat.Builder(this, BREAK_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_lyubishchev_r)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        getSystemService(NotificationManager::class.java)?.notify(id, notif)
    }

    private fun cancelBreakReminders() {
        val mgr = getSystemService(NotificationManager::class.java) ?: return
        mgr.cancel(BREAK_POMODORO_ID)
        mgr.cancel(BREAK_ULTRADIAN_ID)
    }

    private fun formatElapsed(ms: Long): String {
        val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date(ms.coerceAtLeast(0)))
    }

    private fun nextMilestone(elapsed: Long, period: Long): Long {
        if (elapsed < 0) return period
        val k = elapsed / period
        return (k + 1) * period
    }

    private fun elapsedOffset(): Long =
        elapsedOffset + (SystemClock.elapsedRealtime() - startRealtime)

    override fun onDestroy() {
        watchJob?.cancel()
        cancelBreakReminders()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        // 番茄工作法（Cirillo, 1980s）：25 分钟专注 + 5 分钟短休息
        private const val POMODORO_WORK_MS = 25L * 60 * 1000
        private const val POMODORO_BREAK_MIN = 5
        // 超日节律 / 基本休息-活动周期（Kleitman, 1955）：约 90 分钟一个生理工作单元
        private const val ULTRADIAN_MS = 90L * 60 * 1000
        private const val ULTRADIAN_BREAK_MIN = 20

        private const val POLL_INTERVAL_MS = 60_000L

        private const val NOTIF_ID = 1
        private const val BREAK_POMODORO_ID = 2
        private const val BREAK_ULTRADIAN_ID = 3
    }
}

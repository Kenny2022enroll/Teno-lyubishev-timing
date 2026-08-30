package com.example.lyubishchevtiming

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import com.example.lyubishchevtiming.database.AppDatabase
import com.example.lyubishchevtiming.databinding.ActivityTimeTrackingBinding
import com.example.lyubishchevtiming.model.Log as TimeLog
import com.example.lyubishchevtiming.model.Task
import com.example.lyubishchevtiming.service.TimeTrackingService
import com.example.lyubishchevtiming.widget.SummaryWidgetService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class TimeTrackingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTimeTrackingBinding

    private var isRunning = false
    private var task: Task? = null
    private var timeAmount = 0L
    private var desiredTimeAmount = 0L
    private var timeWhenStopped = 0L

    private val db by lazy { AppDatabase.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimeTrackingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 解析传入的任务
        task = IntentCompat.getParcelableExtra(intent, "task", Task::class.java)
        desiredTimeAmount = intent.getLongExtra("todayDesiredTime", 0)

        when {
            savedInstanceState != null -> {
                isRunning = savedInstanceState.getBoolean(CHRONOMETER_RUNNING_KEY)
                setCurrentTime(savedInstanceState.getLong(CHRONOMETER_TIME_KEY))
                if (isRunning) {
                    binding.chronometer.start()
                    ensureServiceRunning()
                }
            }
            intent.hasExtra("time") && intent.hasExtra("running") -> {
                timeWhenStopped = intent.getLongExtra("time", 0)
                isRunning = intent.getBooleanExtra("running", false)
                setCurrentTime(timeWhenStopped)
                if (isRunning) {
                    binding.chronometer.start()
                    ensureServiceRunning()
                }
            }
            else -> startChronometer()
        }

        binding.taskNameSummary.text = task?.name.orEmpty()

        binding.stopBtn.setOnClickListener {
            if (isRunning) stopChronometer()
        }
        binding.cancelBtn.setOnClickListener { finish() }
    }

    private fun setCurrentTime(time: Long) {
        timeWhenStopped = time
        binding.chronometer.base = SystemClock.elapsedRealtime() - timeWhenStopped
    }

    private fun startChronometer() {
        binding.chronometer.base = SystemClock.elapsedRealtime() - timeWhenStopped
        binding.chronometer.start()
        isRunning = true
        // 标记当前正在追踪的任务（供 TaskActivity 的“注意力残留”切换警告使用）
        task?.id?.let { getPrefs().edit().putInt(KEY_RUNNING_TASK_ID, it).apply() }
        ensureServiceRunning()
    }

    private fun stopChronometer() {
        binding.chronometer.stop()
        timeAmount = SystemClock.elapsedRealtime() - binding.chronometer.base
        isRunning = false
        // 停止前台服务（同时取消休息提醒）并清除“当前运行任务”标记
        stopTimeTrackingService()
        getPrefs().edit().remove(KEY_RUNNING_TASK_ID).apply()

        val currentTask = task
        if (currentTask != null) {
            val log = TimeLog(
                todayDate = Date(),
                todayTimeAmount = timeAmount,
                desiredTimeAmount = desiredTimeAmount,
                taskId = currentTask.id
            )
            Log.d(TAG, "saveLogToDb: ${log.id} / ${log.todayDate} / ${log.todayTimeAmount} / ${log.desiredTimeAmount} / ${log.taskId}")
            lifecycleScope.launch {
                withContext(Dispatchers.IO) { db.logDao().insertLog(log) }
                SummaryWidgetService.startActionUpdateSummaryWidgets(this@TimeTrackingActivity)
            }
        }

        val intent = Intent(this, TimeTrackingSummaryActivity::class.java).apply {
            putExtra("time_amount", timeAmount)
            putExtra("task", currentTask)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    /**
     * 启动 / 刷新前台计时服务。服务在整个计时会话期间持续运行，
     * 从而在前后台两种状态下都能触发番茄钟与超日节律休息提醒。
     */
    private fun ensureServiceRunning() {
        val currentTask = task ?: return
        val serviceIntent = Intent(this, TimeTrackingService::class.java).apply {
            putExtra("taskExtra", currentTask)
            putExtra("desiredTime", desiredTimeAmount)
            putExtra("time", if (isRunning)
                SystemClock.elapsedRealtime() - binding.chronometer.base else timeWhenStopped)
            putExtra("running", isRunning)
        }
        // 使用 ContextCompat 兼容 API 25（Android 7.1）：startForegroundService 自 API 26 起
        // 才存在，直接调用会在 Android 7.1 上抛 NoSuchMethodError 导致点击 Start 闪退。
        // ContextCompat 在 API < 26 时回退到 startService()，服务内仍调用 startForeground()。
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun stopTimeTrackingService() {
        stopService(Intent(this, TimeTrackingService::class.java))
    }

    override fun onBackPressed() {
        // 计时进行中不允许直接返回，避免误触丢失会话
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (isRunning) {
            timeWhenStopped = SystemClock.elapsedRealtime() - binding.chronometer.base
        }
        outState.putLong(CHRONOMETER_TIME_KEY, timeWhenStopped)
        outState.putBoolean(CHRONOMETER_RUNNING_KEY, isRunning)
    }

    private fun getPrefs() = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

    companion object {
        private const val TAG = "TimeTrackingActivity"
        private const val CHRONOMETER_TIME_KEY = "time"
        private const val CHRONOMETER_RUNNING_KEY = "running"
        const val PREFS_NAME = "lyubishchev_timing_prefs"
        const val KEY_RUNNING_TASK_ID = "running_task_id"
    }
}

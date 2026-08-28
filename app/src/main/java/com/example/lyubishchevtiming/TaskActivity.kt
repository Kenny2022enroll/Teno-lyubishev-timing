package com.example.lyubishchevtiming

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.lyubishchevtiming.database.AppDatabase
import com.example.lyubishchevtiming.databinding.ActivityTaskBinding
import com.example.lyubishchevtiming.model.Task
import com.example.lyubishchevtiming.model.Week
import com.example.lyubishchevtiming.viewmodel.LogByDateForTaskViewModel
import com.example.lyubishchevtiming.viewmodel.LogByDateForTaskViewModelFactory
import com.example.lyubishchevtiming.viewmodel.TaskViewModel
import com.example.lyubishchevtiming.viewmodel.TaskViewModelFactory
import com.example.lyubishchevtiming.viewmodel.TaskWeekViewModel
import com.example.lyubishchevtiming.viewmodel.TaskWeekViewModelFactory
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.TimeZone

class TaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskBinding

    private var task: Task? = null
    private var week: Week? = null
    private var timeFromLogs = 0L
    private var todayDesiredAmountOfTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentTask = IntentCompat.getParcelableExtra(intent, "task", Task::class.java)
        this.task = currentTask
        if (currentTask != null) {
            loadWeekFromDatabase(currentTask.weekId)
            loadTaskFromDatabase(currentTask.id)
            getLogsForToday(currentTask.id)
        }

        binding.done.text = getString(R.string.done, getString(R.string.default_done))

        binding.fabEditAddTask.setOnClickListener {
            startActivity(Intent(this, AddEditTaskActivity::class.java).apply {
                putExtra("task", task)
            })
        }

        binding.startButton.setOnClickListener { maybeStartTracking() }
    }

    /**
     * 启动计时前检查：若另一项任务正在追踪，依据"注意力残留理论"
     *（Leroy, 2009）警告用户：频繁切换任务会让上一任务的注意残留在新任务上，损害表现。
     */
    private fun maybeStartTracking() {
        val currentTask = task ?: return
        val runningTaskId = getPrefs().getInt(TimeTrackingActivity.KEY_RUNNING_TASK_ID, -1)
        if (runningTaskId != -1 && runningTaskId != currentTask.id) {
            AlertDialog.Builder(this)
                .setTitle(R.string.attention_residue_title)
                .setMessage(getString(R.string.attention_residue_text, currentTask.name))
                .setPositiveButton(R.string.continue_anyway) { _, _ -> launchTimeTracking(currentTask) }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        } else {
            launchTimeTracking(currentTask)
        }
    }

    private fun launchTimeTracking(currentTask: Task) {
        startActivity(Intent(this, TimeTrackingActivity::class.java).apply {
            putExtra("task", currentTask)
            putExtra("todayDesiredTime", todayDesiredAmountOfTime)
        })
    }

    private fun populateUI() {
        val currentTask = task ?: return
        binding.taskNameSummary.text = currentTask.name
        binding.goal.text =
            getString(R.string.goal, convertTimeAmountToStringWithoutUTF(currentTask.duration))
        setImageViewColor()
        setWeekDayIcons()
        calculateLeftTime()
    }

    private fun convertTimeAmountToString(timeAmount: Long): String = formatHms(timeAmount, utc = true)
    private fun convertTimeAmountToStringWithoutUTF(timeAmount: Long): String = formatHms(timeAmount, utc = false)

    private fun formatHms(timeAmount: Long, utc: Boolean): String {
        val formatter: DateFormat = SimpleDateFormat("HH:mm:ss")
        if (utc) formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Date(timeAmount))
    }

    private fun setWeekDayIcons() {
        val currentWeek = week ?: return
        todayDesiredAmountOfTime = 0
        val day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        applyWeekday(binding.monIcon, currentWeek.mon, day, Calendar.MONDAY,
            R.mipmap.mon_disabled, R.mipmap.mon_today, R.mipmap.mon_enabled)
        applyWeekday(binding.tueIcon, currentWeek.tue, day, Calendar.TUESDAY,
            R.mipmap.tue_disabled, R.mipmap.tue_today, R.mipmap.tue_enabled)
        applyWeekday(binding.wedIcon, currentWeek.wed, day, Calendar.WEDNESDAY,
            R.mipmap.wed_disabled, R.mipmap.wed_today, R.mipmap.wed_enabled)
        applyWeekday(binding.thuIcon, currentWeek.thu, day, Calendar.THURSDAY,
            R.mipmap.thu_disabled, R.mipmap.thu_today, R.mipmap.thu_enabled)
        applyWeekday(binding.friIcon, currentWeek.fri, day, Calendar.FRIDAY,
            R.mipmap.fri_disabled, R.mipmap.fri_today, R.mipmap.fri_enabled)
        applyWeekday(binding.satIcon, currentWeek.sat, day, Calendar.SATURDAY,
            R.mipmap.sat_disabled, R.mipmap.sat_today, R.mipmap.sat_enabled)
        applyWeekday(binding.sunIcon, currentWeek.sun, day, Calendar.SUNDAY,
            R.mipmap.sun_disabled, R.mipmap.sun_today, R.mipmap.sun_enabled)
    }

    private fun applyWeekday(
        icon: ImageView, planned: Long, today: Int, target: Int,
        disabledRes: Int, todayRes: Int, enabledRes: Int
    ) {
        val res = when {
            planned == 0L -> disabledRes
            today == target -> {
                todayDesiredAmountOfTime = planned
                todayRes
            }
            else -> enabledRes
        }
        icon.setImageResource(res)
    }

    private fun loadTaskFromDatabase(id: Int) {
        val factory = TaskViewModelFactory(AppDatabase.getInstance(this), id)
        val viewModel = ViewModelProvider(this, factory)[TaskViewModel::class.java]
        viewModel.task.observe(this, Observer { loaded ->
            if (loaded != null) { task = loaded; populateUI() }
        })
    }

    private fun loadWeekFromDatabase(id: String) {
        val factory = TaskWeekViewModelFactory(AppDatabase.getInstance(this), id)
        val viewModel = ViewModelProvider(this, factory)[TaskWeekViewModel::class.java]
        viewModel.week.observe(this, Observer { loaded ->
            if (loaded != null) { week = loaded; populateUI() }
        })
    }

    private fun getLogsForToday(taskId: Int) {
        val calendar = GregorianCalendar().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendar.time
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val end = calendar.time

        val factory = LogByDateForTaskViewModelFactory(
            AppDatabase.getInstance(this), taskId, start, end
        )
        val viewModel = ViewModelProvider(this, factory)[LogByDateForTaskViewModel::class.java]
        viewModel.logsForTask.observe(this, Observer { time ->
            if (time != null) {
                timeFromLogs = time
                binding.done.text = getString(R.string.done, convertTimeAmountToString(timeFromLogs))
                calculateLeftTime()
            }
        })
    }

    private fun calculateLeftTime() {
        val currentTask = task ?: return
        if (todayDesiredAmountOfTime == 0L) {
            binding.left.text = getString(R.string.left, getString(R.string.default_left))
            return
        }
        // 剩余时间以"今日计划"为基准
        val goal = convertTimeAmountToStringWithoutUTF(todayDesiredAmountOfTime)
        val done = convertTimeAmountToString(timeFromLogs)
        val leftSec = toSeconds(goal) - toSeconds(done)
        binding.left.text = getString(R.string.left, formatSecondsToHms(leftSec))
    }

    private fun toSeconds(time: String): Int {
        if (time.length < 8) return 0
        return time.substring(0, 2).toInt() * 3600 +
               time.substring(3, 5).toInt() * 60 +
               time.substring(6, 8).toInt()
    }

    private fun formatSecondsToHms(seconds: Int): String {
        val clamped = seconds.coerceAtLeast(0)
        val h = clamped / 3600
        val m = clamped % 3600 / 60
        val s = clamped % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }

    private fun setImageViewColor() {
        val currentTask = task ?: return
        val colorRes = when (currentTask.color) {
            "red" -> R.color.red
            "glaucous" -> R.color.glaucous
            "yellow" -> R.color.yellow
            "green" -> R.color.green
            "orange" -> R.color.orange
            "peach" -> R.color.peach
            "lavender" -> R.color.lavender
            "blue" -> R.color.blue
            else -> R.color.colorPrimaryLight
        }
        binding.viewA.setBackgroundColor(resources.getColor(colorRes, theme))
    }

    private fun getPrefs() =
        getSharedPreferences(TimeTrackingActivity.PREFS_NAME, MODE_PRIVATE)
}

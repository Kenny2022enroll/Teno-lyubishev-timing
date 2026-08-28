package com.example.lyubishchevtiming

import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import android.widget.NumberPicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.lyubishchevtiming.database.AppDatabase
import com.example.lyubishchevtiming.databinding.ActivityAddEditTaskBinding
import com.example.lyubishchevtiming.model.Task
import com.example.lyubishchevtiming.model.Week
import com.example.lyubishchevtiming.viewmodel.TaskWeekViewModel
import com.example.lyubishchevtiming.viewmodel.TaskWeekViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random
import java.util.TimeZone

class AddEditTaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditTaskBinding

    private var task: Task? = null
    private var week: Week = Week()
    private var isEdit = false

    private val db by lazy { AppDatabase.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val incoming = intent
        if (incoming.hasExtra("task")) {
            task = IntentCompat.getParcelableExtra(incoming, "task", Task::class.java)
            isEdit = true
            task?.let { loadWeekFromDatabase(it.weekId) }
            fillFieldsFromIntent()
        } else {
            task = Task()
            week = Week()
            isEdit = false
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        binding.hoursNumberPicker.minValue = 0
        binding.hoursNumberPicker.maxValue = 23
        binding.hoursNumberPicker.setOnValueChangedListener(onNumberChangeListener)
        binding.minutesNumberPicker.minValue = 0
        binding.minutesNumberPicker.maxValue = 59
        binding.minutesNumberPicker.setOnValueChangedListener(onNumberChangeListener)

        binding.saveFab.setOnClickListener {
            val nameOk = !TextUtils.isEmpty(binding.taskNameEditText.text)
            val daysOk = anyDayChecked()
            val timeOk = binding.hoursNumberPicker.value != 0 || binding.minutesNumberPicker.value != 0
            if (nameOk && daysOk && timeOk) {
                if (isEdit) {
                    getUpdatedDataFromFields()
                    updateTaskInDatabase()
                } else {
                    getDataFromFields()
                    addTaskToDatabase()
                }
                finish()
            } else {
                if (!nameOk) binding.taskNameEditText.error = getString(R.string.err_enter_task_name)
                if (!daysOk) toast(R.string.err_select_days)
                if (!timeOk) toast(R.string.err_select_time)
            }
        }

        savedInstanceState?.let {
            binding.hoursNumberPicker.value = it.getInt(HOURS_KEY)
            binding.minutesNumberPicker.value = it.getInt(MINUTES_KEY)
        }
    }

    private fun anyDayChecked(): Boolean =
        binding.monCheckBox.isChecked || binding.tueCheckBox.isChecked ||
                binding.wedCheckBox.isChecked || binding.thuCheckBox.isChecked ||
                binding.friCheckBox.isChecked || binding.satCheckBox.isChecked ||
                binding.sunCheckBox.isChecked

    private fun fillFieldsFromIntent() {
        val currentTask = task ?: return
        loadWeekFromDatabase(currentTask.weekId)
        // 等待 week 异步加载后回填
        binding.taskNameEditText.setText(currentTask.name)
    }

    /** 由 loadWeekFromDatabase 的 observer 调用以回填表单。 */
    private fun fillFieldsFromWeek(loaded: Week) {
        week = loaded
        binding.monCheckBox.isChecked = loaded.mon != 0L
        binding.tueCheckBox.isChecked = loaded.tue != 0L
        binding.wedCheckBox.isChecked = loaded.wed != 0L
        binding.thuCheckBox.isChecked = loaded.thu != 0L
        binding.friCheckBox.isChecked = loaded.fri != 0L
        binding.satCheckBox.isChecked = loaded.sat != 0L
        binding.sunCheckBox.isChecked = loaded.sun != 0L
        val currentTask = task ?: return
        val durationMs = currentTask.duration
        val date = Date(durationMs)
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        val s = formatter.format(date)
        binding.hoursNumberPicker.value = s.substring(0, 2).toInt()
        binding.minutesNumberPicker.value = s.substring(3, 5).toInt()
    }

    private fun durationFromPickers(): Long {
        val hours = binding.hoursNumberPicker.value
        val minutes = binding.minutesNumberPicker.value
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return try {
            formatter.parse("$hours:$minutes:00")?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun applyWeekFields(week: Week, ms: Long) {
        week.mon = if (binding.monCheckBox.isChecked) ms else 0L
        week.tue = if (binding.tueCheckBox.isChecked) ms else 0L
        week.wed = if (binding.wedCheckBox.isChecked) ms else 0L
        week.thu = if (binding.thuCheckBox.isChecked) ms else 0L
        week.fri = if (binding.friCheckBox.isChecked) ms else 0L
        week.sat = if (binding.satCheckBox.isChecked) ms else 0L
        week.sun = if (binding.sunCheckBox.isChecked) ms else 0L
    }

    private fun getDataFromFields() {
        val currentTask = task ?: Task()
        currentTask.name = binding.taskNameEditText.text.toString()
        currentTask.color = "green"
        val ms = durationFromPickers()
        currentTask.duration = ms
        week = Week()
        applyWeekFields(week, ms)
        val weekId = (currentTask.id + Random().nextInt(100)).toString()
        week.id = weekId
        currentTask.weekId = weekId
        task = currentTask
    }

    private fun getUpdatedDataFromFields() {
        val currentTask = task ?: return
        currentTask.name = binding.taskNameEditText.text.toString()
        currentTask.color = "blue"
        val ms = durationFromPickers()
        currentTask.duration = ms
        applyWeekFields(week, ms)
    }

    private fun loadWeekFromDatabase(id: String) {
        val factory = TaskWeekViewModelFactory(db, id)
        val viewModel = ViewModelProvider(this, factory)[TaskWeekViewModel::class.java]
        // 仅在首次加载到数据后填充表单，随后移除 Observer，避免后续 LiveData 变更重复覆盖用户输入。
        val observer = object : Observer<Week?> {
            override fun onChanged(loaded: Week?) {
                viewModel.week.removeObserver(this)
                if (loaded != null) fillFieldsFromWeek(loaded)
            }
        }
        viewModel.week.observe(this, observer)
    }

    private fun addTaskToDatabase() {
        val currentTask = task ?: return
        val currentWeek = week
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                db.weekDao().insertWeekDayCombination(currentWeek)
                db.taskDao().insertTask(currentTask)
            }
        }
    }

    private fun updateTaskInDatabase() {
        val currentTask = task ?: return
        val currentWeek = week
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                db.weekDao().updateWeek(currentWeek)
                db.taskDao().updateTask(currentTask)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(HOURS_KEY, binding.hoursNumberPicker.value)
        outState.putInt(MINUTES_KEY, binding.minutesNumberPicker.value)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }

    private fun toast(resId: Int) = Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()

    private val onNumberChangeListener =
        NumberPicker.OnValueChangeListener { picker, _, _ ->
            Toast.makeText(this@AddEditTaskActivity,
                getString(R.string.toast_selected_number, picker.value), Toast.LENGTH_SHORT).show()
        }

    companion object {
        private const val HOURS_KEY = "hourskey"
        private const val MINUTES_KEY = "minuteskey"
    }
}

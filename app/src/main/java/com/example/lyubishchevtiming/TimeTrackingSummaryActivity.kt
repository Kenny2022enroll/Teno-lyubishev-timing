package com.example.lyubishchevtiming

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import com.example.lyubishchevtiming.databinding.ActivityTimeTrackingSummaryBinding
import com.example.lyubishchevtiming.model.Task
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

class TimeTrackingSummaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTimeTrackingSummaryBinding
    private var timeAmount = 0L
    private var task: Task? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimeTrackingSummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        timeAmount = intent.getLongExtra("time_amount", 0L)
        task = IntentCompat.getParcelableExtra(intent, "task", Task::class.java)

        binding.taskLabel.text = task?.name.orEmpty()
        Log.d(TAG, "onCreate: $timeAmount")
        binding.timeAmountSummary.text = convertTimeAmountToString(timeAmount)

        binding.continueButton.setOnClickListener {
            val intent = Intent(this, TaskActivity::class.java)
            intent.putExtra("task", task)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
    }

    private fun convertTimeAmountToString(timeAmount: Long): String {
        val formatter = SimpleDateFormat("HH:mm:ss")
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Date(timeAmount))
    }

    override fun onBackPressed() {
        // 计时结束后回到任务页，禁止直接返回计时页
    }

    companion object {
        private const val TAG = "TimeTracking"
    }
}

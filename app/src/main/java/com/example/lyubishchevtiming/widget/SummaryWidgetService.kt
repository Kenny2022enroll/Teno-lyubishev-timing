package com.example.lyubishchevtiming.widget

import android.app.IntentService
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.lyubishchevtiming.R
import com.example.lyubishchevtiming.database.AppDatabase
import com.example.lyubishchevtiming.model.Summary
import kotlinx.coroutines.runBlocking
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.TimeZone

class SummaryWidgetService : IntentService("SummaryWidgetService") {

    private lateinit var db: AppDatabase

    override fun onHandleIntent(intent: Intent?) {
        if (intent != null && ACTION_UPDATE_SUMMARY_WIDGETS == intent.action) {
            handleActionUpdateSummaryWidgets()
        }
    }

    private fun handleActionUpdateSummaryWidgets() {
        Log.d(TAG, "run: actual and desired start")
        db = AppDatabase.getInstance(applicationContext)

        val calendar = GregorianCalendar().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendar.time
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val end = calendar.time

        // IntentService 已在后台线程，使用 runBlocking 调用 suspend DAO
        val summaries: List<Summary> = runBlocking {
            db.logDao().getLogsForService(start, end)
        }

        var actualTotal = 0L
        var totalDesiredHours = 0
        var totalDesiredMin = 0
        summaries.forEach { s ->
            actualTotal += s.actualTimeAmount
            // desired 时长按毫秒存储且带时区偏移，需格式化回 "HH:mm:ss" 再解析
            val desiredTime = if (s.desiredTimeAmount != 0L)
                convertTimeAmountToStringWithoutUTF(s.desiredTimeAmount) else "00:00:00"
            totalDesiredHours += desiredTime.substring(0, 2).toInt()
            totalDesiredMin += desiredTime.substring(3, 5).toInt()
        }

        val actualTime = convertTimeAmountToString(actualTotal)
        val actualHours = actualTime.substring(0, 2).toInt()
        val actualMin = actualTime.substring(3, 5).toInt()

        val actual: String
        val desired: String
        if (summaries.isNotEmpty()) {
            actual = getString(R.string.actual_time, actualHours, actualMin)
            desired = getString(R.string.desired_time, totalDesiredHours, totalDesiredMin)
            Log.d(TAG, "run: actual and desired $actual$desired")
        } else {
            actual = "Start activity now!"
            desired = ""
        }

        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(applicationContext, SummaryWidgetProvider::class.java)
        )
        SummaryWidgetProvider.updateSummaryWidgets(
            applicationContext, appWidgetManager, actual, desired, appWidgetIds
        )
    }

    private fun convertTimeAmountToStringWithoutUTF(timeAmount: Long): String {
        val formatter: DateFormat = SimpleDateFormat("HH:mm:ss")
        return formatter.format(Date(timeAmount))
    }

    private fun convertTimeAmountToString(timeAmount: Long): String {
        val formatter = SimpleDateFormat("HH:mm:ss")
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Date(timeAmount))
    }

    companion object {
        const val ACTION_UPDATE_SUMMARY_WIDGETS =
            "com.example.lyubishchevtiming.action.update_summary_widgets"
        private const val TAG = "SummaryWidgetService"

        fun startActionUpdateSummaryWidgets(context: Context) {
            val intent = Intent(context, SummaryWidgetService::class.java).apply {
                action = ACTION_UPDATE_SUMMARY_WIDGETS
            }
            context.startService(intent)
        }
    }
}

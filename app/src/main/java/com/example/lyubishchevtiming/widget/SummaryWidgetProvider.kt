package com.example.lyubishchevtiming.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.lyubishchevtiming.MainActivity
import com.example.lyubishchevtiming.R

/**
 * 桌面小部件：展示当天实际/计划时长。点击进入主界面。
 */
class SummaryWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        SummaryWidgetService.startActionUpdateSummaryWidgets(context)
    }

    companion object {
        fun updateSummaryWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            actual: String,
            desired: String,
            appWidgetIds: IntArray
        ) {
            appWidgetIds.forEach { updateAppWidget(context, appWidgetManager, actual, desired, it) }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            actual: String,
            desired: String,
            appWidgetId: Int
        ) {
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra("tab", 1)
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val views = RemoteViews(context.packageName, R.layout.summary_widget_provider).apply {
                setTextViewText(R.id.actual_time, actual)
                setTextViewText(R.id.desired_time, desired)
                setOnClickPendingIntent(R.id.actual_time, pendingIntent)
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

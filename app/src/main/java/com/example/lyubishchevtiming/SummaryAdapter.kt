package com.example.lyubishchevtiming

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lyubishchevtiming.databinding.ListItemBinding
import com.example.lyubishchevtiming.model.Summary
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

class SummaryAdapter(
    private var summaryList: List<Summary>,
    private val mContext: Context
) : RecyclerView.Adapter<SummaryAdapter.MyViewHolder>() {

    class MyViewHolder(val binding: ListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val name: TextView get() = binding.taskNameSummary
        val timeAmount: TextView get() = binding.taskTimeSummary
        val viewForeground: View get() = binding.viewForeground
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = ListItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val taskSummary = summaryList[position]
        holder.name.text = taskSummary.taskName

        val actualTime = convertTimeAmountToString(taskSummary.actualTimeAmount)
        val desiredTime = if (taskSummary.desiredTimeAmount == 0L) {
            "00:00:00"
        } else {
            convertTimeAmountToStringWithoutUTF(taskSummary.desiredTimeAmount)
        }
        holder.timeAmount.text = "$actualTime\\$desiredTime"
        holder.viewForeground.setBackgroundResource(R.color.blue)

        val lp = holder.viewForeground.layoutParams as ViewGroup.MarginLayoutParams
        val ratio = calculateRatio(actualTime, desiredTime)
        holder.viewForeground.layoutParams.width =
            (getScreenWidth(mContext, lp.rightMargin) * ratio).toInt()
    }

    fun calculateRatio(actualTime: String, desiredTime: String): Double {
        val actual = toSeconds(actualTime)
        val desired = toSeconds(desiredTime)
        if (desired <= 0) return 0.01
        return if (actual > desired) 1.0 else {
            (actual.toDouble() / desired.toDouble()).coerceAtLeast(0.01)
        }
    }

    private fun toSeconds(time: String): Int {
        if (time.length < 8) return 0
        val hours = time.substring(0, 2).toIntOrNull() ?: 0
        val min = time.substring(3, 5).toIntOrNull() ?: 0
        val sec = time.substring(6, 8).toIntOrNull() ?: 0
        return hours * 3600 + min * 60 + sec
    }

    fun convertTimeAmountToString(timeAmount: Long): String {
        val date = Date(timeAmount)
        val formatter = SimpleDateFormat("HH:mm:ss")
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(date)
    }

    fun convertTimeAmountToStringWithoutUTF(timeAmount: Long): String {
        val date = Date(timeAmount)
        val formatter: DateFormat = SimpleDateFormat("HH:mm:ss")
        return formatter.format(date)
    }

    private fun getScreenWidth(context: Context, margin: Int): Int {
        val displayMetrics = DisplayMetrics()
        (context as Activity).windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.widthPixels - margin * 2
    }

    override fun getItemCount(): Int = summaryList.size
}

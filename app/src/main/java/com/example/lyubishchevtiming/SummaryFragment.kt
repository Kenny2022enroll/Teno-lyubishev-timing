package com.example.lyubishchevtiming

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lyubishchevtiming.database.AppDatabase
import com.example.lyubishchevtiming.databinding.FragmentSummaryBinding
import com.example.lyubishchevtiming.model.Log
import com.example.lyubishchevtiming.model.Summary
import com.example.lyubishchevtiming.viewmodel.SummaryViewModel
import com.example.lyubishchevtiming.viewmodel.SummaryViewModelFactory
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.TimeZone

class SummaryFragment : Fragment() {

    private var _binding: FragmentSummaryBinding? = null
    private val binding get() = _binding!!

    private var summaries: List<Summary> = emptyList()
    private var adapter: SummaryAdapter? = null
    private lateinit var db: AppDatabase

    private val timePeriods = arrayOf("today", "last 7 days", "last 30 days", "last 365 days")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSummaryBinding.inflate(inflater, container, false)
        db = AppDatabase.getInstance(requireActivity())

        loadSummaryData(binding.timePeriod.text.toString())

        binding.tasksListRecyclerView.setHasFixedSize(true)
        binding.tasksListRecyclerView.isNestedScrollingEnabled = false
        binding.tasksListRecyclerView.layoutManager = LinearLayoutManager(requireActivity())
        if (summaries.isNotEmpty()) adapter = SummaryAdapter(summaries, requireActivity())
        binding.tasksListRecyclerView.adapter = adapter

        binding.timePeriod.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.select_period)
                .setSingleChoiceItems(timePeriods, -1) { dialog: DialogInterface, item: Int ->
                    binding.timePeriod.text = timePeriods[item]
                    loadSummaryData(timePeriods[item])
                    dialog.dismiss()
                }
                .create()
                .show()
        }

        binding.fabDownloadCsv.setOnClickListener { exportAllLogsToCsv() }
        return binding.root
    }

    private fun loadSummaryData(timePeriod: String) {
        val calendar = GregorianCalendar().apply {
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = calendar.time
        when (timePeriod) {
            "today" -> calendar.add(Calendar.DAY_OF_MONTH, -1)
            "last 7 days" -> calendar.add(Calendar.DAY_OF_MONTH, -7)
            "last 30 days" -> calendar.add(Calendar.DAY_OF_MONTH, -30)
            "last 365 days" -> calendar.add(Calendar.DAY_OF_MONTH, -365)
        }
        val start = calendar.time

        val fmt = SimpleDateFormat("EEE, MMM dd")
        val startString = fmt.format(start)
        val endString = fmt.format(end)

        val factory = SummaryViewModelFactory(db, start, end)
        val viewModel = ViewModelProvider(this, factory)[SummaryViewModel::class.java]
        viewModel.summary.observe(requireActivity(), Observer { result ->
            if (result != null) {
                summaries = result
                adapter = SummaryAdapter(summaries, requireActivity())
                binding.tasksListRecyclerView.adapter = adapter
                binding.tasksListRecyclerView.isNestedScrollingEnabled = false
                adjustPieChart()
                addData()
                binding.period.text = "$startString - $endString"
            }
        })
    }

    private fun exportAllLogsToCsv() {
        lifecycleScope.launch {
            val logs: List<Log> = withContext(Dispatchers.IO) { db.logDao().loadAllLogsSync() }
            if (logs.isEmpty()) {
                Toast.makeText(requireContext(), R.string.no_logs_to_export, Toast.LENGTH_SHORT).show()
                return@launch
            }
            val sb = StringBuilder()
            sb.append("Id,Date,Actual Time Amount,Desired Time Amount,Task Id")
            logs.forEach { log ->
                val actual = formatTimeString(convertTimeAmountToString(log.todayTimeAmount))
                val desired = if (log.desiredTimeAmount != 0L)
                    formatTimeString(convertTimeAmountToStringWithoutUTF(log.desiredTimeAmount))
                else "day off"
                sb.append("\n${log.id},${convertDateToString(log.todayDate)},$actual,$desired,${log.taskId}")
            }
            try {
                val out = requireContext().openFileOutput("data.csv", Context.MODE_PRIVATE)
                out.write(sb.toString().toByteArray())
                out.close()
                val fileLocation = File(requireContext().filesDir, "data.csv")
                val path = FileProvider.getUriForFile(
                    requireContext(), "${requireContext().packageName}.fileprovider", fileLocation
                )
                val fileIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_SUBJECT, "Logs")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    putExtra(Intent.EXTRA_STREAM, path)
                }
                startActivity(Intent.createChooser(fileIntent, getString(R.string.send_logs)))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun formatTimeString(time: String): String {
        if (time.length < 8) return time
        val hours = time.substring(0, 2).toInt()
        val min = time.substring(3, 5).toInt()
        val sec = time.substring(6, 8).toInt()
        return "$hours h $min min $sec sec"
    }

    private fun convertDateToString(date: Date): String {
        val df: DateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
        return df.format(date)
    }

    private fun adjustPieChart() {
        binding.pieChart.isRotationEnabled = true
        binding.pieChart.holeRadius = 60f
        binding.pieChart.setCenterTextSize(20f)
        binding.pieChart.setUsePercentValues(true)
    }

    /**
     * 绘制饼图。
     *
     * 科学重解读：原版把"计划-实际"的差额一律标为"Ineffective time"（无效时间），
     * 这与恢复性研究相悖——休息与恢复是维持持续性注意的必要条件
     *（Attention Restoration Theory, Kaplan & Kaplan, 1995；以及超日节律 / BRAC）。
     * 因此本版将该切片重命名为"Untracked time"（未追踪时间），
     * 并在中心文字旁加注"休息是必要的"，避免把必要的恢复污名化为"无效"。
     */
    private fun addData() {
        val yEntries = ArrayList<PieEntry>()

        var actualTotal = 0L
        var totalDesiredHours = 0
        var totalDesiredMin = 0

        summaries.forEach { s ->
            val timeStr = convertTimeAmountToString(s.actualTimeAmount)
            val seconds = toSeconds(timeStr)
            yEntries.add(PieEntry(seconds.toFloat(), s.taskName))
            actualTotal += s.actualTimeAmount
            val desiredStr = if (s.desiredTimeAmount != 0L)
                convertTimeAmountToStringWithoutUTF(s.desiredTimeAmount) else "00:00:00"
            totalDesiredHours += desiredStr.substring(0, 2).toInt()
            totalDesiredMin += desiredStr.substring(3, 5).toInt()
        }

        binding.pieChart.setEntryLabelColor(R.color.colorIcons)

        val actualStr = if (actualTotal != 0L) convertTimeAmountToString(actualTotal) else "00:00:00"
        val actualHours = actualStr.substring(0, 2).toInt()
        val actualMin = actualStr.substring(3, 5).toInt()

        val actualSec = toSeconds(actualStr)
        val desiredSec = totalDesiredHours * 3600 + totalDesiredMin * 60
        val difference = (desiredSec - actualSec).coerceAtLeast(0)
        // 重新解读：未追踪时间（含必要的恢复性休息），而非"无效"
        yEntries.add(PieEntry(difference.toFloat(), getString(R.string.untracked_time)))

        val pieDataSet = PieDataSet(yEntries, getString(R.string.productivity)).apply {
            sliceSpace = 2f
            valueTextSize = 14f
        }
        binding.pieChart.data = PieData(pieDataSet)

        if (summaries.isNotEmpty()) {
            binding.pieChart.centerText = getString(
                R.string.chart_name, actualHours, actualMin, totalDesiredHours, totalDesiredMin
            ) + "\n" + getString(R.string.chart_recovery_note)
        } else {
            binding.pieChart.centerText = getString(R.string.nothing_done_today)
        }

        binding.pieChart.setDrawSliceText(false)
        binding.pieChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry, h: Highlight) {
                val matched = yEntries.firstOrNull { it.y == e.y }
                Toast.makeText(requireContext(), matched?.label.orEmpty(), Toast.LENGTH_LONG).show()
            }
            override fun onNothingSelected() {}
        })
        binding.pieChart.invalidate()
    }

    private fun toSeconds(time: String): Int {
        if (time.length < 8) return 0
        return time.substring(0, 2).toInt() * 3600 +
               time.substring(3, 5).toInt() * 60 +
               time.substring(6, 8).toInt()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.example.lyubishchevtiming.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.lyubishchevtiming.database.AppDatabase
import java.util.Date

class LogByDateForTaskViewModel(
    database: AppDatabase,
    id: Int,
    start: Date,
    end: Date
) : ViewModel() {

    val logsForTask: LiveData<Long> = database.logDao().loadLogsForTodayTask(id, start, end)
}

package com.example.lyubishchevtiming.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.lyubishchevtiming.database.AppDatabase
import com.example.lyubishchevtiming.model.Summary
import java.util.Date

class SummaryViewModel(
    database: AppDatabase,
    startDate: Date,
    endDate: Date
) : ViewModel() {

    val summary: LiveData<List<Summary>> =
        database.logDao().getLogsAndTaskInfoForSpecificDate(startDate, endDate)
}

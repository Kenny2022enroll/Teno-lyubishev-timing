package com.example.lyubishchevtiming.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.lyubishchevtiming.database.AppDatabase
import com.example.lyubishchevtiming.model.Week

class TaskWeekViewModel(
    database: AppDatabase,
    weekId: String
) : ViewModel() {

    val week: LiveData<Week> = database.weekDao().loadWeekById(weekId)
}

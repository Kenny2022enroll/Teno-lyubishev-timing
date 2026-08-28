package com.example.lyubishchevtiming.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.lyubishchevtiming.database.AppDatabase
import com.example.lyubishchevtiming.model.Week

class TaskWeekByNameViewModel(
    database: AppDatabase,
    taskName: String
) : ViewModel() {

    val weekByTaskName: LiveData<Week> = database.weekDao().loadWeekByTaskName(taskName)
}

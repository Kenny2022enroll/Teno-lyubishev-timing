package com.example.lyubishchevtiming.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.lyubishchevtiming.database.AppDatabase
import java.util.Date

class LogByDateForTaskViewModelFactory(
    private val database: AppDatabase,
    private val taskId: Int,
    private val start: Date,
    private val end: Date
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return LogByDateForTaskViewModel(database, taskId, start, end) as T
    }
}

package com.example.lyubishchevtiming.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.lyubishchevtiming.database.AppDatabase

class TaskWeekViewModelFactory(
    private val database: AppDatabase,
    private val weekId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TaskWeekViewModel(database, weekId) as T
    }
}

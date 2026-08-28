package com.example.lyubishchevtiming.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.lyubishchevtiming.database.AppDatabase

class TaskWeekByNameViewModelFactory(
    private val database: AppDatabase,
    private val taskName: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TaskWeekByNameViewModel(database, taskName) as T
    }
}

package com.example.lyubishchevtiming.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.lyubishchevtiming.database.AppDatabase

class TaskViewModelFactory(
    private val database: AppDatabase,
    private val taskId: Int
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TaskViewModel(database, taskId) as T
    }
}

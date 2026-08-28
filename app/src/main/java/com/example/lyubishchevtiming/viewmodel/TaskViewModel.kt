package com.example.lyubishchevtiming.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.lyubishchevtiming.database.AppDatabase
import com.example.lyubishchevtiming.model.Task

class TaskViewModel(
    database: AppDatabase,
    taskId: Int
) : ViewModel() {

    val task: LiveData<Task> = database.taskDao().loadTaskById(taskId)
}

package com.example.lyubishchevtiming.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.lyubishchevtiming.database.AppDatabase
import com.example.lyubishchevtiming.model.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val tasks: LiveData<List<Task>> = database.taskDao().loadAllTasks()

    /** 删除所有任务（与周记录级联）。在 IO 调度器上执行。 */
    suspend fun deleteAllTasks() = withContext(Dispatchers.IO) {
        database.taskDao().deleteTasks()
    }

    /**
     * “删除”单个任务：实际为软删除（归档）。任务从活动列表移除，
     * 但其历史 Log 不被删除，统计中仍保留对应时长。
     */
    suspend fun archiveTask(taskId: Int) = withContext(Dispatchers.IO) {
        database.taskDao().archiveTask(taskId)
    }
}

package com.example.lyubishchevtiming.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.lyubishchevtiming.database.AppDatabase
import com.example.lyubishchevtiming.model.Log

class AllLogsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val logs: LiveData<List<Log>> = database.logDao().loadAllLogs()
}

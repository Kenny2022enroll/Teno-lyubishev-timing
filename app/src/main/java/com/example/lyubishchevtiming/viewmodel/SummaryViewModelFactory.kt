package com.example.lyubishchevtiming.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.lyubishchevtiming.database.AppDatabase
import java.util.Date

class SummaryViewModelFactory(
    private val database: AppDatabase,
    private val startDate: Date,
    private val endDate: Date
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SummaryViewModel(database, startDate, endDate) as T
    }
}

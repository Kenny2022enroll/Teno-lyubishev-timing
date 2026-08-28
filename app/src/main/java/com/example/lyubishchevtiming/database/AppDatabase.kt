package com.example.lyubishchevtiming.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.lyubishchevtiming.dao.LogDao
import com.example.lyubishchevtiming.dao.TaskDao
import com.example.lyubishchevtiming.dao.WeekDao
import com.example.lyubishchevtiming.model.Log
import com.example.lyubishchevtiming.model.Task
import com.example.lyubishchevtiming.model.Week

@Database(entities = [Log::class, Task::class, Week::class], version = 1, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun logDao(): LogDao
    abstract fun weekDao(): WeekDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lyubishchevTiming"
                ).build().also { instance = it }
            }
        }
    }
}

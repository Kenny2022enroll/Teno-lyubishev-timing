package com.example.lyubishchevtiming.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.lyubishchevtiming.dao.LogDao
import com.example.lyubishchevtiming.dao.TaskDao
import com.example.lyubishchevtiming.dao.WeekDao
import com.example.lyubishchevtiming.model.Log
import com.example.lyubishchevtiming.model.Task
import com.example.lyubishchevtiming.model.Week

@Database(entities = [Log::class, Task::class, Week::class], version = 2, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun logDao(): LogDao
    abstract fun weekDao(): WeekDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /**
         * v1 → v2：为 task 表增加 is_archived 列，支持“软删除”任务而保留历史统计。
         * 默认 0（未归档）。使用 ALTER TABLE 保留已有数据。
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE task ADD COLUMN is_archived INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lyubishchevTiming"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build().also { instance = it }
            }
        }
    }
}

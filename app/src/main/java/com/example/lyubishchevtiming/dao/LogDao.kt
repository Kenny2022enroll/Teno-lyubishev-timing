package com.example.lyubishchevtiming.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.lyubishchevtiming.model.Log
import com.example.lyubishchevtiming.model.Summary
import java.util.Date

@Dao
interface LogDao {

    @Query("SELECT * FROM log WHERE id = :id")
    fun loadLogById(id: Int): LiveData<List<Log>>

    @Query("SELECT * FROM log")
    fun loadAllLogs(): LiveData<List<Log>>

    @Query("SELECT * FROM log")
    suspend fun loadAllLogsSync(): List<Log>

    @Insert
    suspend fun insertLog(log: Log)

    @Query("SELECT sum(today_time_amount) FROM log WHERE today_date BETWEEN :start AND :end AND task_id = :id group by task_id")
    fun loadLogsForTodayTask(id: Int, start: Date, end: Date): LiveData<Long>

    @Query(
        """SELECT task.id, task.name, sum(log.today_time_amount) as today_time_amount,
                  (log.desired_time_amount) as desired_time_amount, task.color
           FROM log LEFT JOIN task ON log.task_id = task.id
           GROUP BY task.id
           HAVING (log.today_date BETWEEN :start_date AND :end_date)"""
    )
    fun getLogsAndTaskInfoForSpecificDate(start_date: Date, end_date: Date): LiveData<List<Summary>>

    @Query(
        """SELECT task.id, task.name, sum(log.today_time_amount) as today_time_amount,
                  min(log.desired_time_amount) as desired_time_amount, task.color
           FROM log LEFT JOIN task ON log.task_id = task.id
           GROUP BY task.id
           HAVING (log.today_date BETWEEN :start_date AND :end_date)"""
    )
    suspend fun getLogsForService(start_date: Date, end_date: Date): List<Summary>
}

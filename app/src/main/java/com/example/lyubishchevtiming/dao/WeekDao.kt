package com.example.lyubishchevtiming.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.lyubishchevtiming.model.Week

@Dao
interface WeekDao {

    @Query("SELECT * FROM week")
    fun loadAllWeekDaysCombinations(): LiveData<List<Week>>

    @Query("SELECT * FROM week WHERE id = :id")
    fun loadWeekById(id: String): LiveData<Week>

    @Query("SELECT * FROM week WHERE task_name = :taskName")
    fun loadWeekByTaskName(taskName: String): LiveData<Week>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeekDayCombination(week: Week)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateWeek(week: Week)

    @Query("DELETE FROM week")
    suspend fun deleteWeeks()
}

package com.example.lyubishchevtiming.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.lyubishchevtiming.model.Task

@Dao
interface TaskDao {

    @Query("SELECT * FROM task t WHERE t.id = :id")
    fun loadTaskById(id: Int): LiveData<Task>

    @Query("SELECT * FROM task")
    fun loadAllTasks(): LiveData<List<Task>>

    @Insert
    suspend fun insertTask(task: Task)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateTask(task: Task)

    @Query("DELETE FROM task")
    suspend fun deleteTasks()
}

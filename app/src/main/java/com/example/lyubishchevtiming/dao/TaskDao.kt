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

    // 仅列出未归档的任务；已“删除”（软删除）的任务不显示在活动列表中，
    // 但其历史 Log 仍参与统计（见 LogDao 的汇总查询）。
    @Query("SELECT * FROM task WHERE is_archived = 0 ORDER BY id DESC")
    fun loadAllTasks(): LiveData<List<Task>>

    @Insert
    suspend fun insertTask(task: Task)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateTask(task: Task)

    /** 软删除：标记任务为已归档，保留其历史 Log 与统计贡献。 */
    @Query("UPDATE task SET is_archived = 1 WHERE id = :id")
    suspend fun archiveTask(id: Int)

    @Query("DELETE FROM task")
    suspend fun deleteTasks()
}

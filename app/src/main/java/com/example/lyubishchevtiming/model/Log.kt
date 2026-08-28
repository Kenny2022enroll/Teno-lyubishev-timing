package com.example.lyubishchevtiming.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 单次时间追踪记录。一次"开始-停止"产生一条 [Log]。
 * 与 [Task] 为多对一关系，外键级联删除。
 */
@Entity(
    tableName = "log",
    indices = [Index("task_id")],
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Log(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    @ColumnInfo(name = "today_date") var todayDate: Date = Date(),
    @ColumnInfo(name = "today_time_amount") var todayTimeAmount: Long = 0L,
    @ColumnInfo(name = "desired_time_amount") var desiredTimeAmount: Long = 0L,
    @ColumnInfo(name = "task_id") var taskId: Int = 0
)

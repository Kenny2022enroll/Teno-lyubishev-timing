package com.example.lyubishchevtiming.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * 用户想要追踪的活动（任务）。
 * 隶属于某个 [Week] 计划，外键级联删除。
 *
 * “删除”采用软删除（[isArchived] = true）：任务从活动列表中隐藏，
 * 但其历史 [Log] 记录与对统计的贡献保持不变——不删除已统计的数据。
 */
@Entity(
    tableName = "task",
    indices = [Index("week_id")],
    foreignKeys = [
        ForeignKey(
            entity = Week::class,
            parentColumns = ["id"],
            childColumns = ["week_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@Parcelize
data class Task(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    var name: String = "",
    var color: String = "",
    var duration: Long = 0L,
    @ColumnInfo(name = "week_id") var weekId: String = "",
    @ColumnInfo(name = "is_archived") var isArchived: Boolean = false
) : Parcelable

package com.example.lyubishchevtiming.model

import androidx.room.ColumnInfo

/**
 * 聚合查询的结果 POJO（不是独立实体）。
 * 用于按时间段汇总每个任务的实际/计划时长。
 */
data class Summary(
    @ColumnInfo(name = "id") val taskId: Int = 0,
    @ColumnInfo(name = "name") val taskName: String = "",
    @ColumnInfo(name = "desired_time_amount") val desiredTimeAmount: Long = 0L,
    @ColumnInfo(name = "today_time_amount") val actualTimeAmount: Long = 0L,
    @ColumnInfo(name = "color") val taskColor: String = ""
)

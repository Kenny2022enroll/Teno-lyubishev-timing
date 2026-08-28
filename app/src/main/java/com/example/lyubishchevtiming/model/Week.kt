package com.example.lyubishchevtiming.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * 某任务在一周内每天的计划时长（基于 Lyubishchev 的每日"应当"时间）。
 */
@Entity(tableName = "week")
@Parcelize
data class Week(
    @PrimaryKey(autoGenerate = false) var id: String = "",
    @ColumnInfo(name = "task_name") var taskName: String = "",
    var mon: Long = 0L,
    var tue: Long = 0L,
    var wed: Long = 0L,
    var thu: Long = 0L,
    var fri: Long = 0L,
    var sat: Long = 0L,
    var sun: Long = 0L
) : Parcelable

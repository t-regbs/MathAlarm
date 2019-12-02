package com.android.example.mathalarm.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "alarms")
data class Alarm (
    @PrimaryKey(autoGenerate = true)
    var alarmId: Long = 0L,

    @ColumnInfo(name = "hour")
    var hour: Int = -1,

    @ColumnInfo(name = "minute")
    var minute: Int = -1,

    @ColumnInfo(name = "repeat")
    var repeat: Boolean = false,

    @ColumnInfo(name = "daysoftheweek")
    var repeatDays: String = "FFFFFFF",

    @ColumnInfo(name = "ison")
    var isOn: Boolean = false,

    @ColumnInfo(name = "difficulty")
    var difficulty: Int = 0,

    @ColumnInfo(name = "tone")
    var alarmTone: String = "",

    @ColumnInfo(name = "vibrate")
    var vibrate: Boolean = false,

    @ColumnInfo(name = "snooze")
    var snooze: Int = 5

)
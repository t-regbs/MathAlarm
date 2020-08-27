package com.android.example.mathalarm.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "alarms")
data class Alarm (
    @PrimaryKey(autoGenerate = true)
    var alarmId: Long = 0L,

    @Ignore
    val newCal: Calendar = Calendar.getInstance(),

    @Ignore
    val newHour: Int = newCal[Calendar.HOUR_OF_DAY],

    @Ignore
    val newMinute: Int = newCal[Calendar.MINUTE],

    @ColumnInfo(name = "hour")
    var hour: Int = newHour,

    @ColumnInfo(name = "minute")
    var minute: Int = newMinute,

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
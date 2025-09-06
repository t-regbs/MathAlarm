package com.timilehinaregbesola.mathalarm.framework.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val alarmId: Long,

    @ColumnInfo(name = "hour")
    val hour: Int,

    @ColumnInfo(name = "minute")
    val minute: Int,

    @ColumnInfo(name = "repeat")
    val repeat: Boolean,

    @ColumnInfo(name = "daysoftheweek")
    val repeatDays: String,

    @ColumnInfo(name = "ison")
    val isOn: Boolean,

    @ColumnInfo(name = "difficulty")
    val difficulty: Int,

    @ColumnInfo(name = "tone")
    val alarmTone: String,

    @ColumnInfo(name = "vibrate")
    val vibrate: Boolean,

    @ColumnInfo(name = "snooze")
    val snooze: Int,

    @ColumnInfo(name = "title", defaultValue = "")
    val title: String,

    @ColumnInfo(name = "isSaved")
    val isSaved: Boolean,
) : Parcelable

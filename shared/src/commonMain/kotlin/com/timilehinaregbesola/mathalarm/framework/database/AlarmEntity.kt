package com.timilehinaregbesola.mathalarm.framework.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
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
    @ColumnInfo(defaultValue = "1")
    val questionCount: Int = 1,
    @ColumnInfo(defaultValue = "'+−×÷'")
    val challengeOperations: String = "+−×÷",
    @ColumnInfo(defaultValue = "0")
    val additionRange: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val factorRange: Int = 0,
    @ColumnInfo(defaultValue = "''")
    val difficultyMix: String = "",
    @ColumnInfo(name = "tone")
    val alarmTone: String,
    @ColumnInfo(name = "vibrate")
    val vibrate: Boolean,
    @ColumnInfo(name = "snooze")
    val snooze: Int,
    @ColumnInfo(defaultValue = "0")
    val maxSnoozes: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val snoozeRequiresQuestion: Boolean = false, // Retired flag retained for database compatibility.
    @ColumnInfo(defaultValue = "0")
    val snoozeCount: Int = 0,
    @ColumnInfo(name = "title", defaultValue = "")
    val title: String,
    @ColumnInfo(name = "isSaved")
    val isSaved: Boolean,
    @ColumnInfo(defaultValue = "''")
    val pendingTimes: String = "",
    @ColumnInfo(defaultValue = "0")
    val scheduleInitialized: Boolean = false,
    val snoozedUntil: Long? = null,
    val activeAt: Long? = null,
    val skippedDate: String? = null,
    val scheduleError: String? = null,
    val scheduleTimeZone: String? = null,
)

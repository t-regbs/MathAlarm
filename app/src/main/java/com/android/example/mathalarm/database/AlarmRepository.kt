package com.android.example.mathalarm.database

class AlarmRepository(private val alarmDao: AlarmDao) {

    suspend fun add(alarm: Alarm): Long{
        return alarmDao.addAlarm(alarm)
    }

    suspend fun delete(alarm: Alarm) {
        alarmDao.deleteAlarm(alarm)
    }

    suspend fun update(alarm: Alarm){
        alarmDao.updateAlarm(alarm)
    }


    suspend fun clear() {
        alarmDao.clear()
    }
}
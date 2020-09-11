package com.timilehinaregbesola.mathalarm.database

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

    suspend fun getAlarms(): List<Alarm> {
        return alarmDao.getAlarms()
    }

    suspend fun getLatestAlarmFromDatabase(): Alarm? {
        return alarmDao.getLastAlarm()
    }

    suspend fun findAlarm(id: Long): Alarm = alarmDao.getAlarm(id)

    suspend fun clear() {
        alarmDao.clear()
    }
}
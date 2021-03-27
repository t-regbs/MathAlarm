package com.timilehinaregbesola.mathalarm.framework

import com.timilehinaregbesola.mathalarm.data.AlarmDataSource
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.database.AlarmDao
import com.timilehinaregbesola.mathalarm.framework.database.AlarmMapper

class RoomAlarmDataSource(
    private val alarmDao: AlarmDao,
    private val mapper: AlarmMapper
) : AlarmDataSource {

    override suspend fun addAlarm(alarm: Alarm): Long {
        return alarmDao.addAlarm(mapper.mapFromDomainModel(alarm))
    }

    override suspend fun deleteAlarm(alarm: Alarm) =
        alarmDao.deleteAlarm(mapper.mapFromDomainModel(alarm))

    override suspend fun updateAlarm(alarm: Alarm) =
        alarmDao.updateAlarm(mapper.mapFromDomainModel(alarm))

    override suspend fun getAlarms(): List<Alarm> = mapper.toDomainList(alarmDao.getAlarms())

    override suspend fun getLatestAlarmFromDatabase(): Alarm? = alarmDao.getLastAlarm()?.let {
        mapper.mapToDomainModel(it)
    }

    override suspend fun findAlarm(id: Long): Alarm = mapper.mapToDomainModel(alarmDao.getAlarm(id))

    override suspend fun clear() = alarmDao.clear()
}

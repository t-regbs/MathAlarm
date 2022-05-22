package com.timilehinaregbesola.mathalarm.framework

import com.timilehinaregbesola.mathalarm.data.datasource.AlarmDataSource
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.database.AlarmDao
import com.timilehinaregbesola.mathalarm.framework.database.AlarmMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomAlarmDataSource(
    private val alarmDao: AlarmDao,
    private val mapper: AlarmMapper
) : AlarmDataSource {

    override suspend fun addAlarm(alarm: Alarm): Long {
        return alarmDao.addAlarm(mapper.mapFromDomainModel(alarm))
    }

    override suspend fun deleteAlarm(alarm: Alarm) =
        alarmDao.deleteAlarm(mapper.mapFromDomainModel(alarm))

    override suspend fun deleteAlarmFromId(id: Long) {
        alarmDao.deleteAlarmWithId(id)
    }

    override suspend fun updateAlarm(alarm: Alarm) =
        alarmDao.updateAlarm(mapper.mapFromDomainModel(alarm))

    override fun getAlarms(): Flow<List<Alarm>> = alarmDao.getAlarms().map { entityList ->
        mapper.toDomainList(entityList)
    }

    override fun getSavedAlarms(): Flow<List<Alarm>> = alarmDao.getSavedAlarms().map { entityList ->
        mapper.toDomainList(entityList)
    }

//    override suspend fun getLatestAlarmFromDatabase(): Alarm? = alarmDao.getLastAlarm()?.let {
//        mapper.mapToDomainModel(it)
//    }

    override suspend fun findAlarm(id: Long): Alarm? = alarmDao.getAlarm(id)?.let { mapper.mapToDomainModel(it) }

    override suspend fun clear() = alarmDao.clear()
}

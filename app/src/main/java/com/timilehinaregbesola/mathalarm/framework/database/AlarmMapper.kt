package com.timilehinaregbesola.mathalarm.framework.database

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.domain.util.DomainMapper

class AlarmMapper : DomainMapper<AlarmEntity, Alarm> {
    override fun mapToDomainModel(model: AlarmEntity): Alarm {
        return Alarm(
            alarmId = model.alarmId,
            hour = model.hour,
            minute = model.minute,
            repeat = model.repeat,
            repeatDays = model.repeatDays,
            isOn = model.isOn,
            difficulty = model.difficulty,
            alarmTone = model.alarmTone,
            vibrate = model.vibrate,
            snooze = model.snooze,
            title = model.title,
            isSaved = model.isSaved
        )
    }

    override fun mapFromDomainModel(domainModel: Alarm): AlarmEntity {
        return AlarmEntity(
            alarmId = domainModel.alarmId,
            hour = domainModel.hour,
            minute = domainModel.minute,
            repeat = domainModel.repeat,
            repeatDays = domainModel.repeatDays,
            isOn = domainModel.isOn,
            difficulty = domainModel.difficulty,
            alarmTone = domainModel.alarmTone,
            vibrate = domainModel.vibrate,
            snooze = domainModel.snooze,
            title = domainModel.title,
            isSaved = domainModel.isSaved
        )
    }

    fun toDomainList(initial: List<AlarmEntity>): List<Alarm> {
        return initial.map { mapToDomainModel(it) }
    }

    fun fromDomainList(initial: List<Alarm>): List<AlarmEntity> {
        return initial.map { mapFromDomainModel(it) }
    }
}

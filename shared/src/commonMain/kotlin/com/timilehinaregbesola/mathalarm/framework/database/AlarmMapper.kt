package com.timilehinaregbesola.mathalarm.framework.database

import com.timilehinaregbesola.mathalarm.domain.model.MathChallenge
import com.timilehinaregbesola.mathalarm.domain.model.mathChallenge
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.domain.util.DomainMapper

class AlarmMapper : DomainMapper<AlarmEntity, Alarm> {
    override fun mapToDomainModel(model: AlarmEntity): Alarm {
        val challenge = MathChallenge(
            difficulty = model.difficulty,
            questionCount = model.questionCount,
            operations = model.challengeOperations,
            additionRange = model.additionRange,
            factorRange = model.factorRange,
            difficultyMix = model.difficultyMix,
        ).normalized()
        return Alarm(
            alarmId = model.alarmId,
            hour = model.hour,
            minute = model.minute,
            repeat = model.repeat,
            repeatDays = model.repeatDays,
            isOn = model.isOn,
            difficulty = challenge.difficulty,
            questionCount = challenge.questionCount,
            challengeOperations = challenge.operations,
            additionRange = challenge.additionRange,
            factorRange = challenge.factorRange,
            difficultyMix = challenge.difficultyMix,
            alarmTone = model.alarmTone,
            vibrate = model.vibrate,
            snooze = model.snooze,
            title = model.title,
            isSaved = model.isSaved,
            pendingTimes = model.pendingTimes.split(',').mapNotNull(String::toLongOrNull),
            scheduleInitialized = model.scheduleInitialized,
            snoozedUntil = model.snoozedUntil,
            activeAt = model.activeAt,
            scheduleError = model.scheduleError,
            scheduleTimeZone = model.scheduleTimeZone
        )
    }

    override fun mapFromDomainModel(domainModel: Alarm): AlarmEntity {
        val challenge = domainModel.mathChallenge
        return AlarmEntity(
            alarmId = domainModel.alarmId,
            hour = domainModel.hour,
            minute = domainModel.minute,
            repeat = domainModel.repeat,
            repeatDays = domainModel.repeatDays,
            isOn = domainModel.isOn,
            difficulty = challenge.difficulty,
            questionCount = challenge.questionCount,
            challengeOperations = challenge.operations,
            additionRange = challenge.additionRange,
            factorRange = challenge.factorRange,
            difficultyMix = challenge.difficultyMix,
            alarmTone = domainModel.alarmTone,
            vibrate = domainModel.vibrate,
            snooze = domainModel.snooze,
            title = domainModel.title,
            isSaved = domainModel.isSaved,
            pendingTimes = domainModel.pendingTimes.joinToString(","),
            scheduleInitialized = domainModel.scheduleInitialized,
            snoozedUntil = domainModel.snoozedUntil,
            activeAt = domainModel.activeAt,
            scheduleError = domainModel.scheduleError,
            scheduleTimeZone = domainModel.scheduleTimeZone
        )
    }

    fun toDomainList(initial: List<AlarmEntity>): List<Alarm> {
        return initial.map { mapToDomainModel(it) }
    }

    fun fromDomainList(initial: List<Alarm>): List<AlarmEntity> {
        return initial.map { mapFromDomainModel(it) }
    }
}

package com.timilehinaregbesola.mathalarm.presentation.alarmmath

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.domain.model.MathChallenge
import com.timilehinaregbesola.mathalarm.domain.model.mathChallenge
import com.timilehinaregbesola.mathalarm.framework.database.AlarmEntity
import com.timilehinaregbesola.mathalarm.framework.database.AlarmMapper
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class MathChallengePersistenceTest {
    @Test fun malformedSettingsAreNormalizedAtBothMappingBoundaries() {
        val mapper = AlarmMapper()
        val invalid = Alarm(difficulty = 9, questionCount = 99, challengeOperations = "?", additionRange = -1, factorRange = 9)
        val entity = mapper.mapFromDomainModel(invalid)
        assertEquals(3, entity.difficulty)
        assertEquals(10, entity.questionCount)
        assertEquals("+−×÷", entity.challengeOperations)
        val restored = mapper.mapToDomainModel(entity.copy(questionCount = 0, challengeOperations = "××?"))
        assertEquals(1, restored.questionCount)
        assertEquals("×", restored.challengeOperations)
    }

    @Test fun customSettingsSurviveNotificationSerializationAndMapping() {
        val mapper = AlarmMapper()
        val alarm = Alarm(alarmId = 42, difficulty = 3, questionCount = 10,
            challengeOperations = "−÷", additionRange = 1, factorRange = 2)
        val json = Json.encodeToString(mapper.mapFromDomainModel(alarm))
        val restored = mapper.mapToDomainModel(Json.decodeFromString<AlarmEntity>(json))
        assertEquals(alarm.mathChallenge, restored.mathChallenge)
    }

    @Test fun legacyNotificationPayloadDefaultsToOneQuestion() {
        val legacy = """{"alarmId":1,"hour":7,"minute":30,"repeat":false,"repeatDays":"FFFFFFF","isOn":true,"difficulty":2,"alarmTone":"tone","vibrate":false,"snooze":5,"title":"Wake up","isSaved":true}"""
        val alarm = AlarmMapper().mapToDomainModel(Json.decodeFromString<AlarmEntity>(legacy))
        assertEquals(MathChallenge(difficulty = 2), alarm.mathChallenge)
    }
}

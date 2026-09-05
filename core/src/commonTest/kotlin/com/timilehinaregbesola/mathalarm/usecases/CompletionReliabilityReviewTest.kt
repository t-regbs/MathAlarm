package com.timilehinaregbesola.mathalarm.review

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.*
import com.timilehinaregbesola.mathalarm.usecases.CompleteAlarm
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse

class CompletionReliabilityReviewTest {
    @Test fun finalMondayAfterSaturdayMustCompleteOneTimeCycle() = runTest {
        val repository = AlarmRepository(AlarmRepositoryFake())
        val interactor = AlarmInteractorFake()
        val clock = DateTimeProviderFake().apply { setFixedDateTime(2026, 9, 7, 7, 5) }
        // Created Friday for Saturday Sep 5 and Monday Sep 7. Both have now fired.
        val alarm = Alarm(alarmId = 44, hour = 7, minute = 0, repeat = false,
            repeatDays = "FTFFFFT", isOn = true, isSaved = true)
        repository.addAlarm(alarm)
        CompleteAlarm(repository, interactor, NotificationInteractorFake(), clock)(alarm)
        assertFalse(repository.findAlarm(44)!!.isOn,
            "No occurrences remain: last Monday completion must switch the one-time alarm off")
    }
}

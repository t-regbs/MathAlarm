package com.timilehinaregbesola.mathalarm

import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.alarm.AlarmSchedulerBridge
import com.timilehinaregbesola.mathalarm.alarm.NativeAlarmScheduler
import com.timilehinaregbesola.mathalarm.coroutines.AppCoroutineScope
import com.timilehinaregbesola.mathalarm.di.initKoin
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.framework.database.AlarmDatabase
import com.timilehinaregbesola.mathalarm.navigation.AlarmHandoff
import com.timilehinaregbesola.mathalarm.navigation.decodeAlarmHandoff
import com.timilehinaregbesola.mathalarm.navigation.encodeAlarmHandoff
import com.timilehinaregbesola.mathalarm.notification.NotificationDeeplinkHolder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent

/**
 * Native lifecycle facade. Startup, prewarming, reconciliation and handoff encoding
 * work without constructing a UIViewController. Call native entry points on Main.
 * The DI container and repositories stay private to Kotlin.
 */
object IosApplication {
    private val dependencies = object : KoinComponent {}
    private var initialized = false
    private val applicationScope: CoroutineScope
        get() = dependencies.getKoin().get<AppCoroutineScope>()

    /** Register delivery services before any persisted schedule is read/reconciled. */
    fun initialize(scheduler: NativeAlarmScheduler) {
        AlarmSchedulerBridge.registerScheduler(scheduler)
        if (initialized) return
        initKoin()
        com.timilehinaregbesola.mathalarm.application.AlarmApplicationStatus.restore()
        initialized = true
        applicationScope.launch(Dispatchers.IO) {
            try {
                dependencies.getKoin().get<AlarmDatabase>().alarmDatabaseDao
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(e) { "Database prewarm failed" }
            }
        }
    }

    /** Acknowledged deliveries still exist as durable active occurrences after process death. */
    fun restoreUnresolvedHandoffs(completion: (List<String>, Boolean) -> Unit) {
        check(initialized) { "Initialize IosApplication before restoration" }
        applicationScope.launch(Dispatchers.IO) {
            try {
                val payloads = dependencies.getKoin().get<Usecases>().command {
                    getSavedAlarms().first().filter { it.isOn && it.activeAt != null }
                        .sortedWith(compareBy({ it.activeAt }, { it.alarmId }))
                        .map { encodeAlarmHandoff(AlarmHandoff(it.alarmId, activeAt = it.activeAt)) }
                }
                withContext(Dispatchers.Main) { completion(payloads, true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(e) { "Unresolved alarm restoration failed" }
                withContext(Dispatchers.Main) { completion(emptyList(), false) }
            }
        }
    }

    /** Native tokens identify deliveries; authoritative activeAt is loaded below presentation. */
    fun createAlarmHandoffJson(alarmId: Long, deliveryId: String, activeAt: Long?): String =
        encodeAlarmHandoff(AlarmHandoff(alarmId, activeAt = activeAt, version = 2, deliveryId = deliveryId))

    /** Pure DTO decoding; parsing does not accept, consume or acknowledge a delivery. */
    fun decodeAlarmHandoffJson(payload: String): AlarmHandoff? = decodeAlarmHandoff(payload)

    fun reportRecoveryFailure(alarmId: Long) =
        com.timilehinaregbesola.mathalarm.application.AlarmApplicationStatus.reportRecoveryFailure(alarmId)

    fun clearRecoveryFailure(alarmId: Long) =
        com.timilehinaregbesola.mathalarm.application.AlarmApplicationStatus.clearRecoveryFailure(alarmId)

    fun deliverPendingHandoff(payload: String) = NotificationDeeplinkHolder.setAlarmDeeplink(payload)

    fun acknowledgeHandoff(payload: String) {
        NotificationDeeplinkHolder.acknowledgeDeeplink(payload)
        resumeAlarmSchedules()
    }

    /** Reconciliation belongs to the application, not a view or its observers. */
    fun resumeAlarmSchedules() {
        check(initialized) { "Initialize IosApplication before reconciliation" }
        applicationScope.launch(Dispatchers.Main) {
            if (hasPendingDelivery()) return@launch
            try {
                dependencies.getKoin().get<Usecases>().command {
                    if (hasPendingDelivery()) return@command
                    rescheduleFutureAlarms.onAppResume(skipWhenNativeCurrent = true)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(e) { "Alarm reconciliation failed" }
            }
        }
    }

    private fun hasPendingDelivery(): Boolean =
        NotificationDeeplinkHolder.deeplinkInfo.value != null || AlarmSchedulerBridge.hasPendingHandoff()

}

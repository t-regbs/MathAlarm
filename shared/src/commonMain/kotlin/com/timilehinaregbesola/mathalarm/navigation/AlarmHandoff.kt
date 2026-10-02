package com.timilehinaregbesola.mathalarm.navigation

import com.timilehinaregbesola.mathalarm.framework.database.AlarmEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Native delivery identity is separate from authoritative persisted occurrence time.
 * Version 1 alarmId-only/activeAt payloads remain readable in the durable queue.
 */
@Serializable
data class AlarmHandoff(
    val alarmId: Long,
    val activeAt: Long? = null,
    val version: Int = 1,
    val deliveryId: String? = null,
)

private val handoffCodec = Json { ignoreUnknownKeys = true }

fun encodeAlarmHandoff(handoff: AlarmHandoff): String = Json.encodeToString(handoff)

/** Android notification entrypoints provide the full alarm snapshot. */
@OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
@kotlin.native.HiddenFromObjC
fun decodeAlarmHandoff(payload: String): AlarmHandoff? =
    runCatching { handoffCodec.decodeFromString<AlarmHandoff>(payload) }
        .getOrNull()?.takeIf { it.alarmId > 0 && it.version in 1..2 &&
            (it.version == 1 || !it.deliveryId.isNullOrBlank()) }

@OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
@kotlin.native.HiddenFromObjC
fun decodeAlarmSnapshot(payload: String): AlarmEntity? =
    runCatching { Json.decodeFromString<AlarmEntity>(payload) }
        .getOrNull()?.takeIf { it.alarmId > 0 }

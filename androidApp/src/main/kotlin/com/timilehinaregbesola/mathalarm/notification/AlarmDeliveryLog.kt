package com.timilehinaregbesola.mathalarm.notification

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Bounded, on-device evidence of the schedule -> delivery -> audio lifecycle. */
object AlarmDeliveryLog {
    @Synchronized
    fun record(context: Context, event: String, id: Long, triggerAt: Long? = null, detail: String? = null) {
        val prefs = context.getSharedPreferences("alarm_delivery_log", Context.MODE_PRIVATE)
        val previous = runCatching { JSONArray(prefs.getString("events", "[]")) }.getOrDefault(JSONArray())
        val events = JSONArray()
        for (i in maxOf(0, previous.length() - 99) until previous.length()) events.put(previous.get(i))
        events.put(JSONObject().put("at", System.currentTimeMillis()).put("event", event)
            .put("alarmId", id).put("triggerAt", triggerAt).put("detail", detail))
        prefs.edit().putString("events", events.toString()).commit()
    }
}

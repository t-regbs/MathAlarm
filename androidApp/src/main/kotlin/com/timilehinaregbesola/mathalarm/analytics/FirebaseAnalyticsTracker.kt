package com.timilehinaregbesola.mathalarm.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

class FirebaseAnalyticsTracker(private val firebase: FirebaseAnalytics) : AnalyticsTracker {
    override fun track(event: AnalyticsEvent) {
        val parameters = Bundle().apply {
            event.labels.forEach { (key, value) -> putString(key, value) }
            event.counts.forEach { (key, value) -> putLong(key, value) }
        }
        firebase.logEvent(event.name, parameters)
    }
}

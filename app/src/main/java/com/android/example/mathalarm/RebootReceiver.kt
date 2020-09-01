package com.android.example.mathalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RebootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val service = Intent(context, RebootService::class.java)
        service.putExtra("service_extra", "Reboot")
        context.startService(service)
    }
}

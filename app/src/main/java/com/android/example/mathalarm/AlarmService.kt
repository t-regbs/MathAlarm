package com.android.example.mathalarm

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import androidx.navigation.fragment.NavHostFragment.findNavController
import com.android.example.mathalarm.screens.alarmmath.AlarmMathFragment
import com.android.example.mathalarm.screens.alarmsettings.AlarmSettingsFragment
import com.android.example.mathalarm.screens.alarmsettings.AlarmSettingsFragmentDirections

class AlarmService: JobIntentService() {

    companion object{
        // Service unique ID
        val SERVICE_JOB_ID = 50

        fun enqueueWork(context: Context?, service: Intent?) {
            enqueueWork(context!!, AlarmService::class.java, SERVICE_JOB_ID, service!!)
        }
    }

    override fun onHandleWork(intent: Intent) {
        onHandleIntent(intent)
    }


    fun onHandleIntent(intent: Intent) {
//        val mathFragment = Intent(this, AlarmMathFragment::class.java)
//        mathFragment.putExtra(
//            ALARM_EXTRA,
//            intent.extras!![ALARM_EXTRA].toString()
//        )
//        mathFragment.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        findNavController(AlarmSettingsFragment()).navigate(
            AlarmSettingsFragmentDirections.actionAlarmSettingsFragmentToAlarmMathFragment(intent.extras!![ALARM_EXTRA].toString().toLong(), ""))
//        startActivity(mathFragment)
    }
}
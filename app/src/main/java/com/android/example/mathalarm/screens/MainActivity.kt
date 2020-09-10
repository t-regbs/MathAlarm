package com.android.example.mathalarm.screens

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.android.example.mathalarm.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            RestartServiceBroadcastReceiver.scheduleJob(applicationContext)
//        } else {
//            val bck = ProcessMainClass()
//            bck.launchService(applicationContext)
//        }
    }
}

package com.android.example.mathalarm.screens.alarmmath

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.android.example.mathalarm.R
import com.android.example.mathalarm.screens.SingleFragmentActivity

class AlarmMathActivity : SingleFragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_math)
    }

    override fun createFragment(): Fragment {
        return AlarmMathFragment.newInstance()
    }

    override fun onBackPressed() {
        //Do nothing
    }
}
package com.android.example.mathalarm.screens.alarmmath

import androidx.fragment.app.Fragment
import com.android.example.mathalarm.screens.SingleFragmentActivity

class AlarmMathActivity : SingleFragmentActivity() {
    override fun createFragment(): Fragment {
        return AlarmMathFragment.newInstance()
    }

    override fun onBackPressed() {
        //Do nothing
    }
}
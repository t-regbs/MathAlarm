package com.timilehinaregbesola.mathalarm.utils

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.timilehinaregbesola.mathalarm.R
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmFragmentDirections
import java.security.InvalidParameterException

enum class Screen { AlarmList, AlarmSettings, AlarmMath }

fun Fragment.navigate(to: Screen, from: Screen, alarmId: Long? = null, isAdd: Boolean?) {
    if (to == from) {
        throw InvalidParameterException("Can't navigate to $to")
    }
    when (to) {
        Screen.AlarmList -> {
            findNavController().navigate(R.id.alarmFragment)
        }
        Screen.AlarmSettings -> {
            findNavController().navigate(
                AlarmFragmentDirections
                    .actionAlarmFragmentToAlarmSettingsFragment(alarmId!!, isAdd!!)
            )
        }
        Screen.AlarmMath -> {
//            findNavController().navigate(R.id.sign_in_fragment)
        }
    }
}

fun Fragment.pop() {
    findNavController().popBackStack()
}

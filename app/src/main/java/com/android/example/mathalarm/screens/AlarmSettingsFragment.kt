package com.android.example.mathalarm.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.android.example.mathalarm.R
import com.android.example.mathalarm.databinding.FragmentAlarmSettingsBinding

class AlarmSettingsFragment: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val binding: FragmentAlarmSettingsBinding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_alarm_settings, container, false)
        return binding.root
    }

}